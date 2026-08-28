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
            "book-metamorphosis" -> getMetamorphosisMap()
            "cat-great-gatsby" -> getGreatGatsbyMap()
            "cat-dracula" -> getDraculaMap()
            "cat-pride-prejudice" -> getPridePrejudiceMap()
            "cat-beyond-good-evil" -> getBeyondGoodEvilMap()
            "cat-republic" -> getRepublicMap()
            "cat-letters-stoic" -> getLettersStoicMap()
            "cat-dorian-gray" -> getDorianGrayMap()
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
            "book-meditations",
            "book-metamorphosis",
            "cat-great-gatsby",
            "cat-dracula",
            "cat-pride-prejudice",
            "cat-beyond-good-evil",
            "cat-republic",
            "cat-letters-stoic",
            "cat-dorian-gray"
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

    private fun getMetamorphosisMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "gregor",
                name = "Gregor Samsa",
                role = "Travelling Salesman & Vermin",
                faction = "Samsa Household",
                description = "Awakens transformed into a monstrous insect; struggles between human dignity and alienation.",
                keyQuote = "What's happened to me? It wasn't a dream.",
                avatarEmoji = "🪲",
                xPercent = 0.50f,
                yPercent = 0.35f,
                significance = 1.35f,
                mentionCount = 175,
                sentimentScore = -0.4f
            ),
            CharacterNode(
                id = "grete",
                name = "Grete Samsa",
                role = "Sister & Caregiver",
                faction = "Samsa Household",
                description = "Initially brings food and cares for Gregor, later demands that the family rid themselves of him.",
                keyQuote = "We must try to get rid of it. It is killing our parents.",
                avatarEmoji = "🎻",
                xPercent = 0.22f,
                yPercent = 0.65f,
                significance = 1.2f,
                mentionCount = 110,
                sentimentScore = 0.1f
            ),
            CharacterNode(
                id = "father",
                name = "Herr Samsa (Father)",
                role = "Authoritarian Patriarch",
                faction = "Samsa Household",
                description = "Hostile toward Gregor; throws apples that inflict Gregor's fatal wound.",
                keyQuote = "He threw apple after apple with astonishing accuracy.",
                avatarEmoji = "👨‍🦳",
                xPercent = 0.78f,
                yPercent = 0.65f,
                significance = 1.15f,
                mentionCount = 85,
                sentimentScore = -0.8f
            ),
            CharacterNode(
                id = "clerk",
                name = "The Chief Clerk",
                role = "Corporate Enforcer",
                faction = "Commercial Enterprise",
                description = "Visits Gregor's home immediately when he misses the morning train, representing economic pressure.",
                keyQuote = "Your productivity has lately been very unsatisfactory.",
                avatarEmoji = "💼",
                xPercent = 0.50f,
                yPercent = 0.85f,
                significance = 0.95f,
                mentionCount = 30,
                sentimentScore = -0.7f
            )
        )

        val edges = listOf(
            RelationshipEdge("grete", "gregor", "Initial Compassion Turns to Rejection", RelationType.KINSHIP, 0.95f, 0.2f, listOf("His sister alone had retained some courage and brought him milk and bread.")),
            RelationshipEdge("father", "gregor", "Antagonistic & Violent Control", RelationType.RIVAL, 1.0f, -0.9f, listOf("His father clenched his fist with a hostile expression.")),
            RelationshipEdge("clerk", "gregor", "Economic Surveillance & Demands", RelationType.RIVAL, 0.8f, -0.6f, listOf("The representative of the firm demanding immediate labor."))
        )

        val plotPoints = listOf(
            PlotNode("meta_1", "Chapter 1", "The Morning Transformation", PlotStage.EXPOSITION, "Gregor wakes to find himself transformed into a giant insect.", listOf("gregor"), ConflictType.PERSON_VS_SELF, 0.3f, "He found himself transformed in his bed into a horrible vermin."),
            PlotNode("meta_2", "Chapter 1", "The Chief Clerk's Visit", PlotStage.INCITING_INCIDENT, "Gregor unlocks the door and reveals his horrifying form to the family and clerk.", listOf("gregor", "clerk", "father"), ConflictType.PERSON_VS_SOCIETY, 0.75f, "The chief clerk fled down the stairs in terror."),
            PlotNode("meta_3", "Chapter 2", "Apples and Exile", PlotStage.RISING_ACTION, "His father attacks him with apples, one of which lodges painfully in his back.", listOf("gregor", "father"), ConflictType.PERSON_VS_PERSON, 0.85f, "An apple lodged firmly in Gregor's back, remaining as an inflamed reminder."),
            PlotNode("meta_4", "Chapter 3", "The Violin and the Lodgers", PlotStage.CLIMAX, "Drawn by Grete's violin playing, Gregor creeps out, causing the lodgers to give notice.", listOf("gregor", "grete"), ConflictType.PERSON_VS_SOCIETY, 0.95f, "Was he an animal, that music could move him so?"),
            PlotNode("meta_5", "Chapter 3", "Gregor's Demise", PlotStage.FALLING_ACTION, "Gregor dies quietly in his room during the night, thinking of his family with love.", listOf("gregor"), ConflictType.PERSON_VS_SELF, 0.4f, "He thought back on his family with deep affection and love."),
            PlotNode("meta_6", "Chapter 3", "The Spring Excursion", PlotStage.RESOLUTION, "The family takes a tram ride into the countryside with renewed hopes for Grete's future.", listOf("grete", "father"), ConflictType.PERSON_VS_FATE, 0.2f, "Their daughter sprang to her feet first and stretched her young body.")
        )

        return BookMindMap(
            bookId = "book-metamorphosis",
            bookTitle = "The Metamorphosis",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
            thematicSummary = "A searing existential exploration of dehumanization, corporate exhaustion, familial guilt, and conditional love."
        )
    }

    private fun getGreatGatsbyMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "gatsby",
                name = "Jay Gatsby",
                role = "Enigmatic Millionaire",
                faction = "West Egg",
                description = "Self-made visionary pursuing his idealized romantic past with Daisy Buchanan.",
                keyQuote = "Can't repeat the past? Why of course you can!",
                avatarEmoji = "🥂",
                xPercent = 0.50f,
                yPercent = 0.30f,
                significance = 1.35f,
                mentionCount = 210,
                sentimentScore = 0.7f
            ),
            CharacterNode(
                id = "nick",
                name = "Nick Carraway",
                role = "Narrator & Moral Anchor",
                faction = "West Egg",
                description = "Bond salesman from Minnesota; inclined to reserve all judgments.",
                keyQuote = "I am one of the few honest people that I have ever known.",
                avatarEmoji = "📝",
                xPercent = 0.22f,
                yPercent = 0.60f,
                significance = 1.25f,
                mentionCount = 190,
                sentimentScore = 0.8f
            ),
            CharacterNode(
                id = "daisy",
                name = "Daisy Buchanan",
                role = "Golden Girl & Illusion",
                faction = "East Egg",
                description = "Nick's cousin and the object of Gatsby's lifelong obsession; voice full of money.",
                keyQuote = "Her voice is full of money. That was the inexhaustible charm that rose and fell in it.",
                avatarEmoji = "✨",
                xPercent = 0.78f,
                yPercent = 0.35f,
                significance = 1.2f,
                mentionCount = 140,
                sentimentScore = 0.4f
            ),
            CharacterNode(
                id = "tom",
                name = "Tom Buchanan",
                role = "Aristocratic Bully",
                faction = "East Egg",
                description = "Brutal, wealthy old-money heir; aggressive and hypocritical defender of social hierarchy.",
                keyQuote = "They were careless people, Tom and Daisy—they smashed up things and creatures.",
                avatarEmoji = "🏇",
                xPercent = 0.78f,
                yPercent = 0.75f,
                significance = 1.15f,
                mentionCount = 95,
                sentimentScore = -0.7f
            )
        )

        val edges = listOf(
            RelationshipEdge("gatsby", "daisy", "Idealized Passion & Obsession", RelationType.ROMANTIC, 1.0f, 0.8f, listOf("He had thrown himself into it with a creative passion.")),
            RelationshipEdge("nick", "gatsby", "Loyal Friendship & Chronicler", RelationType.ALLY, 0.9f, 0.9f, listOf("They're a rotten crowd. You're worth the whole damn bunch put together.")),
            RelationshipEdge("tom", "gatsby", "Class Hostility & Rivalry", RelationType.RIVAL, 0.95f, -0.9f, listOf("An intense clash at the Plaza Hotel over Daisy.")),
            RelationshipEdge("tom", "daisy", "Careless Aristocratic Marriage", RelationType.NEUTRAL, 0.8f, -0.2f, listOf("They retreated back into their money and vast carelessness."))
        )

        val plotPoints = listOf(
            PlotNode("gg_1", "Chapter 1", "The Green Light at the Dock", PlotStage.EXPOSITION, "Nick arrives in West Egg and sees Gatsby reaching into the darkness across the Sound.", listOf("nick", "gatsby"), ConflictType.PERSON_VS_SELF, 0.2f, "A single green light, minute and far away, at the end of a dock."),
            PlotNode("gg_2", "Chapter 5", "Tea and Reunion", PlotStage.INCITING_INCIDENT, "Nick hosts tea, reuniting Gatsby and Daisy after five years of separation.", listOf("nick", "gatsby", "daisy"), ConflictType.PERSON_VS_PERSON, 0.6f, "The colossal significance of that light had now vanished forever."),
            PlotNode("gg_3", "Chapter 7", "The Confrontation at the Plaza", PlotStage.RISING_ACTION, "A sweltering afternoon in Manhattan where Tom confronts Gatsby over his origins.", listOf("gatsby", "tom", "daisy"), ConflictType.PERSON_VS_PERSON, 0.9f, "Your wife doesn't love you. She loves me."),
            PlotNode("gg_4", "Chapter 7", "The Valley of Ashes Tragedy", PlotStage.CLIMAX, "Myrtle Wilson is struck and killed by Daisy driving Gatsby's yellow roadster.", listOf("daisy", "gatsby"), ConflictType.PERSON_VS_SOCIETY, 1.0f, "Her life violently extinguished in the dust."),
            PlotNode("gg_5", "Chapter 8", "The Gunshot in the Pool", PlotStage.FALLING_ACTION, "George Wilson murders Gatsby in his swimming pool before taking his own life.", listOf("gatsby"), ConflictType.PERSON_VS_FATE, 0.8f, "The holocaust was complete."),
            PlotNode("gg_6", "Chapter 9", "Boats Against the Current", PlotStage.RESOLUTION, "Nick reflects on Gatsby's incorruptible dream and departs for the Midwest.", listOf("nick"), ConflictType.PERSON_VS_FATE, 0.1f, "So we beat on, boats against the current, borne back ceaselessly into the past.")
        )

        return BookMindMap(
            bookId = "cat-great-gatsby",
            bookTitle = "The Great Gatsby",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
            thematicSummary = "A poetic critique of the American Dream, reckless consumerism, class stratification, and romantic illusion."
        )
    }

    private fun getDraculaMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "dracula",
                name = "Count Dracula",
                role = "Ancient Vampire Lord",
                faction = "Transylvania / Carfax",
                description = "Centuries-old undead nobleman seeking to conquer Victorian London.",
                keyQuote = "My revenge is just begun! I spread it over centuries, and time is on my side.",
                avatarEmoji = "🧛",
                xPercent = 0.50f,
                yPercent = 0.30f,
                significance = 1.35f,
                mentionCount = 230,
                sentimentScore = -0.9f
            ),
            CharacterNode(
                id = "jonathan",
                name = "Jonathan Harker",
                role = "Solicitor & Prisoner",
                faction = "Crew of Light",
                description = "Young English lawyer trapped inside Castle Dracula; survives to aid the hunt.",
                keyQuote = "Welcome to my house! Enter freely and of your own will!",
                avatarEmoji = "📜",
                xPercent = 0.20f,
                yPercent = 0.65f,
                significance = 1.15f,
                mentionCount = 120,
                sentimentScore = 0.6f
            ),
            CharacterNode(
                id = "mina",
                name = "Mina Harker",
                role = "Intellectual Heroine",
                faction = "Crew of Light",
                description = "Jonathan's wife whose stenographic records, telepathic connection, and bravery defeat Dracula.",
                keyQuote = "She has a man's brain and a woman's heart.",
                avatarEmoji = "🧠",
                xPercent = 0.80f,
                yPercent = 0.65f,
                significance = 1.3f,
                mentionCount = 150,
                sentimentScore = 0.9f
            ),
            CharacterNode(
                id = "van_helsing",
                name = "Prof. Van Helsing",
                role = "Occult Scholar & Physician",
                faction = "Crew of Light",
                description = "Polymath scholar armed with garlic, communion wafers, and scientific acumen.",
                keyQuote = "We learn from failure, not from our success.",
                avatarEmoji = "✝️",
                xPercent = 0.50f,
                yPercent = 0.80f,
                significance = 1.25f,
                mentionCount = 135,
                sentimentScore = 0.8f
            )
        )

        val edges = listOf(
            RelationshipEdge("dracula", "jonathan", "Imprisons & Feeds Off Fear", RelationType.RIVAL, 1.0f, -0.95f, listOf("A chilling host holding Harker hostage in the Carpathians.")),
            RelationshipEdge("van_helsing", "mina", "Guides & Protects with Science", RelationType.MENTOR, 0.95f, 0.9f, listOf("United in forensic pursuit through telepathic hypnosis.")),
            RelationshipEdge("jonathan", "mina", "Devoted Marital Union", RelationType.ROMANTIC, 1.0f, 0.95f, listOf("Steadfast devotion across terror and psychological torment.")),
            RelationshipEdge("van_helsing", "dracula", "Metaphysical Duel", RelationType.RIVAL, 1.0f, -1.0f, listOf("Science and ancient ritual combined to cleanse the undead."))
        )

        val plotPoints = listOf(
            PlotNode("drac_1", "Chapters 1-4", "Castle Dracula", PlotStage.EXPOSITION, "Harker travels to Transylvania and discovers Dracula's supernatural nature.", listOf("jonathan", "dracula"), ConflictType.PERSON_VS_PERSON, 0.4f, "The Count's hand was cold as ice—like a dead man's."),
            PlotNode("drac_2", "Chapter 7", "The Demeter's Wreck at Whitby", PlotStage.INCITING_INCIDENT, "The ghost ship Demeter crashes at Whitby with the captain lashed dead to the helm.", listOf("dracula"), ConflictType.PERSON_VS_NATURE, 0.7f, "An immense dog sprang on deck and leapt ashore."),
            PlotNode("drac_3", "Chapter 16", "The Holy Stake", PlotStage.RISING_ACTION, "Van Helsing and the men euthanize Lucy Westenra in her tomb.", listOf("van_helsing"), ConflictType.PERSON_VS_FATE, 0.85f, "Resting at last in peace."),
            PlotNode("drac_4", "Chapter 21", "The Attack on Mina", PlotStage.CLIMAX, "Dracula forces Mina to drink his blood, establishing a psychic link.", listOf("dracula", "mina", "jonathan"), ConflictType.PERSON_VS_PERSON, 1.0f, "Flesh of my flesh, bone of my bone!"),
            PlotNode("drac_5", "Chapter 27", "The Carpathian Pursuit", PlotStage.FALLING_ACTION, "The hunters intercept Dracula's box of earth as the sun sinks over the Borgo Pass.", listOf("jonathan", "mina", "dracula"), ConflictType.PERSON_VS_PERSON, 0.9f, "Before the sun dipped below the ridge."),
            PlotNode("drac_6", "Chapter 27", "Ashes in the Snow", PlotStage.RESOLUTION, "Harker's kukri knife and Morris's bowie knife turn the Count to dust.", listOf("dracula", "jonathan", "mina"), ConflictType.PERSON_VS_FATE, 0.2f, "The whole body crumbled into dust and passed from our sight.")
        )

        return BookMindMap(
            bookId = "cat-dracula",
            bookTitle = "Dracula",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
            thematicSummary = "The archetypal gothic battle between Victorian technological modernity and primordial eastern folklore."
        )
    }

    private fun getPridePrejudiceMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "elizabeth",
                name = "Elizabeth Bennet",
                role = "Witty Second Daughter",
                faction = "Longbourn Estate",
                description = "Perceptive, quick-witted heroine whose prejudice blinds her to Darcy's genuine character.",
                keyQuote = "I could easily forgive his pride, if he had not mortified mine.",
                avatarEmoji = "📖",
                xPercent = 0.35f,
                yPercent = 0.35f,
                significance = 1.35f,
                mentionCount = 260,
                sentimentScore = 0.8f
            ),
            CharacterNode(
                id = "darcy",
                name = "Fitzwilliam Darcy",
                role = "Master of Pemberley",
                faction = "Pemberley Estate",
                description = "Proud, aristocratic landowner who learns humility through his love for Elizabeth.",
                keyQuote = "You must allow me to tell you how ardently I admire and love you.",
                avatarEmoji = "🎩",
                xPercent = 0.65f,
                yPercent = 0.35f,
                significance = 1.35f,
                mentionCount = 220,
                sentimentScore = 0.7f
            ),
            CharacterNode(
                id = "jane",
                name = "Jane Bennet",
                role = "Eldest Sister",
                faction = "Longbourn Estate",
                description = "Gentle and universally kind; sees only the best in every acquaintance.",
                keyQuote = "She never sees a fault in anybody.",
                avatarEmoji = "🌸",
                xPercent = 0.20f,
                yPercent = 0.75f,
                significance = 1.1f,
                mentionCount = 90,
                sentimentScore = 0.95f
            ),
            CharacterNode(
                id = "wickham",
                name = "George Wickham",
                role = "Charming Militia Officer",
                faction = "Militia Regiment",
                description = "Smooth-talking seducer whose agreeable manners mask financial greed and dishonesty.",
                keyQuote = "One has got all the goodness, and the other all the appearance of it.",
                avatarEmoji = "🎭",
                xPercent = 0.80f,
                yPercent = 0.75f,
                significance = 1.15f,
                mentionCount = 80,
                sentimentScore = -0.7f
            )
        )

        val edges = listOf(
            RelationshipEdge("darcy", "elizabeth", "Mutual Humility & Devoted Marriage", RelationType.ROMANTIC, 1.0f, 0.95f, listOf("Pride humbled, prejudice resolved into mutual respect.")),
            RelationshipEdge("wickham", "darcy", "Slander & Old Resentment", RelationType.RIVAL, 0.9f, -0.9f, listOf("Wickham's attempts to seduce Georgiana Darcy and swindle the family.")),
            RelationshipEdge("wickham", "elizabeth", "Early Deceptive Flirtation", RelationType.NEUTRAL, 0.75f, -0.2f, listOf("Elizabeth initially believing Wickham's false tale."))
        )

        val plotPoints = listOf(
            PlotNode("pp_1", "Chapter 3", "The Meryton Assembly", PlotStage.EXPOSITION, "Darcy refuses to dance with Elizabeth, calling her 'tolerable, but not handsome enough to tempt me.'", listOf("elizabeth", "darcy"), ConflictType.PERSON_VS_PERSON, 0.3f, "She is tolerable; but not handsome enough to tempt me."),
            PlotNode("pp_2", "Chapter 34", "The Disastrous First Proposal", PlotStage.INCITING_INCIDENT, "Darcy proposes at Hunsford Parsonage; Elizabeth vehemently rejects him.", listOf("darcy", "elizabeth"), ConflictType.PERSON_VS_PERSON, 0.8f, "You could not have made me the offer of your hand in any possible way that would have tempted me."),
            PlotNode("pp_3", "Chapter 35", "Darcy's Vindication Letter", PlotStage.RISING_ACTION, "Darcy's letter reveals the true corrupt nature of Wickham and his motives regarding Bingley.", listOf("darcy", "elizabeth"), ConflictType.PERSON_VS_SELF, 0.6f, "Till this moment I never knew myself."),
            PlotNode("pp_4", "Chapter 43", "The Visit to Pemberley", PlotStage.CLIMAX, "Elizabeth encounters the genuine benevolence of Darcy at his ancestral estate.", listOf("elizabeth", "darcy"), ConflictType.PERSON_VS_SELF, 0.75f, "To be mistress of Pemberley might be something!"),
            PlotNode("pp_5", "Chapter 48", "Lydia's Elopement Salvaged", PlotStage.FALLING_ACTION, "Darcy secretly pays Wickham's debts to rescue the Bennet family from disgrace.", listOf("darcy", "wickham"), ConflictType.PERSON_VS_SOCIETY, 0.9f, "He did it all for her."),
            PlotNode("pp_6", "Chapter 58", "The Second Proposal", PlotStage.RESOLUTION, "Elizabeth and Darcy confess their love on a walk through the Hertfordshire lanes.", listOf("elizabeth", "darcy"), ConflictType.PERSON_VS_PERSON, 0.1f, "My affections and wishes are unchanged.")
        )

        return BookMindMap(
            bookId = "cat-pride-prejudice",
            bookTitle = "Pride and Prejudice",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
            thematicSummary = "A sparkling social satire of Regency manners, marriage as an economic transaction, and the necessity of self-knowledge."
        )
    }

    private fun getBeyondGoodEvilMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "free_spirit",
                name = "The Free Spirit",
                role = "Philosopher of the Future",
                faction = "Transvaluation of Values",
                description = "Daring thinker who questions all moral dogma, truth claims, and herd conformism.",
                keyQuote = "It is the business of the very few to be independent; it is a privilege of the strong.",
                avatarEmoji = "🦅",
                xPercent = 0.50f,
                yPercent = 0.30f,
                significance = 1.35f,
                mentionCount = 140,
                sentimentScore = 0.9f
            ),
            CharacterNode(
                id = "will_to_power",
                name = "The Will to Power",
                role = "Foundational Life Force",
                faction = "Cosmic Principle",
                description = "The intrinsic driving energy of all organic life to grow, master, and overcome resistance.",
                keyQuote = "Life itself is will to power; self-preservation is only one of the indirect results.",
                avatarEmoji = "⚡",
                xPercent = 0.50f,
                yPercent = 0.75f,
                significance = 1.3f,
                mentionCount = 120,
                sentimentScore = 0.85f
            ),
            CharacterNode(
                id = "master_morality",
                name = "Master Morality",
                role = "Noble & Life-Affirming Value System",
                faction = "Aristocratic Ethics",
                description = "Values strength, excellence, beauty, and greatness; defines opposites as Good vs. Bad.",
                keyQuote = "The noble type of man regards himself as a determiner of values.",
                avatarEmoji = "👑",
                xPercent = 0.20f,
                yPercent = 0.55f,
                significance = 1.15f,
                mentionCount = 65,
                sentimentScore = 0.7f
            ),
            CharacterNode(
                id = "slave_morality",
                name = "Slave Morality (Ressentiment)",
                role = "Reactive Herd Morality",
                faction = "The Herd",
                description = "Born out of ressentiment against the strong; equates weakness with humility and power with evil.",
                keyQuote = "The slave's eye is not favorable to the virtues of the powerful.",
                avatarEmoji = "⛓️",
                xPercent = 0.80f,
                yPercent = 0.55f,
                significance = 1.15f,
                mentionCount = 70,
                sentimentScore = -0.5f
            )
        )

        val edges = listOf(
            RelationshipEdge("free_spirit", "will_to_power", "Harnesses for Intellectual Overcoming", RelationType.ALLY, 1.0f, 0.95f, listOf("A philosophy that embraces struggle and creative self-mastery.")),
            RelationshipEdge("master_morality", "slave_morality", "Historical Cultural Dialectic", RelationType.RIVAL, 1.0f, -0.9f, listOf("The millennia-long struggle between Roman nobility and Judeo-Christian ressentiment.")),
            RelationshipEdge("free_spirit", "slave_morality", "Critiques Unconscious Dogmatism", RelationType.RIVAL, 0.9f, -0.7f, listOf("Exposing the hidden will to power behind ascetic ideals."))
        )

        val plotPoints = listOf(
            PlotNode("bge_1", "Chapter 1", "On the Prejudices of Philosophers", PlotStage.EXPOSITION, "Nietzsche interrogates why humanity has sought 'truth' rather than untruth and uncertainty.", listOf("free_spirit"), ConflictType.PERSON_VS_SOCIETY, 0.3f, "What really is this 'Will to Truth' in us?"),
            PlotNode("bge_2", "Chapter 2", "The Free Spirit", PlotStage.INCITING_INCIDENT, "Distinguishing genuine trailblazing philosophers from mere academic scholars.", listOf("free_spirit"), ConflictType.PERSON_VS_SELF, 0.6f, "O sancta simplicitas! In what strange simplification man lives!"),
            PlotNode("bge_3", "Chapter 5", "The Natural History of Morals", PlotStage.RISING_ACTION, "Unmasking ethical systems as physiological and psychological symptoms.", listOf("master_morality", "slave_morality"), ConflictType.PERSON_VS_SOCIETY, 0.8f, "Morality in Europe at present is herd-animal morality."),
            PlotNode("bge_4", "Chapter 7", "Our Virtues", PlotStage.CLIMAX, "Elevating intellectual intellectual honesty and hardness over sentimental pity.", listOf("free_spirit", "will_to_power"), ConflictType.PERSON_VS_SELF, 0.9f, "To be severe against oneself; the discipline of great suffering."),
            PlotNode("bge_5", "Chapter 9", "What is Noble?", PlotStage.FALLING_ACTION, "Delineating the characteristics of the elevated human being.", listOf("master_morality"), ConflictType.PERSON_VS_SOCIETY, 0.7f, "Every elevation of the type 'man' has hitherto been the work of an aristocratic society."),
            PlotNode("bge_6", "From High Mountains", "Epode and Song", PlotStage.RESOLUTION, "A lyrical invocation of solitary mountain peaks and the arrival of new philosophical companions.", listOf("free_spirit"), ConflictType.PERSON_VS_FATE, 0.1f, "Noon of life! A second time of youth!")
        )

        return BookMindMap(
            bookId = "cat-beyond-good-evil",
            bookTitle = "Beyond Good and Evil",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
            thematicSummary = "A radical polemic dissecting dogmatic metaphysics, the psychology of morals, and the dawn of free thinkers."
        )
    }

    private fun getRepublicMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "socrates",
                name = "Socrates",
                role = "Philosophical Inquirer",
                faction = "The Dialectic",
                description = "Guides the conversation using the elenchus method to define justice and the ideal Kallipolis.",
                keyQuote = "The unexamined life is not worth living.",
                avatarEmoji = "🏛️",
                xPercent = 0.50f,
                yPercent = 0.35f,
                significance = 1.35f,
                mentionCount = 250,
                sentimentScore = 0.9f
            ),
            CharacterNode(
                id = "glaucon",
                name = "Glaucon",
                role = "Spirited Interlocutor",
                faction = "The Youth of Athens",
                description = "Plato's brother; presents the Ring of Gyges challenge to test if justice is valued for its own sake.",
                keyQuote = "They say that to do injustice is good, but to suffer injustice is evil.",
                avatarEmoji = "🛡️",
                xPercent = 0.20f,
                yPercent = 0.65f,
                significance = 1.2f,
                mentionCount = 130,
                sentimentScore = 0.7f
            ),
            CharacterNode(
                id = "thrasymachus",
                name = "Thrasymachus",
                role = "Sophist & Realist",
                faction = "The Sophists",
                description = "Asserts that justice is merely the advantage of the stronger.",
                keyQuote = "Justice is nothing else than the interest of the stronger.",
                avatarEmoji = "⚡",
                xPercent = 0.80f,
                yPercent = 0.65f,
                significance = 1.15f,
                mentionCount = 60,
                sentimentScore = -0.6f
            ),
            CharacterNode(
                id = "philosopher_king",
                name = "The Philosopher-King",
                role = "Ideal Ruler",
                faction = "Kallipolis Guardians",
                description = "Ruled by reason and knowledge of the Form of the Good; reluctantly governs without personal greed.",
                keyQuote = "Until philosophers are kings, cities will never have rest from their evils.",
                avatarEmoji = "👑",
                xPercent = 0.50f,
                yPercent = 0.80f,
                significance = 1.25f,
                mentionCount = 85,
                sentimentScore = 0.95f
            )
        )

        val edges = listOf(
            RelationshipEdge("socrates", "thrasymachus", "Refutes Might-Makes-Right Sophistry", RelationType.RIVAL, 0.95f, -0.5f, listOf("A fierce dialectical clash over the true ruler's benefit.")),
            RelationshipEdge("socrates", "glaucon", "Constructs the Ideal City in Speech", RelationType.MENTOR, 1.0f, 0.9f, listOf("Examining the three parts of the soul: rational, spirited, appetitive.")),
            RelationshipEdge("socrates", "philosopher_king", "Envisions Enlightened Governance", RelationType.ALLY, 0.9f, 1.0f, listOf("The rule of reason guided by the Form of the Good."))
        )

        val plotPoints = listOf(
            PlotNode("rep_1", "Book I", "The Challenge of Thrasymachus", PlotStage.EXPOSITION, "Thrasymachus roars into the debate claiming justice is solely the advantage of the stronger.", listOf("socrates", "thrasymachus"), ConflictType.PERSON_VS_PERSON, 0.4f, "Justice is nothing else than the interest of the stronger."),
            PlotNode("rep_2", "Book II", "The Ring of Gyges", PlotStage.INCITING_INCIDENT, "Glaucon asks whether any human would remain just if given invisibility and total impunity.", listOf("socrates", "glaucon"), ConflictType.PERSON_VS_SELF, 0.6f, "No man would keep his hands off what was not his own."),
            PlotNode("rep_3", "Book IV", "The Tripartite Soul and City", PlotStage.RISING_ACTION, "Justice is defined as every part of the city and soul performing its own proper function.", listOf("socrates"), ConflictType.PERSON_VS_SELF, 0.75f, "Justice is minding one's own business."),
            PlotNode("rep_4", "Book VII", "The Allegory of the Cave", PlotStage.CLIMAX, "The journey of the prisoner from chained darkness to the blinding sunlight of truth.", listOf("socrates", "glaucon"), ConflictType.PERSON_VS_SOCIETY, 1.0f, "And they see only their own shadows on the wall of the cave."),
            PlotNode("rep_5", "Book VIII", "The Degeneration of Regimes", PlotStage.FALLING_ACTION, "The decay from Aristocracy to Timocracy, Oligarchy, Democracy, and finally Tyranny.", listOf("socrates"), ConflictType.PERSON_VS_SOCIETY, 0.8f, "Extreme freedom leads to nothing other than extreme slavery."),
            PlotNode("rep_6", "Book X", "The Myth of Er", PlotStage.RESOLUTION, "The cosmic vision of reincarnation and the eternal responsibility for choosing one's destiny.", listOf("socrates"), ConflictType.PERSON_VS_FATE, 0.2f, "Let us hold fast to the upward path and pursue justice and wisdom.")
        )

        return BookMindMap(
            bookId = "cat-republic",
            bookTitle = "The Republic",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
            thematicSummary = "The foundational Socratic dialogue examining individual justice, psychological harmony, censorship, and ideal statecraft."
        )
    }

    private fun getLettersStoicMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "seneca",
                name = "Lucius Annaeus Seneca",
                role = "Stoic Statesman & Philosopher",
                faction = "Rome / Campania",
                description = "Imperial advisor offering seasoned pragmatic counsel on mastering time, wealth, and fear.",
                keyQuote = "Hold every hour in your grasp. While we are postponing, life speeds by.",
                avatarEmoji = "📜",
                xPercent = 0.35f,
                yPercent = 0.35f,
                significance = 1.35f,
                mentionCount = 180,
                sentimentScore = 0.95f
            ),
            CharacterNode(
                id = "lucilius",
                name = "Lucilius Junior",
                role = "Procurator of Sicily & Friend",
                faction = "Sicily",
                description = "Seneca's younger friend and philosophical correspondent seeking steadfast tranquility.",
                keyQuote = "Judging by what you write me, I am full of good hope for your progress.",
                avatarEmoji = "✉️",
                xPercent = 0.65f,
                yPercent = 0.35f,
                significance = 1.2f,
                mentionCount = 120,
                sentimentScore = 0.8f
            ),
            CharacterNode(
                id = "tranquility",
                name = "Tranquility (Ataraxia)",
                role = "Stoic Peace of Mind",
                faction = "Inner State",
                description = "Freedom from emotional disturbance achieved through rational judgment and moderation.",
                keyQuote = "A well-ordered mind is a man's ability to remain in one place and linger in his own company.",
                avatarEmoji = "🧘",
                xPercent = 0.50f,
                yPercent = 0.75f,
                significance = 1.25f,
                mentionCount = 90,
                sentimentScore = 1.0f
            )
        )

        val edges = listOf(
            RelationshipEdge("seneca", "lucilius", "Philosophical Mentorship & Friendship", RelationType.MENTOR, 1.0f, 0.95f, listOf("124 letters providing an intimate guide to virtuous living.")),
            RelationshipEdge("seneca", "tranquility", "Cultivates Through Daily Practice", RelationType.ALLY, 0.95f, 0.9f, listOf("Practice poverty, face mortality, and detach from public fortune."))
        )

        val plotPoints = listOf(
            PlotNode("sen_1", "Letter 1", "On Saving Time", PlotStage.EXPOSITION, "Seneca urges Lucilius to reclaim his hours from carelessness and postponement.", listOf("seneca", "lucilius"), ConflictType.PERSON_VS_SELF, 0.2f, "All things are another's, Lucilius; time alone is ours."),
            PlotNode("sen_2", "Letter 2", "On Discursiveness in Reading", PlotStage.INCITING_INCIDENT, "Warning against flitting between hundreds of books without digesting the master thinkers.", listOf("seneca", "lucilius"), ConflictType.PERSON_VS_SELF, 0.4f, "To be everywhere is to be nowhere."),
            PlotNode("sen_3", "Letter 18", "On Festivals and Fasting", PlotStage.RISING_ACTION, "Practicing periods of voluntary poverty to inoculate oneself against fear of ruin.", listOf("seneca"), ConflictType.PERSON_VS_SELF, 0.65f, "Is this the condition that I feared?"),
            PlotNode("sen_4", "Letter 28", "On Travel as a Cure for Discontent", PlotStage.CLIMAX, "Explaining that changing skies does not cure internal turmoil; you take yourself wherever you go.", listOf("seneca", "lucilius"), ConflictType.PERSON_VS_SELF, 0.8f, "You must change the spirit, not the climate."),
            PlotNode("sen_5", "Letter 70", "On the Proper Time to Slip the Cable", PlotStage.FALLING_ACTION, "Facing mortality and retaining sovereign autonomy over one's life and exit.", listOf("seneca"), ConflictType.PERSON_VS_FATE, 0.7f, "Life is not to be bought at any price."),
            PlotNode("sen_6", "Letter 124", "On the True Good", PlotStage.RESOLUTION, "The true good is found only in the rational soul, independent of sensory pleasures.", listOf("seneca", "lucilius"), ConflictType.PERSON_VS_SELF, 0.1f, "Seek the good where reason alone discerns it.")
        )

        return BookMindMap(
            bookId = "cat-letters-stoic",
            bookTitle = "Letters from a Stoic",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
            thematicSummary = "A masterclass in practical Stoicism emphasizing time stewardship, financial detachment, resilience, and serene companionship."
        )
    }

    private fun getDorianGrayMap(): BookMindMap {
        val nodes = listOf(
            CharacterNode(
                id = "dorian",
                name = "Dorian Gray",
                role = "Ageless Hedonist",
                faction = "Mayfair Society",
                description = "Corrupted by Lord Henry's aesthetic hedonism while his hidden portrait absorbs his sins.",
                keyQuote = "If it were I who was to be always young, and the picture that was to grow old!",
                avatarEmoji = "🖼️",
                xPercent = 0.50f,
                yPercent = 0.35f,
                significance = 1.35f,
                mentionCount = 240,
                sentimentScore = -0.6f
            ),
            CharacterNode(
                id = "lord_henry",
                name = "Lord Henry Wotton",
                role = "Cynical Hedonist & Mentor",
                faction = "High Society",
                description = "Brilliant, amoral aristocrat whose aphorisms seduce Dorian into absolute indulgence.",
                keyQuote = "The only way to get rid of a temptation is to yield to it.",
                avatarEmoji = "🚬",
                xPercent = 0.20f,
                yPercent = 0.65f,
                significance = 1.3f,
                mentionCount = 180,
                sentimentScore = 0.2f
            ),
            CharacterNode(
                id = "basil",
                name = "Basil Hallward",
                role = "Devoted Artist & Conscience",
                faction = "The Studio",
                description = "Painter whose idolization of Dorian created the masterpiece; urges Dorian to repent.",
                keyQuote = "I have worshipped you with far more romance of feeling than a man usually gives to a friend.",
                avatarEmoji = "🎨",
                xPercent = 0.80f,
                yPercent = 0.65f,
                significance = 1.2f,
                mentionCount = 110,
                sentimentScore = 0.7f
            ),
            CharacterNode(
                id = "sibyl",
                name = "Sibyl Vane",
                role = "Tragic Actress",
                faction = "The Slum Theatre",
                description = "Pure-hearted young actress whose genuine love destroys her acting ability, prompting Dorian's cruel rejection.",
                keyQuote = "You have killed my love. You used to stir my imagination. Now you are simply shallow.",
                avatarEmoji = "🎭",
                xPercent = 0.50f,
                yPercent = 0.80f,
                significance = 1.1f,
                mentionCount = 75,
                sentimentScore = 0.5f
            )
        )

        val edges = listOf(
            RelationshipEdge("lord_henry", "dorian", "Corrupting Philosophical Influence", RelationType.MENTOR, 1.0f, -0.4f, listOf("Pouring poisoned aesthetic theories into Dorian's impressionable ears.")),
            RelationshipEdge("basil", "dorian", "Artistic Worship & Tragic Conscience", RelationType.ALLY, 0.95f, 0.6f, listOf("Basil's adoration yields the supernatural canvas.")),
            RelationshipEdge("dorian", "sibyl", "Cruel Abandonment Leading to Tragedy", RelationType.ROMANTIC, 0.85f, -0.8f, listOf("The catalyst that causes the portrait's first sneer of cruelty.")),
            RelationshipEdge("dorian", "basil", "Murder in the Locked Schoolroom", RelationType.RIVAL, 1.0f, -1.0f, listOf("Dorian murders Basil after revealing the rotten portrait."))
        )

        val plotPoints = listOf(
            PlotNode("dg_1", "Chapter 1", "The Studio and the Portrait", PlotStage.EXPOSITION, "Basil reveals his portrait of Dorian Gray and warns Lord Henry not to corrupt him.", listOf("basil", "lord_henry"), ConflictType.PERSON_VS_PERSON, 0.2f, "There is only one thing in the world worse than being talked about, and that is not being talked about."),
            PlotNode("dg_2", "Chapter 2", "The Faustian Wish", PlotStage.INCITING_INCIDENT, "Under Lord Henry's influence, Dorian wishes to remain forever young while the painting bears his age.", listOf("dorian", "lord_henry"), ConflictType.PERSON_VS_SELF, 0.6f, "I would give my soul for that!"),
            PlotNode("dg_3", "Chapter 7", "The Cruel Words to Sibyl", PlotStage.RISING_ACTION, "Dorian brutally abandons Sibyl; returning home, he notices the portrait's first sneer.", listOf("dorian", "sibyl"), ConflictType.PERSON_VS_PERSON, 0.75f, "A touch of cruelty in the mouth."),
            PlotNode("dg_4", "Chapter 13", "The Murder of Basil Hallward", PlotStage.CLIMAX, "Dorian shows the rotting canvas to Basil and stabs him in a frenzied rage.", listOf("dorian", "basil"), ConflictType.PERSON_VS_PERSON, 1.0f, "A cry of terror broke from the painter's lips."),
            PlotNode("dg_5", "Chapter 18", "The Fear of Death", PlotStage.FALLING_ACTION, "Paranoia consumes Dorian as he encounters James Vane in the opium dens.", listOf("dorian"), ConflictType.PERSON_VS_SELF, 0.85f, "Conscience had turned every shadow into an avenger."),
            PlotNode("dg_6", "Chapter 20", "The Knife in the Heart", PlotStage.RESOLUTION, "Dorian slashes the painting to destroy his conscience, killing himself in the act.", listOf("dorian"), ConflictType.PERSON_VS_SELF, 0.1f, "Lying on the floor was a dead man, in evening dress, with a knife in his heart. He was withered, wrinkled, and loathsome of visage.")
        )

        return BookMindMap(
            bookId = "cat-dorian-gray",
            bookTitle = "The Picture of Dorian Gray",
            nodes = nodes,
            edges = edges,
            plotPoints = plotPoints,
            thematicSummary = "A cautionary masterpiece on aesthetic decadence, the duplicity of Victorian respectability, and the mortal cost of hedonism."
        )
    }
}
