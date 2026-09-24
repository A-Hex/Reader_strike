package com.example

import com.example.model.ActionLoop
import com.example.model.ActionLoopStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure logic for the "Read -> Build" habit loop. No Android dependency, so it runs as a plain
 * JVM test.
 */
class ActionLoopTest {

    @Test
    fun emptyLoopHasNoCompletedStepsAndStartsAtRead() {
        val loop = ActionLoop(id = "loop-1")

        assertEquals(4, ActionLoopStage.entries.size)
        assertEquals(0, loop.completedSteps)
        assertEquals(0f, loop.progress, 0.0001f)
        assertFalse(loop.isReadyToBuild)
        assertEquals(ActionLoopStage.READ, loop.nextStage)
    }

    @Test
    fun nextStageFollowsTheHabitOrder() {
        val loop = ActionLoop(
            id = "loop-2",
            readNote = "Chapter 3",
            problem = "I keep postponing deep work",
            bookIdea = "Protect a fixed block of time",
            action = ""
        )

        assertEquals(3, loop.completedSteps)
        assertEquals(ActionLoopStage.BUILD, loop.nextStage)
        assertFalse(loop.isReadyToBuild)
        assertEquals(0.75f, loop.progress, 0.0001f)
    }

    @Test
    fun fullyWrittenLoopIsReadyToBuild() {
        val loop = ActionLoop(
            id = "loop-3",
            bookTitle = "Deep Work",
            readNote = "Chapter 3",
            problem = "I keep postponing deep work",
            bookIdea = "Protect a fixed block of time",
            action = "Block 07:00-08:00 tomorrow with no phone"
        )

        assertEquals(4, loop.completedSteps)
        assertEquals(1f, loop.progress, 0.0001f)
        assertTrue(loop.isReadyToBuild)
        assertNull(loop.nextStage)
    }

    @Test
    fun namingTheBookCompletesTheReadStep() {
        val loop = ActionLoop(id = "loop-5", bookTitle = "Meditations")

        assertEquals("Meditations", loop.readStep)
        assertEquals(1, loop.completedSteps)
        assertEquals(ActionLoopStage.COMPRESS, loop.nextStage)
    }

    @Test
    fun writtenNotesTakePrecedenceOverTheBookTitle() {
        val loop = ActionLoop(
            id = "loop-6",
            bookTitle = "Meditations",
            readNote = "Book IV - the obstacle is the way"
        )

        assertEquals("Book IV - the obstacle is the way", loop.readStep)
        assertEquals(1, loop.completedSteps)
        assertEquals(ActionLoopStage.COMPRESS, loop.nextStage)
    }

    @Test
    fun blankWhitespaceDoesNotCountAsAWrittenStep() {
        val loop = ActionLoop(id = "loop-4", readNote = "   ", problem = "\n")

        assertEquals(0, loop.completedSteps)
        assertEquals(ActionLoopStage.READ, loop.nextStage)
    }
}
