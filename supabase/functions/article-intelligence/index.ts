const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
};

type Action = "summary" | "deep_dive" | "learn" | "ask";

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  try {
    const authorization = req.headers.get("Authorization") ?? "";
    if (!authorization.startsWith("Bearer ")) return json({ error: "Sign in is required" }, 401);

    const supabaseUrl = requiredEnv("SUPABASE_URL");
    const anonKey = requiredEnv("SUPABASE_ANON_KEY");
    const serviceKey = requiredEnv("SUPABASE_SERVICE_ROLE_KEY");
    const openAiKey = requiredEnv("OPENAI_API_KEY");

    const userResponse = await fetch(`${supabaseUrl}/auth/v1/user`, {
      headers: { Authorization: authorization, apikey: anonKey },
    });
    if (!userResponse.ok) return json({ error: "Your session has expired. Please sign in again." }, 401);
    const user = await userResponse.json();

    const body = await req.json();
    const action = String(body.action ?? "") as Action;
    if (!["summary", "deep_dive", "learn", "ask"].includes(action)) return json({ error: "Unknown intelligence action" }, 400);
    const article = body.article ?? {};
    const profileResponse = await fetch(
      `${supabaseUrl}/rest/v1/profiles?select=subscription_tier,subscription_status,premium_until,monthly_ai_limit,monthly_ai_used,usage_reset_at,email&id=eq.${user.id}&limit=1`,
      { headers: serviceHeaders(serviceKey) },
    );
    const profiles = profileResponse.ok ? await profileResponse.json() : [];
    const profile = profiles[0] ?? {};
    const isAdmin = profile.subscription_tier === "admin" || String(profile.email ?? "").toLowerCase() === "anoop.git26@gmail.com";
    const premiumActive = profile.subscription_tier === "premium" && profile.subscription_status === "active" &&
      (!profile.premium_until || new Date(profile.premium_until) > new Date());
    if (!isAdmin && !premiumActive) return json({ error: "Premium membership is required for Kestrel AI features.", code: "PREMIUM_REQUIRED" }, 403);
    let usage = Number(profile.monthly_ai_used ?? 0);
    const limit = Number(profile.monthly_ai_limit ?? 0);
    if (profile.usage_reset_at && new Date(profile.usage_reset_at) <= new Date()) usage = 0;
    if (!isAdmin && limit > 0 && usage >= limit) {
      return json({ error: "Your monthly AI allowance has been used. It will reset next month.", code: "AI_LIMIT_REACHED" }, 429);
    }
    const title = clean(article.title, 600);
    const description = clean(article.description, 8000);
    const sourceUrl = clean(article.url, 1000);
    if (!title) return json({ error: "Article title is required" }, 400);
    if (!sourceUrl) return json({ error: "The full article URL is unavailable" }, 400);
    const question = clean(body.question, 1000);
    if (action === "ask" && !question) return json({ error: "Enter a question first" }, 400);

    const articleId = clean(article.id, 300) || title;
    const publisherUrl = clean(article.sourceUrl, 1000);
    const fullArticle = await fetchReadableArticle(sourceUrl, title, publisherUrl);
    if (fullArticle.length < 500) {
      const fallback = await generateWebResearchFallback(openAiKey, action, title, description, sourceUrl, article.source, article.publishedAt, question);
      if (!fallback) {
        return json({
          error: "The publisher did not provide enough readable article text, and web research did not find enough verified reporting yet. Open the full story online and try again later.",
          code: "FULL_ARTICLE_UNAVAILABLE",
        }, 422);
      }
      return json({ content: fallback, cached: false, coverage: "web_research" });
    }
    const contentVersion = (await sha256(fullArticle)).slice(0, 16);
    const cacheKey = await sha256(`${articleId}|full-v1|${contentVersion}|${action}|${action === "ask" ? user.id + "|" + question : "shared"}`);
    const cached = await readCache(supabaseUrl, serviceKey, cacheKey);
    if (cached) return json({ content: cached, cached: true });

    const instructions = promptFor(action, question);
    const articleContext = [
      `Title: ${title}`,
      `Description: ${description || "Not supplied"}`,
      `Publisher: ${clean(article.source, 200)}`,
      `Topic: ${clean(article.topic, 200)}`,
      `Published: ${clean(article.publishedAt, 200)}`,
      `URL: ${sourceUrl}`,
      `Coverage: Full readable article text retrieved by Kestrel`,
      `Full article text:\n${fullArticle}`,
    ].join("\n");

    const aiResponse = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: { Authorization: `Bearer ${openAiKey}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        model: Deno.env.get("OPENAI_MODEL") || "gpt-4.1-mini",
        store: false,
        instructions: `You are Kestrel Intelligence, an executive research assistant. Today is ${new Date().toISOString().slice(0, 10)}. Read the complete supplied article text before answering. Use only the supplied article context. Never rely on remembered political offices, dates or current events. Clearly label inference, never invent facts or company comparisons, and say when more sources are needed. Begin with "Coverage: Full article". Return concise plain text suitable for a mobile screen.`,
        input: `${instructions}\n\nARTICLE CONTEXT\n${articleContext}`,
        max_output_tokens: action === "summary" ? 800 : 1800,
      }),
    });
    const aiBody = await aiResponse.json();
    if (!aiResponse.ok) throw new Error(aiBody?.error?.message || "OpenAI request failed");
    const content = extractOutputText(aiBody);
    if (!content) throw new Error("OpenAI returned an empty response");

    await writeCache(supabaseUrl, serviceKey, {
      cache_key: cacheKey,
      article_id: articleId,
      user_id: action === "ask" ? user.id : null,
      action,
      question: question || null,
      content,
      source_url: sourceUrl,
    });
    if (!isAdmin) {
      const reset = new Date();
      reset.setUTCMonth(reset.getUTCMonth() + 1, 1);
      reset.setUTCHours(0, 0, 0, 0);
      await fetch(`${supabaseUrl}/rest/v1/profiles?id=eq.${user.id}`, {
        method: "PATCH",
        headers: { ...serviceHeaders(serviceKey), "Content-Type": "application/json" },
        body: JSON.stringify({ monthly_ai_used: usage + 1, usage_reset_at: reset.toISOString() }),
      });
    }
    return json({ content, cached: false });
  } catch (error) {
    console.error(error);
    return json({ error: error instanceof Error ? error.message : "Unable to generate intelligence" }, 500);
  }
});

function promptFor(action: Action, question: string): string {
  if (action === "summary") return "Summarize this article in no more than 150 words. Preserve important names, dates, numbers and decisions. Use 3–5 short bullets when helpful.";
  if (action === "deep_dive") return "Create sections titled What happened, Why it matters, Comparable company moves, Opportunities and risks, and What to watch next. If the supplied context does not contain comparable company evidence, state that additional sourced research is needed rather than guessing.";
  if (action === "learn") return "Create a learning guide grounded in the full article using exactly these headings when they have content: AI Dictionary, AI Concepts, and Also Useful to Learn. Under AI Dictionary, list only AI-specific keywords or acronyms actually present in the article, each on one line formatted '- **Term**: simple definition; brief example if useful.' Under AI Concepts, explain important AI ideas, methods, systems, or approaches actually discussed in the article, each on one line with the same format. Under Also Useful to Learn, optionally explain up to three non-AI terms only when needed to understand this article. Never invent terms or pad the lists. If a section has no entries, omit it. Do not add an unstructured takeaway after the sections.";
  return `Answer this question using the article context: ${question}. Be direct. Distinguish facts in the article from inference.`;
}

async function generateWebResearchFallback(
  openAiKey: string,
  action: Action,
  title: string,
  description: string,
  url: string,
  source: unknown,
  publishedAt: unknown,
  question: string,
): Promise<string> {
  const request = action === "ask"
    ? `Answer this follow-up question about the development: ${question}`
    : promptFor(action, question);
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: { Authorization: `Bearer ${openAiKey}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      model: Deno.env.get("OPENAI_MODEL") || "gpt-4.1-mini",
      store: false,
      tools: [{ type: "web_search" }],
      instructions: "You are Kestrel Intelligence. This is a web research fallback because the selected publisher article could not be read. Search for multiple recent, credible sources about the exact story. Never treat the headline as confirmed fact without evidence. Clearly separate verified reporting from inference, mention disagreements, and include source links/citations. Begin with 'Coverage: Web research'. If there is not enough reliable reporting, return an empty response.",
      input: `${request}\n\nHeadline: ${title}\nPublisher: ${clean(source, 200)}\nPublished: ${clean(publishedAt, 200)}\nArticle URL: ${url}\nDescription: ${description || "Not supplied"}`,
      max_output_tokens: action === "summary" ? 900 : 1800,
    }),
    signal: AbortSignal.timeout(45000),
  });
  if (!response.ok) {
    console.error("openai_web_fallback_http", response.status, (await response.text()).slice(0, 300));
    return "";
  }
  const body = await response.json();
  const content = extractOutputText(body);
  return content.startsWith("Coverage:") ? content : content ? `Coverage: Web research\n${content}` : "";
}

async function fetchReadableArticle(url: string, title: string, publisherUrl: string): Promise<string> {
  let resolvedUrl = url;
  const providersTriedFirst = runHost(url) === "news.google.com";
  if (providersTriedFirst) {
    const providerArticle = await findPublisherArticle(title, publisherUrl, url);
    if (providerArticle.length >= 500) return providerArticle;
  }
  try {
    const response = await fetch(url, {
      redirect: "follow",
      headers: {
        "User-Agent": "Mozilla/5.0 (compatible; KestrelIntelligence/1.0; +https://kestrel.local)",
        Accept: "text/html,application/xhtml+xml",
      },
      signal: AbortSignal.timeout(12000),
    });
    resolvedUrl = response.url || url;
    if (!response.ok) return providersTriedFirst ? "" : await findPublisherArticle(title, publisherUrl, resolvedUrl);
    const contentType = response.headers.get("content-type") ?? "";
    if (!contentType.includes("text/html")) return providersTriedFirst ? "" : await findPublisherArticle(title, publisherUrl, resolvedUrl);
    const html = (await response.text()).slice(0, 1_500_000);
    const preferred = html.match(/<article[\s\S]*?<\/article>/i)?.[0]
      ?? html.match(/<main[\s\S]*?<\/main>/i)?.[0]
      ?? html.match(/<body[\s\S]*?<\/body>/i)?.[0]
      ?? "";
    const extracted = decodeEntities(preferred)
      .replace(/<(script|style|noscript|svg|nav|header|footer|aside)[^>]*>[\s\S]*?<\/\1>/gi, " ")
      .replace(/<br\s*\/?\s*>/gi, "\n")
      .replace(/<\/p\s*>/gi, "\n")
      .replace(/<[^>]+>/g, " ")
      .replace(/[\t\r ]+/g, " ")
      .replace(/\n\s+/g, "\n")
      .replace(/\n{3,}/g, "\n\n")
      .trim()
      .slice(0, 30_000);
    if (extracted.length >= 500 && isArticleContent(extracted, title)) return extracted;
    const readerText = await fetchReaderText(resolvedUrl);
    if (readerText.length >= 500 && isArticleContent(readerText, title)) return readerText;
    return providersTriedFirst ? "" : await findPublisherArticle(title, publisherUrl, resolvedUrl);
  } catch {
    return providersTriedFirst ? "" : await findPublisherArticle(title, publisherUrl, resolvedUrl);
  }
}

async function findPublisherArticle(title: string, publisherUrl: string, resolvedUrl: string): Promise<string> {
  const providerResult = await fetchFromContentProviders(title, publisherUrl, resolvedUrl);
  if (providerResult.content.length >= 500 && isArticleContent(providerResult.content, title)) return providerResult.content;
  const searchResults = await searchArticleWithFirecrawl(title, publisherUrl);
  if (searchResults.content) return searchResults.content;
  const scrapeCandidates = [...new Set([providerResult.articleUrl, searchResults.articleUrl, resolvedUrl].filter((url) => isPublisherArticleUrl(url, publisherUrl)))];
  for (const candidate of scrapeCandidates) {
    const content = await fetchWithFirecrawl(candidate);
    if (content.length >= 500 && isArticleContent(content, title)) return content;
  }
  try {
    const host = runHost(publisherUrl);
    if (!host) return "";
    const search = await fetch(`https://s.jina.ai/${encodeURIComponent(`${title} site:${host}`)}`, {
      headers: { Accept: "text/plain" }, signal: AbortSignal.timeout(15000),
    });
    if (!search.ok) return "";
    const results = await search.text();
    const escapedHost = host.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const match = results.match(new RegExp(`https?://(?:www\\.)?${escapedHost}/[^\\s)\\]]+`, "i"));
    if (!match) return "";
    const readerText = await fetchReaderText(match[0]);
    if (readerText.length >= 500 && isArticleContent(readerText, title)) return readerText;
    const crawled = await fetchWithFirecrawl(match[0]);
    return crawled.length >= 500 && isArticleContent(crawled, title) ? crawled : "";
  } catch {
    return "";
  }
}

async function searchArticleWithFirecrawl(title: string, publisherUrl: string): Promise<{content: string; articleUrl: string}> {
  const key = Deno.env.get("FIRECRAWL_API_KEY") ?? "";
  if (!key) return { content: "", articleUrl: "" };
  try {
    const host = runHost(publisherUrl);
    const response = await fetch("https://api.firecrawl.dev/v2/search", {
      method: "POST",
      headers: { Authorization: `Bearer ${key}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        query: `\"${title}\"${host ? ` site:${host}` : ""}`,
        limit: 5,
        sources: ["web"],
        scrapeOptions: { formats: ["markdown"], onlyMainContent: true },
      }),
      signal: AbortSignal.timeout(25000),
    });
    if (!response.ok) {
      console.error(`firecrawl_search_http_${response.status}`, (await response.text()).slice(0, 300));
      return { content: "", articleUrl: "" };
    }
    const data = await response.json();
    const candidates = [...(data?.data?.web ?? []), ...(data?.data?.news ?? [])];
    const normalizedTitle = normalizeTitle(title);
    const ranked = candidates.map((item: any) => {
      const candidateTitle = normalizeTitle(clean(item?.title, 600));
      const candidateHost = runHost(clean(item?.url, 2000));
      const titleScore = candidateTitle === normalizedTitle ? 100 : candidateTitle.includes(normalizedTitle) || normalizedTitle.includes(candidateTitle) ? 55 : 0;
      const hostScore = host && (candidateHost === host || candidateHost.endsWith(`.${host}`)) ? 35 : 0;
      return { score: titleScore + hostScore, url: clean(item?.url, 2000), content: clean(item?.markdown, 30_000) };
    }).sort((a: any, b: any) => b.score - a.score);
    const match = ranked.find((item: any) => item.score >= 55) ?? ranked[0];
    if (match?.content.length >= 500 && isArticleContent(match.content, title)) return { content: match.content, articleUrl: match.url };
    return { content: "", articleUrl: match?.url ?? "" };
  } catch (error) {
    console.error("firecrawl_search_failed", error);
    return { content: "", articleUrl: "" };
  }
}

function isPublisherArticleUrl(url: string, publisherUrl: string): boolean {
  const host = runHost(url);
  const publisherHost = runHost(publisherUrl);
  return Boolean(host && publisherHost && (host === publisherHost || host.endsWith(`.${publisherHost}`)));
}

async function fetchFromContentProviders(title: string, publisherUrl: string, resolvedUrl: string): Promise<{content: string; articleUrl: string}> {
  const gnewsKey = Deno.env.get("GNEWS_API_KEY") ?? "";
  let directUrl = "";
  if (gnewsKey) {
    try {
      const endpoint = new URL("https://gnews.io/api/v4/search");
      endpoint.searchParams.set("q", title);
      endpoint.searchParams.set("lang", "en");
      endpoint.searchParams.set("max", "10");
      endpoint.searchParams.set("expand", "content");
      endpoint.searchParams.set("apikey", gnewsKey);
      const response = await fetch(endpoint, { signal: AbortSignal.timeout(15000) });
      if (response.ok) {
        const data = await response.json();
        const hosts = [runHost(publisherUrl), runHost(resolvedUrl)].filter(Boolean);
        const candidates = Array.isArray(data?.articles) ? data.articles : [];
        const targetTitle = normalizeTitle(title);
        const ranked = candidates.map((item: any) => {
          const itemHost = runHost(item?.url ?? "");
          const candidateTitle = normalizeTitle(clean(item?.title, 600));
          const titleScore = candidateTitle === targetTitle ? 100 : candidateTitle.includes(targetTitle) || targetTitle.includes(candidateTitle) ? 55 : 0;
          const hostScore = hosts.some((host) => itemHost === host || itemHost.endsWith(`.${host}`)) ? 35 : 0;
          return { score: titleScore + hostScore, content: clean(item?.content, 30_000), url: clean(item?.url, 2000) };
        }).sort((a: any, b: any) => b.score - a.score);
        const article = ranked.find((item: any) => item.score >= 55) ?? ranked[0];
        if (article) {
          directUrl = article.url;
          if (article.content.length >= 500 && isArticleContent(article.content, title)) return { content: article.content, articleUrl: directUrl };
        }
      } else {
        console.error(`gnews_http_${response.status}`, (await response.text()).slice(0, 300));
      }
    } catch (error) { console.error("gnews_request_failed", error); }
  }
  return { content: "", articleUrl: directUrl };
}

async function fetchWithFirecrawl(url: string): Promise<string> {
  const key = Deno.env.get("FIRECRAWL_API_KEY") ?? "";
  if (!key) return "";
  try {
    const response = await fetch("https://api.firecrawl.dev/v2/scrape", {
      method: "POST",
      headers: { Authorization: `Bearer ${key}`, "Content-Type": "application/json" },
      body: JSON.stringify({ url, formats: ["markdown"], onlyMainContent: true, timeout: 15000 }),
      signal: AbortSignal.timeout(20000),
    });
    if (!response.ok) {
      const errorText = await response.text();
      console.error(`firecrawl_http_${response.status}`, errorText.slice(0, 300));
      return "";
    }
    const data = await response.json();
    const markdown = clean(data?.data?.markdown ?? data?.markdown, 30_000);
    if (!markdown) console.error("firecrawl_empty_content", JSON.stringify(data).slice(0, 300));
    return markdown;
  } catch (error) { console.error("firecrawl_request_failed", error); return ""; }
}

function normalizeTitle(value: string): string {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, " ").trim().replace(/\s+/g, " ");
}

function isArticleContent(content: string, title: string): boolean {
  if (content.length < 500) return false;
  const normalizedContent = normalizeTitle(content.slice(0, 5000));
  const words = normalizeTitle(title).split(" ").filter((word) => word.length > 3);
  if (!words.length) return true;
  const matches = words.filter((word) => normalizedContent.includes(word)).length;
  return matches >= Math.min(2, words.length);
}

function runHost(value: string): string {
  try { return new URL(value).hostname.replace(/^www\./, ""); } catch { return ""; }
}

async function fetchReaderText(url: string): Promise<string> {
  try {
    const readerUrl = `https://r.jina.ai/http://${url.replace(/^https?:\/\//i, "")}`;
    const response = await fetch(readerUrl, {
      headers: { Accept: "text/plain" },
      signal: AbortSignal.timeout(15000),
    });
    if (!response.ok) return "";
    return (await response.text()).replace(/\n{3,}/g, "\n\n").trim().slice(0, 30_000);
  } catch {
    return "";
  }
}

function decodeEntities(value: string): string {
  return value
    .replace(/&nbsp;/gi, " ")
    .replace(/&amp;/gi, "&")
    .replace(/&quot;/gi, '"')
    .replace(/&#39;|&apos;/gi, "'")
    .replace(/&lt;/gi, "<")
    .replace(/&gt;/gi, ">");
}

function extractOutputText(value: any): string {
  if (typeof value.output_text === "string") return value.output_text.trim();
  return (value.output ?? []).flatMap((item: any) => item.content ?? [])
    .filter((part: any) => part.type === "output_text")
    .map((part: any) => part.text ?? "").join("\n").trim();
}

async function readCache(url: string, key: string, cacheKey: string): Promise<string | null> {
  const response = await fetch(`${url}/rest/v1/article_intelligence_cache?select=content&cache_key=eq.${cacheKey}&limit=1`, { headers: serviceHeaders(key) });
  if (!response.ok) return null;
  const rows = await response.json();
  return rows?.[0]?.content || null;
}

async function writeCache(url: string, key: string, row: Record<string, unknown>) {
  await fetch(`${url}/rest/v1/article_intelligence_cache`, {
    method: "POST",
    headers: { ...serviceHeaders(key), "Content-Type": "application/json", Prefer: "resolution=merge-duplicates" },
    body: JSON.stringify(row),
  });
}

function serviceHeaders(key: string) { return { apikey: key, Authorization: `Bearer ${key}` }; }
function requiredEnv(name: string): string { const value = Deno.env.get(name); if (!value) throw new Error(`${name} is not configured`); return value; }
function clean(value: unknown, max: number): string { return typeof value === "string" ? value.trim().slice(0, max) : ""; }
async function sha256(value: string): Promise<string> { const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)); return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join(""); }
function json(body: unknown, status = 200) { return new Response(JSON.stringify(body), { status, headers: { ...corsHeaders, "Content-Type": "application/json" } }); }
