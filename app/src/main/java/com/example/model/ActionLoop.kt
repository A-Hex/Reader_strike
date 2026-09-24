package com.example.model

/**
 * The four stages of the "Read -> Build" habit loop.
 *
 * The habit is deliberately small: read with one real problem in mind, shrink that problem
 * until it is clear, find the idea in the book that speaks to it, then build one action from it.
 * Every stage is a plain sentence the reader writes themselves, so this is real self-reflection,
 * not generated filler.
 */
enum class ActionLoopStage(
    val stepNumber: Int,
    val title: String,
    val prompt: String
) {
    READ(
        stepNumber = 1,
        title = "Read",
        prompt = "What did you read? Chapter, passage or page."
    ),
    COMPRESS(
        stepNumber = 2,
        title = "Compress the problem",
        prompt = "Shrink the real problem in your life into one or two clear sentences."
    ),
    APPLY(
        stepNumber = 3,
        title = "Apply the idea",
        prompt = "Which idea in what you read speaks to that problem?"
    ),
    BUILD(
        stepNumber = 4,
        title = "Build it",
        prompt = "One concrete action you will do in the next 24 hours."
    )
}

/**
 * One complete turn of the loop. Nothing here is auto-generated: [readNote], [problem],
 * [bookIdea] and [action] are the reader's own words.
 */
data class ActionLoop(
    val id: String,
    val bookTitle: String = "",
    val readNote: String = "",
    val problem: String = "",
    val bookIdea: String = "",
    val action: String = "",
    val isBuilt: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    /**
     * Step 1 in the reader's own words: their notes when they wrote any, otherwise the book
     * itself. Naming what you read already answers "what did you read?".
     */
    val readStep: String
        get() = readNote.ifBlank { bookTitle }

    /** Number of the four stages the reader has actually filled in (0..4). */
    val completedSteps: Int
        get() = listOf(readStep, problem, bookIdea, action).count { it.isNotBlank() }

    val isReadyToBuild: Boolean
        get() = completedSteps == ActionLoopStage.entries.size

    /** Progress through the loop as a 0f..1f fraction for the UI. */
    val progress: Float
        get() = completedSteps.toFloat() / ActionLoopStage.entries.size.toFloat()

    /** The next stage still missing text, or null once every stage is written. */
    val nextStage: ActionLoopStage?
        get() = when {
            readStep.isBlank() -> ActionLoopStage.READ
            problem.isBlank() -> ActionLoopStage.COMPRESS
            bookIdea.isBlank() -> ActionLoopStage.APPLY
            action.isBlank() -> ActionLoopStage.BUILD
            else -> null
        }
}
