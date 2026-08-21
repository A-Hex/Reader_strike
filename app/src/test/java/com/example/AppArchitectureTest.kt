package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import com.example.reader.EpubParser
import com.example.recommendation.RecommendationEngine
import com.example.util.SocialShareHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30, 34]) // Testing Android 11 (SDK 30) and Android 14 (SDK 34)
class AppArchitectureTest {

    @Test
    fun testAndroid11ContextAndResources() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("A-Hex streak", appName)
    }

    @Test
    fun testEpubParserEmptyStream() {
        val emptyStream = ByteArrayInputStream(ByteArray(0))
        val result = EpubParser.parseEpubStream(emptyStream, "Empty Test")
        assertNotNull(result)
        assertEquals("Empty Test", result.title)
        assertTrue(result.chapters.isNotEmpty())
    }

    @Test
    fun testEpubParserPlainTextStream() {
        val plainText = "Chapter 1: The Beginning\n\nThis is paragraph one.\n\nThis is paragraph two."
        val stream = ByteArrayInputStream(plainText.toByteArray(Charsets.UTF_8))
        val result = EpubParser.parsePlainTextStream(stream, "Plain Text Book")
        assertNotNull(result)
        assertEquals("Plain Text Book", result.title)
        assertTrue(result.chapters.isNotEmpty())
        assertTrue(result.chapters[0].content.contains("This is paragraph one."))
    }

    @Test
    fun testRecommendationEngineWithEmptyLibrary() {
        val catalog = listOf(
            Book(
                id = "cat_1",
                title = "Meditations",
                author = "Marcus Aurelius",
                description = "Classic Stoic reflections",
                genre = "Philosophy",
                format = BookFormat.EPUB
            )
        )
        val recommendations = RecommendationEngine.generateRecommendations(
            userLibrary = emptyList(),
            userHighlights = emptyList(),
            catalog = catalog
        )
        assertNotNull(recommendations)
        assertEquals(1, recommendations.size)
        assertEquals("cat_1", recommendations[0].book.id)
    }

    @Test
    fun testSocialShareHelperFormats() {
        val streak = ReadingStreakData(
            currentStreakDays = 7,
            totalMinutesRead = 140,
            totalBooksRead = 2
        )
        val shareText = SocialShareHelper.formatStatsShareText(streak)
        assertTrue(shareText.contains("7 Consecutive Days"))
        assertTrue(shareText.contains("A-Hex streak"))

        val highlight = Highlight(
            id = "hl_1",
            bookId = "book_1",
            bookTitle = "Atomic Habits",
            chapterIndex = 0,
            chapterTitle = "Fundamentals",
            text = "Small habits make a big difference.",
            note = "Key insight",
            color = HighlightColor.AMBER,
            pageOrLocation = 12
        )
        val hlText = SocialShareHelper.formatHighlightShareText(highlight)
        assertTrue(hlText.contains("Small habits make a big difference."))
        assertTrue(hlText.contains("Atomic Habits"))
    }
}
