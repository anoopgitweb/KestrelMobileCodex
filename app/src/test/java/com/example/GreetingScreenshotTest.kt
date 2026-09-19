package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.NewsArticle
import com.example.ui.components.NewsCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleArticle = NewsArticle(
      id = "test_1",
      title = "OpenAI Announces Advanced Autonomous Reasoning Benchmark",
      description = "A comprehensive evaluation framework testing reasoning depth, mathematical consistency, and complex tool execution.",
      link = "https://example.com/openai",
      source = "TechCrunch",
      publishedAtMillis = 1790000000000L,
      topicId = "openai",
      topicName = "OpenAI"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        NewsCard(
          article = sampleArticle,
          onClick = {},
          onBookmarkToggle = {},
          onShare = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun topic_picker_with_all_screenshot() {
    val sampleTopics = listOf(
      com.example.data.model.Topic.ALL_TOPIC,
      com.example.data.model.Topic(
        id = "ai",
        name = "Artificial Intelligence",
        query = "Artificial Intelligence",
        isDefault = true,
        iconName = "psychology"
      ),
      com.example.data.model.Topic(
        id = "ml",
        name = "Machine Learning",
        query = "Machine Learning",
        isDefault = true,
        iconName = "hub"
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.TopicPicker(
          topics = sampleTopics,
          selectedTopic = com.example.data.model.Topic.ALL_TOPIC,
          isBookmarksView = false,
          onTopicSelected = {},
          onAddTopicClicked = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/topic_picker_all.png")
  }

  @Test
  fun news_card_with_sheets_status_screenshot() {
    val sampleArticle = NewsArticle(
      id = "test_sheets",
      title = "Anthropic Unveils Claude 3.7 Sonnet with Hybrid Reasoning",
      description = "Next-generation model combines instant conversational responses with extended step-by-step thinking.",
      link = "https://example.com/claude",
      source = "VentureBeat",
      publishedAtMillis = 1790000000000L,
      topicId = "anthropic",
      topicName = "Anthropic",
      isSavedToSheets = true,
      sheetsUrl = "https://docs.google.com/spreadsheets/d/sample"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        NewsCard(
          article = sampleArticle,
          onClick = {},
          onBookmarkToggle = {},
          onShare = {},
          onSaveToSheets = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/news_card_sheets.png")
  }

  @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
  @Test
  fun pull_to_refresh_box_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
          isRefreshing = true,
          onRefresh = {},
          modifier = androidx.compose.ui.Modifier.fillMaxSize()
        ) {
          androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
          ) {
            androidx.compose.material3.Text("Feed refreshing...")
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/pull_to_refresh.png")
  }
}
