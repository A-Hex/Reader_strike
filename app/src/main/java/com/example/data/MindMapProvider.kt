package com.example.data

import com.example.model.*
import com.example.util.LocalAiRelationDetector

object MindMapProvider {

    private val customMindMaps = mutableMapOf<String, BookMindMap>()

    fun getMindMapForBook(
        book: Book,
        fullText: String = "",
        forceAiDetection: Boolean = false
    ): BookMindMap {
        if (!forceAiDetection && customMindMaps.containsKey(book.id)) {
            return customMindMaps[book.id]!!
        }

        if (forceAiDetection || (fullText.isNotBlank() && !isCanonicalBook(book.id))) {
            val (detectedMap, _) = LocalAiRelationDetector.analyzeBook(book, fullText)
            customMindMaps[book.id] = detectedMap
            return detectedMap
        }

        val map = when (book.id) {
            "book-sherlock-holmes" -> getSherlockHolmesMap()
            "book-frankenstein" -> getFrankensteinMap()
            "book-alice-wonderland" -> getAliceMap()
            "book-art-of-war" -> getArtOfWarMap()
            "book-meditations" -> getMeditationsMap()
            else -> {
                val (dynMap, _) = LocalAiRelationDetector.analyzeBook(book, fullText)
                dynMap
            }
        }
        customMindMaps[book.id] = map
        return map
    }

    fun isCanonicalBook(bookId: String): Boolean {
        return bookId in setOf(
            "book-sherlock-holmes",
            "book-frankenstein",
            "book-alice-wonderland",
            "book-art-of-war",
            "book-meditations"
        )
    }

    fun saveCustomMindMap(map: BookMindMap) {
        customMindMaps[map.bookId] = map
    }

    fun addCustomNode(bookId: String, node: CharacterNode): BookMindMap {
        val current = customMindMaps[bookId] ?: getSherlockHolmesMap().copy(bookId = bookId)
        val updated = current.copy(nodes = current.nodes + node)
        customMindMaps[bookId] = updated
        return updated
    }

    fun addCustomEdge(bookId: String, edge: RelationshipEdge): BookMindMap {
        val current = customMindMaps[bookId] ?: getSherlockHolmesMap().copy(bookId = bookId)
        val updated = current.copy(edges = current.edges + edge)
        customMindMaps[bookId] = updated
        return updated
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
                significance = 1.3f,
                mentionCount = 142,
                sentimentScore = 0.6f
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
                significance = 1.15f,
                mentionCount = 98,
                sentimentScore = 0.8f
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
                significance = 1.0f,
                mentionCount = 35,
                sentimentScore = 0.3f
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
                significance = 0.95f,
                mentionCount = 28,
                sentimentScore = -0.4f
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
                significance = 1.2f,
                mentionCount = 45,
                sentimentScore = -0.9f
            )
        )

        val edges = listOf(
            RelationshipEdge("watson", "holmes", "Trusted Partner & Biographer", RelationType.ALLY, 1.0f, 0.9f, listOf("A trusted comrade whose quiet fidelity never faltered.")),
            RelationshipEdge("gregson", "holmes", "Consults for Insight", RelationType.INVESTIGATION, 0.8f, 0.3f, listOf("Scotland Yard officer consulting the private detective.")),
            RelationshipEdge("holmes", "drebber", "Investigates Murder", RelationType.INVESTIGATION, 0.9f, -0.2f, listOf("Methodical examination of the crime scene at Lauriston Gardens.")),
            RelationshipEdge("holmes", "moriarty", "Intellectual Rivalry", RelationType.RIVAL, 1.0f, -0.95f, listOf("A duel of sheer ratiocination across London.")),
            RelationshipEdge("gregson", "drebber", "Official Crime Case", RelationType.INVESTIGATION, 0.7f, 0.0f, listOf("Official police inquiry into the foreign traveller."))
        )

        val plotPoints = listOf(
            PlotNode("sh_1", "Chapter 1", "Mr. Sherlock Holmes", PlotStage.EXPOSITION, "Watson meets Holmes through young Stamford at Bart's Hospital.", listOf("holmes", "watson"), ConflictType.PERSON_VS_SOCIETY, 0.2f, "You have been in Afghanistan, I perceive."),
            PlotNode("sh_2", "Chapter 3", "The Lauriston Gardens Mystery", PlotStage.INCITING_INCIDENT, "Inspector Gregson summons Holmes to an abandoned house where Drebber lies murdered.", listOf("holmes", "gregson", "drebber"), ConflictType.PERSON_VS_PERSON, 0.65f, "There is no clue to the killer save the word RACHE scrawled in blood."),
            PlotNode("sh_3", "Chapter 5", "Our Advertisement Brings a Visitor", PlotStage.RISING_ACTION, "Holmes tests theories using a bogus ring and encounters underworld deception.", listOf("holmes", "watson"), ConflictType.PERSON_VS_PERSON, 0.75f, "The game is afoot."),
            PlotNode("sh_4", "Chapter 7", "Light in the Darkness", PlotStage.CLIMAX, "Holmes dramatically handcuffs the cabman Jefferson Hope in 221B Baker Street.", listOf("holmes", "watson"), ConflictType.PERSON_VS_PERSON, 0.95f, "Gentlemen, let me introduce you to Mr. Jefferson Hope, the murderer!"),
            PlotNode("sh_5", "Part II", "The Country of the Saints", PlotStage.FALLING_ACTION, "The historical flashback reveals the Utah backstory and tragic motive.", listOf("drebber"), ConflictType.PERSON_VS_SOCIETY, 0.45f, "A long journey across the alkali plains."),
            PlotNode("sh_6", "Conclusion", "The End of Watson's Memoir", PlotStage.RESOLUTION, "Holmes explains every deductive thread over a pipe at Baker Street.", listOf("holmes", "watson"), ConflictType.PERSON_VS_SELF, 0.15f, "What you do in this world is a matter of no consequence.")
        )

        return BookMindMap(
            bookId = "book-sherlock-holmes",
            bookTitle = "A Study in Scarlet",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
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
                significance = 1.3f,
                mentionCount = 180,
                sentimentScore = 0.1f
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
                significance = 1.3f,
                mentionCount = 160,
                sentimentScore = -0.5f
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
                significance = 1.05f,
                mentionCount = 50,
                sentimentScore = 0.6f
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
                significance = 1.0f,
                mentionCount = 65,
                sentimentScore = 0.9f
            )
        )

        val edges = listOf(
            RelationshipEdge("victor", "creature", "Created & Abandoned", RelationType.CREATION, 1.0f, -0.8f, listOf("I had gazed on him while unfinished; he was ugly then, but when these muscles were capable of motion, it became a thing such as even Dante could not have conceived.")),
            RelationshipEdge("creature", "victor", "Vengeance & Yearning", RelationType.RIVAL, 1.0f, -0.9f, listOf("You are my creator, but I am your master; obey!")),
            RelationshipEdge("walton", "victor", "Rescues & Records Confession", RelationType.ALLY, 0.85f, 0.7f, listOf("I would sacrifice my fortune, my existence, my every hope, to the furtherance of my enterprise.")),
            RelationshipEdge("victor", "elizabeth", "Cherished Kin & Betrothed", RelationType.KINSHIP, 0.9f, 0.95f, listOf("The saintly soul of Elizabeth shone like a shrine-dedicated lamp."))
        )

        val plotPoints = listOf(
            PlotNode("fk_1", "Letters 1-4", "Walton's Arctic Letters", PlotStage.EXPOSITION, "Captain Walton discovers a half-frozen Victor Frankenstein pursuing a monstrous entity across ice floes.", listOf("walton", "victor"), ConflictType.PERSON_VS_NATURE, 0.3f, "We perceived a low carriage, fixed on a sledge and drawn by dogs."),
            PlotNode("fk_2", "Chapter 4", "The Spark of Animation", PlotStage.INCITING_INCIDENT, "Victor discovers the secret of imparting life to inanimate clay in his Ingolstadt laboratory.", listOf("victor", "creature"), ConflictType.PERSON_VS_SELF, 0.7f, "With an anxiety that almost amounted to agony, I collected the instruments of life."),
            PlotNode("fk_3", "Chapter 10", "Confrontation on the Mer de Glace", PlotStage.RISING_ACTION, "Victor meets the Creature amidst Alpine glaciers and hears his tragic tale of rejection.", listOf("victor", "creature"), ConflictType.PERSON_VS_PERSON, 0.85f, "Remember that I am thy creature; I ought to be thy Adam, but I am rather the fallen angel."),
            PlotNode("fk_4", "Chapter 23", "The Wedding Night Tragedy", PlotStage.CLIMAX, "The Creature murders Elizabeth, fulfilling his vow: 'I shall be with you on your wedding-night.'", listOf("victor", "creature", "elizabeth"), ConflictType.PERSON_VS_PERSON, 1.0f, "A grin was on the face of the monster; he seemed to jeer as with his fiendish finger he pointed towards the corpse."),
            PlotNode("fk_5", "Chapter 24", "The Arctic Chase", PlotStage.FALLING_ACTION, "Victor pursues the Creature to the ends of the frozen Earth.", listOf("victor", "creature", "walton"), ConflictType.PERSON_VS_NATURE, 0.6f, "Farewell, Walton! Seek happiness in tranquillity and avoid ambition."),
            PlotNode("fk_6", "Conclusion", "Walton's Epilogue", PlotStage.RESOLUTION, "The Creature mourns over Victor's corpse and vanishes into the darkness and distance.", listOf("creature", "walton"), ConflictType.PERSON_VS_FATE, 0.2f, "He was soon borne away by the waves and lost in darkness and distance.")
        )

        return BookMindMap(
            bookId = "book-frankenstein",
            bookTitle = "Frankenstein",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
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
                significance = 1.35f,
                mentionCount = 240,
                sentimentScore = 0.7f
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
                significance = 1.1f,
                mentionCount = 42,
                sentimentScore = 0.2f
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
                significance = 1.1f,
                mentionCount = 38,
                sentimentScore = 0.0f
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
                significance = 1.2f,
                mentionCount = 55,
                sentimentScore = -0.7f
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
                significance = 1.15f,
                mentionCount = 32,
                sentimentScore = 0.5f
            )
        )

        val edges = listOf(
            RelationshipEdge("alice", "rabbit", "Chases Down Rabbit Hole", RelationType.INVESTIGATION, 0.9f, 0.4f, listOf("Alice started to her feet, for it flashed across her mind that she had never before seen a rabbit with either a waistcoat-pocket or a watch.")),
            RelationshipEdge("alice", "hatter", "Attends Surreal Tea Party", RelationType.NEUTRAL, 0.7f, 0.1f, listOf("It's always six o'clock now.")),
            RelationshipEdge("queen", "alice", "Demands Croquet & Trial", RelationType.RIVAL, 0.95f, -0.8f, listOf("The Queen turned crimson with fury, and glared at her for a moment like a wild beast.")),
            RelationshipEdge("cheshire", "alice", "Enigmatic Philosophical Advice", RelationType.MENTOR, 0.85f, 0.6f, listOf("If you only walk long enough, you're sure to get somewhere."))
        )

        val plotPoints = listOf(
            PlotNode("aw_1", "Chapter 1", "Down the Rabbit Hole", PlotStage.EXPOSITION, "Alice falls down the long rabbit hole into the hall of locked doors.", listOf("alice", "rabbit"), ConflictType.PERSON_VS_NATURE, 0.25f, "Down, down, down. Would the fall never come to an end?"),
            PlotNode("aw_2", "Chapter 2", "The Pool of Tears", PlotStage.INCITING_INCIDENT, "Alice changes size drastically, crying a vast pool of tears that traps Wonderland creatures.", listOf("alice"), ConflictType.PERSON_VS_SELF, 0.55f, "Curiouser and curiouser!"),
            PlotNode("aw_3", "Chapter 7", "A Mad Tea-Party", PlotStage.RISING_ACTION, "Alice navigates riddles without answers with the Hatter, March Hare, and Dormouse.", listOf("alice", "hatter"), ConflictType.PERSON_VS_SOCIETY, 0.7f, "No room! No room! they cried out when they saw Alice coming."),
            PlotNode("aw_4", "Chapter 11", "Who Stole the Tarts?", PlotStage.CLIMAX, "The court trial collapses as Alice grows to full size and defies the Queen's arbitrary rulings.", listOf("alice", "queen"), ConflictType.PERSON_VS_SOCIETY, 0.95f, "You're nothing but a pack of cards!"),
            PlotNode("aw_5", "Chapter 12", "Alice's Evidence", PlotStage.FALLING_ACTION, "The playing cards fly up into the air around Alice as the dream dissolves.", listOf("alice"), ConflictType.PERSON_VS_SELF, 0.4f, "She found herself lying on the bank, with her head in the lap of her sister."),
            PlotNode("aw_6", "Conclusion", "A Sister's Reverie", PlotStage.RESOLUTION, "Alice's sister dreams of little Alice growing into a loving storyteller.", listOf("alice"), ConflictType.PERSON_VS_FATE, 0.1f, "How she would gather about her other little children, and make their eyes bright with many a strange tale.")
        )

        return BookMindMap(
            bookId = "book-alice-wonderland",
            bookTitle = "Alice's Adventures in Wonderland",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
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
                significance = 1.35f,
                mentionCount = 110,
                sentimentScore = 0.8f
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
                significance = 1.1f,
                mentionCount = 45,
                sentimentScore = 0.4f
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
                significance = 1.15f,
                mentionCount = 70,
                sentimentScore = 0.7f
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
                significance = 1.05f,
                mentionCount = 30,
                sentimentScore = 0.5f
            )
        )

        val edges = listOf(
            RelationshipEdge("sovereign", "general", "Appoints with Total Field Autonomy", RelationType.ALLY, 0.9f, 0.6f, listOf("There are commands of the sovereign which must not be obeyed.")),
            RelationshipEdge("sun_tzu", "general", "Provides Guiding Strategic Principles", RelationType.MENTOR, 1.0f, 0.9f, listOf("Let your rapidity be that of the wind, your compactness that of the forest.")),
            RelationshipEdge("spies", "general", "Supplies Foreknowledge & Espionage", RelationType.ALLY, 0.85f, 0.7f, listOf("Spies are a most important element in water, because on them depends an army's ability to move."))
        )

        val plotPoints = listOf(
            PlotNode("aow_1", "Chapter 1", "Laying Plans", PlotStage.EXPOSITION, "The five fundamental factors of state survival: Moral Law, Heaven, Earth, Commander, and Method.", listOf("sun_tzu", "sovereign"), ConflictType.PERSON_VS_SOCIETY, 0.2f, "War is a matter of vital importance to the State; a matter of life and death."),
            PlotNode("aow_2", "Chapter 3", "Attack by Stratagem", PlotStage.INCITING_INCIDENT, "The principle of conquering whole states intact rather than destroying them.", listOf("sun_tzu", "general"), ConflictType.PERSON_VS_PERSON, 0.5f, "To fight and conquer in all our battles is not supreme excellence."),
            PlotNode("aow_3", "Chapter 6", "Weak Points and Strong", PlotStage.RISING_ACTION, "Adapting tactics like flowing water; illusion versus reality.", listOf("general"), ConflictType.PERSON_VS_PERSON, 0.75f, "Water shapes its course according to the nature of the ground over which it flows."),
            PlotNode("aow_4", "Chapter 9", "The Army on the March", PlotStage.CLIMAX, "Engaging the adversary across rivers, mountains, and marshes with decisive positioning.", listOf("general"), ConflictType.PERSON_VS_NATURE, 0.9f, "Camp in high places, facing the sun."),
            PlotNode("aow_5", "Chapter 11", "The Nine Situations", PlotStage.FALLING_ACTION, "Navigating desperate ground where troops fight for survival with unified resolve.", listOf("general"), ConflictType.PERSON_VS_FATE, 0.7f, "Throw your soldiers into positions whence there is no escape, and they will prefer death to flight."),
            PlotNode("aow_6", "Chapter 13", "The Use of Spies", PlotStage.RESOLUTION, "The culmination of strategic mastery through comprehensive foreknowledge.", listOf("spies", "sun_tzu"), ConflictType.PERSON_VS_SOCIETY, 0.3f, "Hence it is only the enlightened ruler and the wise general who will use the highest intelligence for purposes of spying.")
        )

        return BookMindMap(
            bookId = "book-art-of-war",
            bookTitle = "The Art of War",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
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
                significance = 1.35f,
                mentionCount = 190,
                sentimentScore = 0.9f
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
                significance = 1.25f,
                mentionCount = 85,
                sentimentScore = 1.0f
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
                significance = 1.1f,
                mentionCount = 35,
                sentimentScore = 0.85f
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
                significance = 1.1f,
                mentionCount = 60,
                sentimentScore = 0.6f
            )
        )

        val edges = listOf(
            RelationshipEdge("epictetus", "marcus", "Teaches Inner Citadel & Stoic Logic", RelationType.MENTOR, 0.95f, 0.9f, listOf("From Rusticus I conceived the need for moral correction and discipline.")),
            RelationshipEdge("marcus", "logos", "Aligns Will with Cosmic Providence", RelationType.ALLY, 1.0f, 1.0f, listOf("Accept whatever comes to you woven in the pattern of your destiny.")),
            RelationshipEdge("marcus", "society", "Practices Unconditional Beneficence", RelationType.KINSHIP, 0.85f, 0.7f, listOf("When you wake up in the morning, tell yourself: The people I deal with today will be meddling, ungrateful, arrogant, dishonest, jealous, and surly. They are like this because they cannot distinguish good from evil."))
        )

        val plotPoints = listOf(
            PlotNode("med_1", "Book 1", "Debts and Lessons", PlotStage.EXPOSITION, "Marcus enumerates the virtues learned from his grandfather, mother, teachers, and Antoninus.", listOf("marcus", "epictetus"), ConflictType.PERSON_VS_SELF, 0.15f, "From my mother: piety, generosity, and abstaining not only from evil deeds, but from evil thoughts."),
            PlotNode("med_2", "Book 2", "On the River Gran", PlotStage.INCITING_INCIDENT, "Written on military campaign along the frozen Danube; facing the brevity of human existence.", listOf("marcus"), ConflictType.PERSON_VS_FATE, 0.5f, "Time is a river, a fierce torrent of things that come into being."),
            PlotNode("med_3", "Book 4", "The Inner Citadel", PlotStage.RISING_ACTION, "Constructing an inviolable sanctuary within the rational mind.", listOf("marcus", "logos"), ConflictType.PERSON_VS_SELF, 0.65f, "Nowhere can man find a quieter or more untroubled retreat than in his own soul."),
            PlotNode("med_4", "Book 7", "The Cosmos and Mortality", PlotStage.CLIMAX, "Confronting physical pain, betrayal, and mortality with equanimity.", listOf("marcus", "society"), ConflictType.PERSON_VS_SELF, 0.85f, "Look back over the past, with its changing empires that rose and fell, and you can foresee the future too."),
            PlotNode("med_5", "Book 10", "Cosmic Necessity", PlotStage.FALLING_ACTION, "Living simply without complaint as a citizen of the universe.", listOf("marcus", "logos"), ConflictType.PERSON_VS_FATE, 0.4f, "Will you never, my soul, be good and simple and all one?"),
            PlotNode("med_6", "Book 12", "Departing with Grace", PlotStage.RESOLUTION, "Closing reflections on bidding farewell to life with serenity.", listOf("marcus"), ConflictType.PERSON_VS_FATE, 0.1f, "Pass on then with a good grace, for he who bids you go is gracious.")
        )

        return BookMindMap(
            bookId = "book-meditations",
            bookTitle = "Meditations",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
            thematicSummary = "Intimate spiritual exercises cultivating resilience, detachment from external praise, and devotion to the common good."
        )
    }
}
