-- Run this migration in the Supabase SQL editor before enabling production sign-in.
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null,
  full_name text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.user_preferences (
  user_id uuid primary key references auth.users(id) on delete cascade,
  selected_topic_id text not null default 'all',
  selected_work_accounts text not null default '',
  custom_work_accounts text not null default '',
  deleted_work_account_ids text not null default '',
  updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.user_preferences enable row level security;

create policy "Users read their own profile" on public.profiles
  for select using (auth.uid() = id);
create policy "Users create their own profile" on public.profiles
  for insert with check (auth.uid() = id);
create policy "Users update their own profile" on public.profiles
  for update using (auth.uid() = id) with check (auth.uid() = id);

create policy "Users read their own preferences" on public.user_preferences
  for select using (auth.uid() = user_id);
create policy "Users create their own preferences" on public.user_preferences
  for insert with check (auth.uid() = user_id);
create policy "Users update their own preferences" on public.user_preferences
  for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

revoke all on public.profiles from anon;
revoke all on public.user_preferences from anon;
grant select, insert, update on public.profiles to authenticated;
grant select, insert, update on public.user_preferences to authenticated;
