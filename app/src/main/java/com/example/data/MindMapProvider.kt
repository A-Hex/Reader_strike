package com.example.data

import com.example.model.*

object MindMapProvider {

    fun getMindMapForBook(book: Book): BookMindMap {
        return when (book.id) {
            "book-sherlock-holmes" -> getSherlockHolmesMap()
            "book-frankenstein" -> getFrankensteinMap()
            "book-alice-wonderland" -> getAliceMap()
            "book-art-of-war" -> getArtOfWarMap()
            "book-meditations" -> getMeditationsMap()
            else -> generateDynamicMindMap(book)
        }
    }

    private fun getSherlockHolmesMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "holmes",
                name = "Sherlock Holmes",
                role = "Consulting Detective",
                faction = "221B Baker Street",
                description = "Master of deduction, ratiocination, and forensic observation.",
                keyQuote = "When you have eliminated the impossible, whatever remains, however improbable, must be the truth.",
                avatarEmoji = "🕵️",
                xPercent = 0.50f,
                yPercent = 0.35f,
                significance = 1.3f
            ),
            CharacterNode(
                id = "watson",
                name = "Dr. John Watson",
                role = "Biographer & Surgeon",
                faction = "221B Baker Street",
                description = "Former army surgeon wounded at Maiwand; loyal companion and narrator.",
                keyQuote = "It was a rare companionship based on mutual respect and shared inquiry.",
                avatarEmoji = "🩺",
                xPercent = 0.22f,
                yPercent = 0.65f,
                significance = 1.15f
            ),
            CharacterNode(
                id = "gregson",
                name = "Inspector Gregson",
                role = "Scotland Yard Detective",
                faction = "Official Law Enforcement",
                description = "The smartest officer in Scotland Yard, frequently seeking Holmes's counsel.",
                keyQuote = "There has been a bad business during the night at Lauriston Gardens...",
                avatarEmoji = "👮",
                xPercent = 0.78f,
                yPercent = 0.62f,
                significance = 1.0f
            ),
            CharacterNode(
                id = "drebber",
                name = "Enoch Drebber",
                role = "Murder Victim",
                faction = "The American Connection",
                description = "Wealthy traveller found dead with no visible wound, bearing the inscription RACHE.",
                keyQuote = "Found in the empty room with cards from Cleveland, Ohio.",
                avatarEmoji = "💼",
                xPercent = 0.50f,
                yPercent = 0.88f,
                significance = 0.95f
            ),
            CharacterNode(
                id = "moriarty",
                name = "Prof. Moriarty",
                role = "Napoleon of Crime",
                faction = "Criminal Underworld",
                description = "The intellectual arch-nemesis who sits motionless at the center of a web.",
                keyQuote = "He is the organizer of half that is evil and of nearly all that is undetected.",
                avatarEmoji = "🕸️",
                xPercent = 0.82f,
                yPercent = 0.18f,
                significance = 1.2f
            )
        )

        val edges = listOf(
            RelationshipEdge("watson", "holmes", "Trusted Partner & Biographer", RelationType.ALLY),
            RelationshipEdge("gregson", "holmes", "Consults for Insight", RelationType.INVESTIGATION),
            RelationshipEdge("holmes", "drebber", "Investigates Murder", RelationType.INVESTIGATION),
            RelationshipEdge("holmes", "moriarty", "Intellectual Rivalry", RelationType.RIVAL),
            RelationshipEdge("gregson", "drebber", "Official Crime Case", RelationType.INVESTIGATION)
        )

        return BookMindMap(
            bookId = "book-sherlock-holmes",
            bookTitle = "A Study in Scarlet",
            nodes = nodes,
            edges = edges,
            thematicSummary = "Explores the contrast between rigid institutional investigation and revolutionary scientific ratiocination in Victorian London."
        )
    }

    private fun getFrankensteinMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "victor",
                name = "Victor Frankenstein",
                role = "Natural Philosopher & Creator",
                faction = "Geneva / Ingolstadt Academy",
                description = "Driven by unremitting ardour to conquer the boundary between life and death.",
                keyQuote = "Life and death appeared to me ideal bounds, which I should first break through.",
                avatarEmoji = "⚡",
                xPercent = 0.48f,
                yPercent = 0.30f,
                significance = 1.3f
            ),
            CharacterNode(
                id = "creature",
                name = "The Creature",
                role = "Sentient Creation",
                faction = "The Alpine Solitude",
                description = "Born benevolent and eloquent, transformed into vengeance by human rejection.",
                keyQuote = "I was benevolent and good; misery made me a fiend. Make me happy, and I shall again be virtuous.",
                avatarEmoji = "🧟",
                xPercent = 0.50f,
                yPercent = 0.78f,
                significance = 1.3f
            ),
            CharacterNode(
                id = "walton",
                name = "Capt. Robert Walton",
                role = "Arctic Explorer",
                faction = "The Northern Expedition",
                description = "Pursuing glory at the North Pole; serves as listener and moral mirror to Victor.",
                keyQuote = "One man's life or death were but a small price to pay for the acquirement of the knowledge.",
                avatarEmoji = "🚢",
                xPercent = 0.18f,
                yPercent = 0.40f,
                significance = 1.05f
            ),
            CharacterNode(
                id = "elizabeth",
                name = "Elizabeth Lavenza",
                role = "Adoptive Sister & Fiancee",
                faction = "Frankenstein Household",
                description = "Embodies empathy, virtue, and domestic harmony; Victor's anchor to peace.",
                keyQuote = "Her voice was the sweetest music, soothing his fevered ambitions.",
                avatarEmoji = "🕊️",
                xPercent = 0.82f,
                yPercent = 0.45f,
                significance = 1.0f
            )
        )

        val edges = listOf(
            RelationshipEdge("victor", "creature", "Created & Abandoned", RelationType.CREATION),
            RelationshipEdge("creature", "victor", "Vengeance & Yearning", RelationType.RIVAL),
            RelationshipEdge("walton", "victor", "Rescues & Records Confession", RelationType.ALLY),
            RelationshipEdge("victor", "elizabeth", "Cherished Kin & Betrothed", RelationType.KINSHIP)
        )

        return BookMindMap(
            bookId = "book-frankenstein",
            bookTitle = "Frankenstein",
            nodes = nodes,
            edges = edges,
            thematicSummary = "A cautionary masterpiece examining the hubris of unbounded ambition, parental responsibility, and existential alienation."
        )
    }

    private fun getAliceMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "alice",
                name = "Alice",
                role = "Curious Dreamer",
                faction = "The Real World / Dreamer",
                description = "A polite, logical Victorian girl navigating surreal nonsense and shifting scale.",
                keyQuote = "Curiouser and curiouser! Now I'm opening out like the largest telescope that ever was!",
                avatarEmoji = "👧",
                xPercent = 0.50f,
                yPercent = 0.42f,
                significance = 1.35f
            ),
            CharacterNode(
                id = "rabbit",
                name = "The White Rabbit",
                role = "Herald & Guide",
                faction = "Wonderland Court",
                description = "Obsessed with time and protocol; lures Alice into the rabbit hole.",
                keyQuote = "Oh dear! Oh dear! I shall be late!",
                avatarEmoji = "🐇",
                xPercent = 0.20f,
                yPercent = 0.22f,
                significance = 1.1f
            ),
            CharacterNode(
                id = "hatter",
                name = "The Mad Hatter",
                role = "Perpetual Tea Host",
                faction = "The Mad Tea Party",
                description = "Trapped in perpetual 6:00 PM tea time alongside the March Hare.",
                keyQuote = "Why is a raven like a writing-desk?",
                avatarEmoji = "🎩",
                xPercent = 0.20f,
                yPercent = 0.72f,
                significance = 1.1f
            ),
            CharacterNode(
                id = "queen",
                name = "Queen of Hearts",
                role = "Tyrannical Monarch",
                faction = "The Card Royalty",
                description = "Governs Wonderland through arbitrary decrees and temper tantrums.",
                keyQuote = "Off with their heads! Sentence first—verdict afterwards!",
                avatarEmoji = "👑",
                xPercent = 0.82f,
                yPercent = 0.35f,
                significance = 1.2f
            ),
            CharacterNode(
                id = "cheshire",
                name = "Cheshire Cat",
                role = "Philosophical Trickster",
                faction = "The Forest Canopy",
                description = "Grinning feline capable of disappearing at will; provides enigmatic guidance.",
                keyQuote = "We're all mad here. I'm mad. You're mad.",
                avatarEmoji = "🐱",
                xPercent = 0.78f,
                yPercent = 0.75f,
                significance = 1.15f
            )
        )

        val edges = listOf(
            RelationshipEdge("alice", "rabbit", "Chases Down Rabbit Hole", RelationType.INVESTIGATION),
            RelationshipEdge("alice", "hatter", "Attends Surreal Tea Party", RelationType.NEUTRAL),
            RelationshipEdge("queen", "alice", "Demands Croquet & Trial", RelationType.RIVAL),
            RelationshipEdge("cheshire", "alice", "Enigmatic Philosophical Advice", RelationType.MENTOR)
        )

        return BookMindMap(
            bookId = "book-alice-wonderland",
            bookTitle = "Alice's Adventures in Wonderland",
            nodes = nodes,
            edges = edges,
            thematicSummary = "A playful, razor-sharp satire of Victorian social conventions, language, and mathematical logic disguised as a whimsical fantasy."
        )
    }

    private fun getArtOfWarMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "sun_tzu",
                name = "Sun Tzu",
                role = "Supreme Military Strategist",
                faction = "State of Wu",
                description = "Advocates tactical deception, terrain mastery, and victory through foreknowledge.",
                keyQuote = "The supreme art of war is to subdue the enemy without fighting.",
                avatarEmoji = "⚔️",
                xPercent = 0.50f,
                yPercent = 0.30f,
                significance = 1.35f
            ),
            CharacterNode(
                id = "sovereign",
                name = "The Sovereign",
                role = "Ruler of the Realm",
                faction = "The Throne",
                description = "Determines national policy; must trust commanders in the field without interference.",
                keyQuote = "He will win who has military capacity and is not interfered with by the sovereign.",
                avatarEmoji = "👑",
                xPercent = 0.22f,
                yPercent = 0.65f,
                significance = 1.1f
            ),
            CharacterNode(
                id = "general",
                name = "The Commander",
                role = "Field General",
                faction = "Armed Forces",
                description = "Embodies wisdom, sincerity, benevolence, courage, and strictness.",
                keyQuote = "Regard your soldiers as your children, and they will follow you into the deepest valleys.",
                avatarEmoji = "🛡️",
                xPercent = 0.50f,
                yPercent = 0.75f,
                significance = 1.15f
            ),
            CharacterNode(
                id = "spies",
                name = "Secret Agents & Spies",
                role = "Foreknowledge Network",
                faction = "Intelligence Division",
                description = "The indispensable pillar of military operations for acquiring real-time tactical intelligence.",
                keyQuote = "Knowledge of the enemy's dispositions can only be obtained from other men.",
                avatarEmoji = "🦅",
                xPercent = 0.80f,
                yPercent = 0.60f,
                significance = 1.05f
            )
        )

        val edges = listOf(
            RelationshipEdge("sovereign", "general", "Appoints with Total Field Autonomy", RelationType.ALLY),
            RelationshipEdge("sun_tzu", "general", "Provides Guiding Strategic Principles", RelationType.MENTOR),
            RelationshipEdge("spies", "general", "Supplies Foreknowledge & Espionage", RelationType.ALLY)
        )

        return BookMindMap(
            bookId = "book-art-of-war",
            bookTitle = "The Art of War",
            nodes = nodes,
            edges = edges,
            thematicSummary = "A timeless strategic treatise prioritizing psychological advantage, flexibility like water, and effortless victory."
        )
    }

    private fun getMeditationsMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "marcus",
                name = "Marcus Aurelius",
                role = "Philosopher-Emperor",
                faction = "Stoic Citadel",
                description = "Emperor of Rome examining his own soul, mortality, and duty during military campaigns.",
                keyQuote = "You have power over your mind - not outside events. Realise this, and you will find strength.",
                avatarEmoji = "🏛️",
                xPercent = 0.50f,
                yPercent = 0.35f,
                significance = 1.35f
            ),
            CharacterNode(
                id = "logos",
                name = "Universal Nature (Logos)",
                role = "Cosmic Reason & Order",
                faction = "The Universe",
                description = "The rational governing principle of the cosmos in which all beings participate.",
                keyQuote = "All things are woven together and the common bond is sacred.",
                avatarEmoji = "🌌",
                xPercent = 0.50f,
                yPercent = 0.80f,
                significance = 1.25f
            ),
            CharacterNode(
                id = "epictetus",
                name = "Epictetus & Mentors",
                role = "Philosophical Guides",
                faction = "Stoic Teachers",
                description = "Freed slave whose discourses taught Marcus the dichotomy of control.",
                keyQuote = "What upsets people is not things themselves, but their judgments about things.",
                avatarEmoji = "📜",
                xPercent = 0.20f,
                yPercent = 0.55f,
                significance = 1.1f
            ),
            CharacterNode(
                id = "society",
                name = "Humanity & Society",
                role = "Fellow Citizens of the World",
                faction = "The Cosmopolis",
                description = "Those with whom we are designed to cooperate like hands, feet, and rows of teeth.",
                keyQuote = "We were made to work together. To obstruct each other is unnatural.",
                avatarEmoji = "👥",
                xPercent = 0.80f,
                yPercent = 0.55f,
                significance = 1.1f
            )
        )

        val edges = listOf(
            RelationshipEdge("epictetus", "marcus", "Teaches Inner Citadel & Stoic Logic", RelationType.MENTOR),
            RelationshipEdge("marcus", "logos", "Aligns Will with Cosmic Providence", RelationType.ALLY),
            RelationshipEdge("marcus", "society", "Practices Unconditional Beneficence", RelationType.KINSHIP)
        )

        return BookMindMap(
            bookId = "book-meditations",
            bookTitle = "Meditations",
            nodes = nodes,
            edges = edges,
            thematicSummary = "Intimate spiritual exercises cultivating resilience, detachment from external praise, and devotion to the common good."
        )
    }

    private fun generateDynamicMindMap(book: Book): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "protagonist",
                name = "Protagonist",
                role = "Central Figure",
                faction = "Core Narrative",
                description = "The driving character navigating the trials of ${book.title}.",
                keyQuote = "Exploring the core dilemma of ${book.genre.lowercase()}.",
                avatarEmoji = "🧭",
                xPercent = 0.50f,
                yPercent = 0.35f,
                significance = 1.3f
            ),
            CharacterNode(
                id = "mentor",
                name = "Guide & Wisdom",
                role = "Mentor / Thematic Anchor",
                faction = "Allies",
                description = "Provides counsel, historical context, and philosophical grounding.",
                keyQuote = "Guiding principles that illuminate the protagonist's journey.",
                avatarEmoji = "📜",
                xPercent = 0.22f,
                yPercent = 0.65f,
                significance = 1.1f
            ),
            CharacterNode(
                id = "catalyst",
                name = "Catalyst / Dilemma",
                role = "Inciting Factor",
                faction = "External Forces",
                description = "The central obstacle or conflict driving transformation.",
                keyQuote = "The moment where necessity forces decisive action.",
                avatarEmoji = "⚡",
                xPercent = 0.78f,
                yPercent = 0.65f,
                significance = 1.1f
            )
        )

        val edges = listOf(
            RelationshipEdge("mentor", "protagonist", "Mentors & Inspires", RelationType.MENTOR),
            RelationshipEdge("catalyst", "protagonist", "Challenges & Transforms", RelationType.RIVAL)
        )

        return BookMindMap(
            bookId = book.id,
            bookTitle = book.title,
            nodes = nodes,
            edges = edges,
            thematicSummary = "A dynamic character node network illustrating the central narrative and conceptual tensions of ${book.title}."
        )
    }
}
