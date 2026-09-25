package com.example

import com.example.ai.LocalLlmModel
import com.example.ai.LocalLlmStreamBuffer
import com.example.data.repository.AssistantPrompts
import com.example.data.repository.AssistantTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * The on-device LLM itself needs a device, but everything around it that can be wrong on a desk is
 * plain logic: which file gets loaded, how sizes are reported, and what exactly is sent to the
 * model. Those are pinned here.
 */
class LocalLlmTest {

    private fun tempDir(): File = Files.createTempDirectory("ahex-local-llm").toFile()

    private fun modelFile(dir: File, name: String, size: Int = 64, modified: Long = 1_000L): File {
        val file = File(dir, name)
        file.writeBytes(ByteArray(size))
        if (modified > 0L) file.setLastModified(modified)
        return file
    }

    // ---------------------------------------------------------------------------------------
    // Model discovery
    // ---------------------------------------------------------------------------------------

    @Test
    fun `only litertlm bundles count as models`() {
        assertTrue(LocalLlmModel.isSupportedFileName("gemma-3-1b-it.litertlm"))
        assertTrue(LocalLlmModel.isSupportedFileName("MODEL.LITERTLM"))
        assertFalse(LocalLlmModel.isSupportedFileName("gemma.task"))
        assertFalse(LocalLlmModel.isSupportedFileName("model.bin"))
        assertFalse(LocalLlmModel.isSupportedFileName("litertlm"))
        assertFalse(LocalLlmModel.isSupportedFileName(""))
    }

    @Test
    fun `an installed model wins over anything discovered`() {
        val configuredDir = tempDir()
        val managedDir = tempDir()
        val configured = modelFile(configuredDir, "chosen.litertlm")
        modelFile(managedDir, "other.litertlm")

        val resolved = LocalLlmModel.resolve(configured, listOf(managedDir), null)

        assertEquals(configured.absolutePath, resolved?.absolutePath)
    }

    @Test
    fun `a stale configured path falls back to the managed directory`() {
        val managedDir = tempDir()
        val discovered = modelFile(managedDir, "fallback.litertlm")

        val resolved = LocalLlmModel.resolve(File(managedDir, "deleted.litertlm"), listOf(managedDir), null)

        assertEquals(discovered.absolutePath, resolved?.absolutePath)
    }

    @Test
    fun `the adb directory is only used when nothing is managed`() {
        val emptyManaged = tempDir()
        val devDir = tempDir()
        val pushed = modelFile(devDir, "pushed.litertlm")

        assertEquals(pushed.absolutePath, LocalLlmModel.resolve(null, listOf(emptyManaged), devDir)?.absolutePath)
        assertNull(LocalLlmModel.resolve(null, listOf(emptyManaged), null))
    }

    @Test
    fun `empty and unrelated files are ignored`() {
        val dir = tempDir()
        modelFile(dir, "empty.litertlm", size = 0)
        modelFile(dir, "notes.txt", size = 128)

        assertTrue(LocalLlmModel.listModels(dir).isEmpty())
        assertNull(LocalLlmModel.resolve(null, listOf(dir), null))
    }

    @Test
    fun `the newest bundle is listed first`() {
        val dir = tempDir()
        val older = modelFile(dir, "older.litertlm", modified = 1_000L)
        val newer = modelFile(dir, "newer.litertlm", modified = 9_000L)

        val models = LocalLlmModel.listModels(dir)

        assertEquals(listOf(newer.absolutePath, older.absolutePath), models.map { it.absolutePath })
    }

    @Test
    fun `sizes are reported in readable units`() {
        assertEquals("512 B", LocalLlmModel.formatSize(512))
        assertEquals("2 KB", LocalLlmModel.formatSize(2L * 1024L))
        assertEquals("2 MB", LocalLlmModel.formatSize(2L * 1024L * 1024L))
        assertEquals("1.50 GB", LocalLlmModel.formatSize(1_610_612_736L))
    }

    // ---------------------------------------------------------------------------------------
    // Streaming buffer
    // ---------------------------------------------------------------------------------------

    @Test
    fun `the first chunk is published straight away`() {
        val buffer = LocalLlmStreamBuffer(intervalNanos = 80_000_000L)

        assertEquals("Hello", buffer.append("Hello", nowNanos = 0L))
    }

    @Test
    fun `chunks inside the interval are merged instead of published separately`() {
        val buffer = LocalLlmStreamBuffer(intervalNanos = 80_000_000L)

        assertEquals("Hello", buffer.append("Hello", nowNanos = 0L))
        assertNull("too soon to publish again", buffer.append(" wor", nowNanos = 1_000_000L))
        assertNull(buffer.append("ld", nowNanos = 79_999_999L))
        assertEquals(
            "the next publish carries everything received so far",
            "Hello world!",
            buffer.append("!", nowNanos = 80_000_000L)
        )
    }

    @Test
    fun `a zero interval publishes every chunk`() {
        val buffer = LocalLlmStreamBuffer(intervalNanos = 0L)

        assertEquals("a", buffer.append("a", nowNanos = 0L))
        assertEquals("ab", buffer.append("b", nowNanos = 0L))
    }

    @Test
    fun `empty chunks never publish`() {
        val buffer = LocalLlmStreamBuffer(intervalNanos = 80_000_000L)

        assertNull(buffer.append("", nowNanos = 0L))
        assertEquals("", buffer.snapshot())
    }

    @Test
    fun `chunk whitespace survives because chunks are deltas`() {
        val buffer = LocalLlmStreamBuffer(intervalNanos = 0L)

        buffer.append("Reading ", nowNanos = 0L)
        buffer.append("is good", nowNanos = 1L)

        assertEquals("Reading is good", buffer.snapshot())
    }

    @Test
    fun `the finished answer is trimmed while the snapshot is exact`() {
        val buffer = LocalLlmStreamBuffer(intervalNanos = 0L)

        buffer.append("\n\n  Final answer  \n", nowNanos = 0L)

        assertEquals("Final answer", buffer.completed())
        assertEquals("\n\n  Final answer  \n", buffer.snapshot())
    }

    // ---------------------------------------------------------------------------------------
    // Prompt assembly
    // ---------------------------------------------------------------------------------------

    @Test
    fun `a short passage is not windowed`() {
        val passage = "It was the best of times."

        assertEquals(passage, AssistantPrompts.window(passage, 4_000))
    }

    @Test
    fun `a long passage keeps its opening and its ending`() {
        val passage = (1..200).joinToString(" ") { "w$it" }
        val windowed = AssistantPrompts.window(passage, 300)

        assertTrue("windowed text should be shorter", windowed.length < passage.length)
        assertTrue("the opening must survive", windowed.startsWith("w1 w2 w3"))
        assertTrue("the ending must survive", windowed.endsWith("w200"))
        assertTrue("the omission must be visible", windowed.contains("middle of the passage omitted"))
    }

    @Test
    fun `the reading turn carries the metadata the passage and the request`() {
        val turn = AssistantPrompts.buildReadingTurn(
            task = AssistantTask.ASK,
            instructions = "TASK: Answer the reader's question using only this passage.",
            bookTitle = "Meditations",
            author = "Marcus Aurelius",
            genre = "Philosophy",
            chapterTitle = "Book II",
            passage = "Begin the morning by saying to thyself, I shall meet with the busy-body.",
            isArabic = false,
            query = "What does the author advise at the start of the day?",
            maxChars = 4_000
        )

        assertTrue(turn.contains("TASK: Answer the reader's question"))
        assertTrue(turn.contains("WORK: Meditations"))
        assertTrue(turn.contains("AUTHOR: Marcus Aurelius"))
        assertTrue(turn.contains("PASSAGE TITLE: Book II"))
        assertTrue(turn.contains("PASSAGE LANGUAGE: English"))
        assertTrue(turn.contains("--- BEGIN PASSAGE ---"))
        assertTrue(turn.contains("--- END PASSAGE ---"))
        assertTrue(turn.contains("Begin the morning by saying to thyself"))
        assertTrue(turn.contains("READER QUESTION: What does the author advise"))
    }

    @Test
    fun `the reader question is left out of tasks that do not ask one`() {
        val turn = AssistantPrompts.buildReadingTurn(
            task = AssistantTask.SUMMARY_AND_KEY_POINTS,
            instructions = "TASK: Summarise the passage.",
            bookTitle = "Meditations",
            author = "Marcus Aurelius",
            genre = "Philosophy",
            chapterTitle = "Book II",
            passage = "Begin the morning by saying to thyself.",
            isArabic = false,
            query = "leftover text from the ask tab",
            maxChars = 4_000
        )

        assertFalse(turn.contains("READER QUESTION"))
        assertFalse(turn.contains("leftover text from the ask tab"))
    }

    @Test
    fun `an arabic passage is announced as arabic`() {
        val turn = AssistantPrompts.buildReadingTurn(
            task = AssistantTask.SUMMARY_AND_KEY_POINTS,
            instructions = "TASK: Summarise the passage.",
            bookTitle = "كليلة ودمنة",
            author = "ابن المقفع",
            genre = "Philosophy",
            chapterTitle = "الباب الأول",
            passage = "قال الفيلسوف: هذا كتاب أهديته إلى الملك.",
            isArabic = true,
            query = "",
            maxChars = 4_000
        )

        assertTrue(turn.contains("PASSAGE LANGUAGE: Arabic"))
    }

    @Test
    fun `the on-device turn is smaller than the cloud turn`() {
        val passage = (1..2_000).joinToString(" ") { "word$it" }

        val cloud = AssistantPrompts.buildReadingTurn(
            task = AssistantTask.DEEP_ANALYSIS,
            instructions = "TASK: Analyse the passage.",
            bookTitle = "Book",
            author = "Author",
            genre = "General",
            chapterTitle = "Chapter",
            passage = passage,
            isArabic = false,
            query = "",
            maxChars = 12_000
        )

        val onDevice = AssistantPrompts.buildReadingTurn(
            task = AssistantTask.DEEP_ANALYSIS,
            instructions = "TASK: Analyse the passage.",
            bookTitle = "Book",
            author = "Author",
            genre = "General",
            chapterTitle = "Chapter",
            passage = passage,
            isArabic = false,
            query = "",
            maxChars = 4_000
        )

        assertTrue(cloud.length > onDevice.length)
        assertTrue("the opening survives", onDevice.contains("word1 "))
        assertFalse("the middle of the chapter must not reach a 1B model", onDevice.contains("word1000"))
        assertTrue("the ending survives", onDevice.contains("word2000"))
        assertTrue(onDevice.contains("middle of the passage omitted"))
    }
}
