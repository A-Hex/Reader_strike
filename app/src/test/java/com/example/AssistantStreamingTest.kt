package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.AssistantFailure
import com.example.data.repository.AssistantProgress
import com.example.data.repository.AssistantResult
import com.example.data.repository.AssistantTask
import com.example.model.Book
import com.example.model.BookChapter
import com.example.model.BookFormat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The contract the assistant sheet relies on to update itself incrementally: a run emits
 * [AssistantProgress.Partial] events only while an engine is actually writing, and always ends with
 * exactly one [AssistantProgress.Done].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AssistantStreamingTest {

    private val book = Book(
        id = "book_1",
        title = "Meditations",
        author = "Marcus Aurelius",
        description = "Stoic reflections",
        format = BookFormat.EPUB,
        genre = "Philosophy"
    )

    private val passage = (1..40).joinToString(" ") {
        "Sentence number $it states a plain fact about the passage under test."
    }

    private fun repository(): AiAssistantRepository =
        AiAssistantRepository(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `a passage below the minimum length fails at once with a single terminal event`() = runBlocking {
        val events = repository().runStreaming(
            task = AssistantTask.SUMMARY_AND_KEY_POINTS,
            book = book,
            chapter = BookChapter(index = 0, title = "Too short", content = "Only a few words.")
        ).toList()

        assertEquals("a run must terminate exactly once", 1, events.size)
        val done = events.single() as AssistantProgress.Done
        val failure = done.result as AssistantResult.Failure
        assertEquals(AssistantFailure.EMPTY_RESPONSE, failure.reason)
        assertNull(failure.offlineMarkdown)
    }

    @Test
    fun `with no installed bundle nothing is streamed but the run still terminates once`() = runBlocking {
        val repository = repository()
        assertFalse("a clean test device has no model bundle", repository.hasLocalModel())

        val events = repository.runStreaming(
            task = AssistantTask.SUMMARY_AND_KEY_POINTS,
            book = book,
            chapter = BookChapter(index = 0, title = "Chapter 1", content = passage)
        ).toList()

        assertTrue(
            "only a streaming engine emits partial answers",
            events.none { it is AssistantProgress.Partial }
        )
        assertEquals(1, events.size)
        assertTrue(events.single() is AssistantProgress.Done)
    }

    @Test
    fun `the terminal event is always last even when partials precede it`() = runBlocking {
        // Streams nothing on this device, but pins the ordering rule the sheet depends on: every
        // Partial arrives before the single Done, never after.
        val events = repository().runStreaming(
            task = AssistantTask.DEEP_ANALYSIS,
            book = book,
            chapter = BookChapter(index = 0, title = "Chapter 1", content = passage)
        ).toList()

        assertTrue(events.isNotEmpty())
        assertTrue("the last event must be terminal", events.last() is AssistantProgress.Done)
        assertEquals(1, events.count { it is AssistantProgress.Done })
    }
}
