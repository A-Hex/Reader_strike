package com.example.data

import com.example.R
import com.example.model.Book
import com.example.model.BookChapter
import com.example.model.BookFormat
import com.example.model.Highlight
import com.example.model.HighlightColor
import com.example.model.ReadingStatus
import com.example.model.StreakBadge
import com.example.model.BadgeTier

object SampleBooksData {

    val INITIAL_BOOKS = listOf(
        Book(
            id = "book-metamorphosis",
            title = "The Metamorphosis",
            author = "Franz Kafka",
            description = "One morning, when Gregor Samsa woke from troubled dreams, he found himself transformed in his bed into a horrible vermin. A profound existential masterwork exploring alienation, guilt, and human transformation.",
            format = BookFormat.EPUB,
            status = ReadingStatus.READING,
            coverGradientStart = 0xFF1E1B4B,
            coverGradientEnd = 0xFF4338CA,
            coverDrawableRes = R.drawable.img_cover_metamorphosis,
            totalPages = 72,
            currentPage = 14,
            readingProgress = 0.19f,
            isFavorite = true,
            isDownloaded = true,
            fileSize = "1.1 MB",
            genre = "Philosophy & Classic",
            tags = listOf("Existentialism", "Classic", "Psychological", "Novella"),
            rating = 4.8f,
            totalMinutesSpent = 45,
            lastReadTimestamp = System.currentTimeMillis() - 1000 * 60 * 30
        ),
        Book(
            id = "book-art-of-war",
            title = "The Art of War",
            author = "Sun Tzu",
            description = "The ancient Chinese military treatise dating from the Late Spring and Autumn Period. Thirteen chapters devoted to different aspects of warfare, strategy, tactical positioning, and leadership.",
            format = BookFormat.EPUB,
            status = ReadingStatus.READING,
            coverGradientStart = 0xFF7F1D1D,
            coverGradientEnd = 0xFFDC2626,
            coverDrawableRes = R.drawable.img_cover_art_of_war,
            totalPages = 84,
            currentPage = 32,
            readingProgress = 0.38f,
            isFavorite = true,
            isDownloaded = true,
            fileSize = "950 KB",
            genre = "Strategy & Philosophy",
            tags = listOf("Strategy", "Ancient", "Leadership", "Wisdom"),
            rating = 4.9f,
            totalMinutesSpent = 90,
            lastReadTimestamp = System.currentTimeMillis() - 1000 * 60 * 120
        ),
        Book(
            id = "book-alice-wonderland",
            title = "Alice's Adventures in Wonderland",
            author = "Lewis Carroll",
            description = "A young girl named Alice falls down a rabbit hole into a subterranean fantasy world populated by peculiar, anthropomorphic creatures. A timeless masterwork of literary nonsense.",
            format = BookFormat.EPUB,
            status = ReadingStatus.WANT_TO_READ,
            coverGradientStart = 0xFF064E3B,
            coverGradientEnd = 0xFF059669,
            coverDrawableRes = R.drawable.img_cover_alice,
            totalPages = 120,
            currentPage = 1,
            readingProgress = 0.0f,
            isFavorite = false,
            isDownloaded = true,
            fileSize = "1.8 MB",
            genre = "Fantasy & Adventure",
            tags = listOf("Fantasy", "Classic", "Adventure", "British"),
            rating = 4.7f,
            totalMinutesSpent = 0,
            lastReadTimestamp = System.currentTimeMillis() - 1000 * 3600 * 48
        ),
        Book(
            id = "book-sherlock-holmes",
            title = "A Study in Scarlet",
            author = "Arthur Conan Doyle",
            description = "The historic first appearance of Sherlock Holmes and Dr. John Watson. A mysterious murder in an abandoned Brixton house uncovers a thrilling transatlantic tale of love, betrayal, and revenge.",
            format = BookFormat.PDF,
            status = ReadingStatus.READING,
            coverGradientStart = 0xFF18181B,
            coverGradientEnd = 0xFF52525B,
            coverDrawableRes = R.drawable.img_cover_sherlock,
            totalPages = 148,
            currentPage = 45,
            readingProgress = 0.30f,
            isFavorite = true,
            isDownloaded = true,
            fileSize = "3.4 MB",
            genre = "Mystery & Detective",
            tags = listOf("Detective", "Mystery", "Victorian", "Crime"),
            rating = 4.9f,
            totalMinutesSpent = 70,
            lastReadTimestamp = System.currentTimeMillis() - 1000 * 3600 * 12
        ),
        Book(
            id = "book-meditations",
            title = "Meditations",
            author = "Marcus Aurelius",
            description = "Personal writings by the Roman Emperor Marcus Aurelius recording private notes to himself and ideas on Stoic philosophy, resilience, mortality, ethics, and inner stillness.",
            format = BookFormat.EPUB,
            status = ReadingStatus.FINISHED,
            coverGradientStart = 0xFF78350F,
            coverGradientEnd = 0xFFD97706,
            coverDrawableRes = R.drawable.img_cover_meditations,
            totalPages = 160,
            currentPage = 160,
            readingProgress = 1.0f,
            isFavorite = true,
            isDownloaded = true,
            fileSize = "1.4 MB",
            genre = "Stoicism & Philosophy",
            tags = listOf("Stoicism", "Self-Mastery", "Roman Empire", "Philosophy"),
            rating = 5.0f,
            totalMinutesSpent = 310,
            lastReadTimestamp = System.currentTimeMillis() - 1000 * 3600 * 24 * 3
        ),
        Book(
            id = "book-frankenstein",
            title = "Frankenstein; or, The Modern Prometheus",
            author = "Mary Shelley",
            description = "Victor Frankenstein creates a sapient creature in an unorthodox scientific experiment, only to recoil from the monstrosity he has brought into the world.",
            format = BookFormat.PDF,
            status = ReadingStatus.WANT_TO_READ,
            coverGradientStart = 0xFF312E81,
            coverGradientEnd = 0xFF6366F1,
            coverDrawableRes = R.drawable.img_cover_frankenstein,
            totalPages = 210,
            currentPage = 1,
            readingProgress = 0.0f,
            isFavorite = false,
            isDownloaded = false,
            fileSize = "4.2 MB",
            genre = "Gothic Horror & Sci-Fi",
            tags = listOf("Gothic", "Science Fiction", "Horror", "Classic"),
            rating = 4.6f,
            totalMinutesSpent = 0,
            lastReadTimestamp = System.currentTimeMillis() - 1000 * 3600 * 96
        )
    )

    val DISCOVER_CATALOG = listOf(
        Book(
            id = "cat-great-gatsby",
            title = "The Great Gatsby",
            author = "F. Scott Fitzgerald",
            description = "Set in the Jazz Age on Long Island, near New York City, the novel depicts first-person narrator Nick Carraway's interactions with mysterious millionaire Jay Gatsby and his obsession with Daisy Buchanan.",
            format = BookFormat.EPUB,
            status = ReadingStatus.WANT_TO_READ,
            coverGradientStart = 0xFF047857,
            coverGradientEnd = 0xFF10B981,
            coverDrawableRes = R.drawable.img_cover_gatsby,
            totalPages = 180,
            currentPage = 1,
            readingProgress = 0.0f,
            isFavorite = false,
            isDownloaded = false,
            fileSize = "2.1 MB",
            genre = "American Modernist",
            tags = listOf("Jazz Age", "Drama", "Romance", "Literary Fiction"),
            rating = 4.7f
        ),
        Book(
            id = "cat-dracula",
            title = "Dracula",
            author = "Bram Stoker",
            description = "The foundational vampire novel that defined the genre. An epistolary tale told through journal entries, letters, and telegrams detailing Count Dracula's attempt to move from Transylvania to England.",
            format = BookFormat.EPUB,
            status = ReadingStatus.WANT_TO_READ,
            coverGradientStart = 0xFF450A0A,
            coverGradientEnd = 0xFF991B1B,
            coverDrawableRes = R.drawable.img_cover_dracula,
            totalPages = 340,
            currentPage = 1,
            readingProgress = 0.0f,
            isFavorite = false,
            isDownloaded = false,
            fileSize = "3.8 MB",
            genre = "Gothic Horror",
            tags = listOf("Vampire", "Horror", "Gothic", "Epistolary"),
            rating = 4.8f
        ),
        Book(
            id = "cat-pride-prejudice",
            title = "Pride and Prejudice",
            author = "Jane Austen",
            description = "Follows the turbulent relationship between Elizabeth Bennet, the daughter of a country gentleman, and Fitzwilliam Darcy, a rich aristocratic landowner in 19th-century England.",
            format = BookFormat.EPUB,
            status = ReadingStatus.WANT_TO_READ,
            coverGradientStart = 0xFF831843,
            coverGradientEnd = 0xFFDB2777,
            coverDrawableRes = R.drawable.img_cover_pride_prejudice,
            totalPages = 290,
            currentPage = 1,
            readingProgress = 0.0f,
            isFavorite = false,
            isDownloaded = false,
            fileSize = "2.9 MB",
            genre = "Romance & Satire",
            tags = listOf("Regency", "Romance", "Wit", "Classic"),
            rating = 4.9f
        ),
        Book(
            id = "cat-beyond-good-evil",
            title = "Beyond Good and Evil",
            author = "Friedrich Nietzsche",
            description = "A scathing critique of past philosophers for their blind acceptance of dogmatic premises and Christian moralities, proposing the concept of will to power and the master-slave morality.",
            format = BookFormat.PDF,
            status = ReadingStatus.WANT_TO_READ,
            coverGradientStart = 0xFF1C1917,
            coverGradientEnd = 0xFF78716C,
            coverDrawableRes = R.drawable.img_cover_nietzsche,
            totalPages = 195,
            currentPage = 1,
            readingProgress = 0.0f,
            isFavorite = false,
            isDownloaded = false,
            fileSize = "3.1 MB",
            genre = "Philosophy & Polemic",
            tags = listOf("Philosophy", "Nietzsche", "Ethics", "Critique"),
            rating = 4.7f
        ),
        Book(
            id = "cat-republic",
            title = "The Republic",
            author = "Plato",
            description = "A Socratic dialogue concerning justice, the order and character of the just city-state, and the just human. Features the famous Allegory of the Cave and philosopher-kings.",
            format = BookFormat.EPUB,
            status = ReadingStatus.WANT_TO_READ,
            coverGradientStart = 0xFF0F172A,
            coverGradientEnd = 0xFF334155,
            coverDrawableRes = R.drawable.img_cover_republic,
            totalPages = 380,
            currentPage = 1,
            readingProgress = 0.0f,
            isFavorite = false,
            isDownloaded = false,
            fileSize = "3.6 MB",
            genre = "Classical Philosophy",
            tags = listOf("Philosophy", "Classics", "Justice", "Greek", "Stoicism"),
            rating = 4.9f
        ),
        Book(
            id = "cat-letters-stoic",
            title = "Letters from a Stoic",
            author = "Seneca",
            description = "Epistulae Morales ad Lucilium—a collection of 124 letters Seneca wrote to his friend Lucilius, giving pragmatic guidance on courage, friendship, wealth, mortality, and tranquility.",
            format = BookFormat.EPUB,
            status = ReadingStatus.WANT_TO_READ,
            coverGradientStart = 0xFF365314,
            coverGradientEnd = 0xFF65A30D,
            coverDrawableRes = R.drawable.img_cover_stoic,
            totalPages = 240,
            currentPage = 1,
            readingProgress = 0.0f,
            isFavorite = false,
            isDownloaded = false,
            fileSize = "2.4 MB",
            genre = "Stoicism & Philosophy",
            tags = listOf("Stoicism", "Seneca", "Philosophy", "Ethics", "Wisdom"),
            rating = 4.95f
        ),
        Book(
            id = "cat-dorian-gray",
            title = "The Picture of Dorian Gray",
            author = "Oscar Wilde",
            description = "The philosophical gothic story of a handsome young man whose portrait ages and records his moral corruption while he remains forever youthful and hedonistic.",
            format = BookFormat.EPUB,
            status = ReadingStatus.WANT_TO_READ,
            coverGradientStart = 0xFF4A044E,
            coverGradientEnd = 0xFF86198F,
            coverDrawableRes = R.drawable.img_cover_dorian,
            totalPages = 230,
            currentPage = 1,
            readingProgress = 0.0f,
            isFavorite = false,
            isDownloaded = false,
            fileSize = "2.2 MB",
            genre = "Gothic & Aestheticism",
            tags = listOf("Gothic", "Classics", "Morality", "Art", "Psychological"),
            rating = 4.8f
        )
    )

    val INITIAL_REVIEWS = listOf(
        com.example.model.BookReview(
            id = "rev-1",
            bookId = "book-meditations",
            bookTitle = "Meditations",
            userName = "MarcusReader",
            userAvatarColor = 0xFFB4CCB9,
            rating = 5.0f,
            reviewTitle = "A daily compass for mental resilience",
            reviewText = "Marcus Aurelius wrote this purely for self-examination, yet 2,000 years later it feels like the most grounded therapy. The lessons on not getting angry at difficult people and focusing strictly on what is within control are timeless.",
            timestamp = System.currentTimeMillis() - 86400000L * 3,
            isUserReview = true,
            helpfulCount = 42
        ),
        com.example.model.BookReview(
            id = "rev-2",
            bookId = "book-meditations",
            bookTitle = "Meditations",
            userName = "Elena Vance",
            userAvatarColor = 0xFFD1E8FF,
            rating = 4.8f,
            reviewTitle = "Pure Stoic gold in bite-sized entries",
            reviewText = "Perfect book to read 5-10 minutes every morning as part of my A-Hex reading streak. Highlights make it easy to review favorite passages later.",
            timestamp = System.currentTimeMillis() - 86400000L * 6,
            isUserReview = false,
            helpfulCount = 19
        ),
        com.example.model.BookReview(
            id = "rev-3",
            bookId = "book-art-of-war",
            bookTitle = "The Art of War",
            userName = "DevStrategist",
            userAvatarColor = 0xFFE89A3C,
            rating = 5.0f,
            reviewTitle = "Applicable far beyond military strategy",
            reviewText = "Sun Tzu's emphasis on winning before the conflict begins, avoiding pointless friction, and knowing oneself inside and out provides direct value to everyday leadership and problem solving.",
            timestamp = System.currentTimeMillis() - 86400000L * 4,
            isUserReview = true,
            helpfulCount = 31
        ),
        com.example.model.BookReview(
            id = "rev-4",
            bookId = "book-metamorphosis",
            bookTitle = "The Metamorphosis",
            userName = "Clara K.",
            userAvatarColor = 0xFF9E86C8,
            rating = 4.7f,
            reviewTitle = "Haunting, heartbreaking, and brilliant",
            reviewText = "Kafka captures the burden of expectation and the alienation of modern work like no one else. A fast read that stays with you forever.",
            timestamp = System.currentTimeMillis() - 86400000L * 8,
            isUserReview = false,
            helpfulCount = 27
        ),
        com.example.model.BookReview(
            id = "rev-5",
            bookId = "book-sherlock-holmes",
            bookTitle = "A Study in Scarlet",
            userName = "Arthur_W",
            userAvatarColor = 0xFF5A8E72,
            rating = 4.9f,
            reviewTitle = "The birth of the world's greatest detective",
            reviewText = "Watson meeting Holmes for the first time is legendary. The deduction methods and Victorian atmosphere make this an absolute delight in PDF reader mode.",
            timestamp = System.currentTimeMillis() - 86400000L * 10,
            isUserReview = false,
            helpfulCount = 15
        )
    )


    val INITIAL_HIGHLIGHTS = listOf(
        Highlight(
            id = "hl-1",
            bookId = "book-art-of-war",
            bookTitle = "The Art of War",
            chapterIndex = 0,
            chapterTitle = "I. Laying Plans",
            text = "The supreme art of war is to subdue the enemy without fighting.",
            note = "Core strategic principle applicable to business negotiation and life decisions.",
            color = HighlightColor.AMBER,
            pageOrLocation = 3,
            timestamp = System.currentTimeMillis() - 1000 * 3600 * 5
        ),
        Highlight(
            id = "hl-2",
            bookId = "book-art-of-war",
            bookTitle = "The Art of War",
            chapterIndex = 2,
            chapterTitle = "III. Attack by Stratagem",
            text = "If you know the enemy and know yourself, you need not fear the result of a hundred battles.",
            note = "Self-awareness paired with accurate environmental observation yields clarity.",
            color = HighlightColor.EMERALD,
            pageOrLocation = 18,
            timestamp = System.currentTimeMillis() - 1000 * 3600 * 24
        ),
        Highlight(
            id = "hl-3",
            bookId = "book-metamorphosis",
            bookTitle = "The Metamorphosis",
            chapterIndex = 0,
            chapterTitle = "Chapter 1",
            text = "One morning, when Gregor Samsa woke from troubled dreams, he found himself transformed in his bed into a horrible vermin.",
            note = "One of the most iconic opening lines in world literature.",
            color = HighlightColor.VIOLET,
            pageOrLocation = 1,
            timestamp = System.currentTimeMillis() - 1000 * 3600 * 48
        ),
        Highlight(
            id = "hl-4",
            bookId = "book-meditations",
            bookTitle = "Meditations",
            chapterIndex = 1,
            chapterTitle = "Book II",
            text = "You have power over your mind - not outside events. Realize this, and you will find strength.",
            note = "The fundamental Dichotomy of Control in Stoic philosophy.",
            color = HighlightColor.SKY,
            pageOrLocation = 22,
            timestamp = System.currentTimeMillis() - 1000 * 3600 * 72
        )
    )

    val INITIAL_BADGES = listOf(
        StreakBadge(
            id = "badge-first-step",
            title = "A-Hex Pioneer",
            description = "Complete your first reading session to ignite your streak.",
            iconName = "flag",
            tier = BadgeTier.BRONZE,
            isUnlocked = false,
            unlockedAt = null,
            progress = 0.0f
        ),
        StreakBadge(
            id = "badge-3-day",
            title = "Hex Tri-Flame",
            description = "Maintain an unbroken 3-day reading streak.",
            iconName = "local_fire_department",
            tier = BadgeTier.BRONZE,
            isUnlocked = false,
            unlockedAt = null,
            progress = 0.0f
        ),
        StreakBadge(
            id = "badge-7-day",
            title = "Hex Archon (7-Day)",
            description = "Read for 7 consecutive days without breaking the chain.",
            iconName = "whatshot",
            tier = BadgeTier.SILVER,
            isUnlocked = false,
            unlockedAt = null,
            progress = 0.0f
        ),
        StreakBadge(
            id = "badge-page-master",
            title = "Century Reader",
            description = "Read over 100 total pages in the offline reader.",
            iconName = "auto_stories",
            tier = BadgeTier.SILVER,
            isUnlocked = false,
            unlockedAt = null,
            progress = 0.0f
        ),
        StreakBadge(
            id = "badge-highlighter",
            title = "Illuminated Scholar",
            description = "Collect 10 or more colorful highlights and notes across your library.",
            iconName = "edit_note",
            tier = BadgeTier.GOLD,
            isUnlocked = false,
            unlockedAt = null,
            progress = 0.0f
        ),
        StreakBadge(
            id = "badge-30-day",
            title = "A-Hex Grandmaster (30-Day)",
            description = "Ascend to ultimate literacy discipline with a 30-day streak.",
            iconName = "workspace_premium",
            tier = BadgeTier.DIAMOND,
            isUnlocked = false,
            unlockedAt = null,
            progress = 0.0f
        )
    )

    fun getSampleChaptersForBook(
        bookId: String,
        title: String? = null,
        author: String? = null,
        description: String? = null,
        languageCode: String? = null
    ): List<BookChapter> {
        val idLower = bookId.lowercase()
        val titleLower = (title ?: "").lowercase()
        val isArabic = (languageCode != null && languageCode.startsWith("ar", ignoreCase = true)) ||
                (title != null && title.any { it in '\u0600'..'\u06FF' }) ||
                (author != null && author.any { it in '\u0600'..'\u06FF' }) ||
                idLower.contains("muqaddimah") || idLower.contains("kalila") || idLower.contains("tawq") ||
                idLower.contains("bukhala") || idLower.contains("layla") || idLower.contains("ayyam") ||
                idLower.contains("mutanabbi") || idLower.contains("abqariyat") || idLower.contains("balagha") ||
                idLower.contains("qalam") || idLower.contains("ajniha") || idLower.contains("khalili") ||
                idLower.contains("isharat")

        if (isArabic) {
            return getArabicChapters(bookId, title, author, description)
        }

        return when {
            idLower.contains("metamorphosis") || idLower == "book-metamorphosis" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter 1: The Transformation",
                    content = """One morning, when Gregor Samsa woke from troubled dreams, he found himself transformed in his bed into a horrible vermin. He lay on his armour-like back, and if he lifted his head a little he could see his brown belly, slightly domed and divided by arches into stiff sections. The bedding was hardly able to cover it and seemed ready to slide off any moment. His many legs, pitifully thin compared with the size of the rest of him, waved about helplessly as he looked.

"What's happened to me?" he thought. It wasn't a dream. His room, a proper human room although a little too small, lay peacefully between its four familiar walls. A collection of textile samples lay spread out on the table—Samsa was a travelling salesman—and above it there hung a picture that he had recently cut out of an illustrated magazine and housed in a nice, gilded frame. It showed a lady fitted out with a fur hat and fur boa who sat upright, raising a heavy fur muff that covered the whole of her lower arm towards the viewer.

Gregor then turned to look out the window at the dull weather. Drops of rain could be heard hitting the pane, which made him feel quite sad. "How about if I sleep a little bit longer and forget all this nonsense," he thought, but that was something he was unable to do because he was used to sleeping on his right, and in his present state he couldn't get into that position. However hard he threw himself onto his right, he always rolled back to where he was. He must have tried it a hundred times, shut his eyes so that he wouldn't have to look at the floundering legs, and only stopped when he began to feel a mild, dull pain there that he had never felt before.

"Oh, God," he thought, "what a strenuous career it is that I've chosen! Travelling day in and day out. Doing business like this takes much more effort than doing your own business at home, and on top of that there's the curse of travelling, worries about making train connections, bad and irregular food, contact with different people all the time so that you can never get to know anyone or become friendly with them. It can all go to Hell!""""
                ),
                BookChapter(
                    index = 1,
                    title = "Chapter 2: Life Behind Closed Doors",
                    content = """It was not until it was getting dark that evening that Gregor awoke from his deep and coma-like sleep. He was still very weak, but he quickly found that his legs worked reasonably well on the floor. He crawled across the room towards the door to see what had been left for him. There was a basin of fresh milk in which floated small slices of white bread. He had almost laughed with joy, as his hunger was even greater than it had been in the morning, and he immediately dipped his head into the milk, nearly up over his eyes.

But he soon drew it back again in disappointment; not only did the pain in his tender left side make it difficult to eat the food, he found that he did not like the milk at all, although it had once been his favourite drink and that was certainly why his sister had put it there for him. He turned away from the basin with disgust and crept back to the middle of the room.

Through the crack in the door, Gregor could see that the gas had been lit in the living room. His father usually read out the newspaper in a loud voice to his mother and sister, but now there was not a sound. Perhaps this reading aloud, which his sister had always written to him about in her letters, had recently ceased altogether. Yet the apartment was not empty; he could hear the faint murmur of hushed conversation."""
                ),
                BookChapter(
                    index = 2,
                    title = "Chapter 3: The Final Awakening",
                    content = """Gregor's serious injury, from which he suffered for over a month—since no one ventured to remove the apple, it remained in his flesh as a visible reminder—seemed to have reminded even his father that Gregor was a member of the family, in spite of his present pathetic and repulsive shape, who could not be treated as an enemy; that, on the contrary, it was the commandment of family duty to swallow their disgust and endure him, make sacrifices, and endure.

And though his injury had made Gregor lose some of his mobility, it gave him the compensation of being able to listen to the family evening conversations through the open living room door. They would sit together around the table in silence. His father slept in his armchair, dressed in his uniform with its polished gold buttons; his mother leaned forward over fine needlework, and his sister was studying French in order to qualify for a better position.

Gregor thought back on his family with deep affection and love. His conviction that he had to disappear was if anything even firmer than his sister's."""
                )
            )
            idLower.contains("art-of-war") || idLower == "book-art-of-war" -> listOf(
                BookChapter(
                    index = 0,
                    title = "I. Laying Plans",
                    content = """Sun Tzu said: The art of war is of vital importance to the State. It is a matter of life and death, a road either to safety or to ruin. Hence it is a subject of inquiry which can on no account be neglected.

The art of war, then, is governed by five constant factors, to be taken into account in one's deliberations, when seeking to determine the conditions obtaining in the field. These are:
1. The Moral Law
2. Heaven (Climate and Season)
3. Earth (Terrain and Distances)
4. The Commander (Wisdom, Sincerity, Benevolence, Courage, Strictness)
5. Method and Discipline

The Moral Law causes the people to be in complete accord with their ruler, so that they will follow him regardless of their lives, undismayed by any danger.

All warfare is based on deception. Hence, when able to attack, we must seem unable; when using our forces, we must seem inactive; when we are near, we must make the enemy believe we are far away; when far away, we must make him believe we are near. Hold out baits to entice the enemy. Feign disorder, and crush him."""
                ),
                BookChapter(
                    index = 1,
                    title = "II. Waging War",
                    content = """Sun Tzu said: In the operations of war, where there are in the field a thousand swift chariots, as many heavy chariots, and a hundred thousand mail-clad soldiers, with provisions enough to carry them a thousand li, the expenditure at home and at the front, including entertainment of guests, small items such as glue and paint, and sums spent on chariots and armour, will reach the total of a thousand ounces of silver per day. Such is the cost of raising an army of 100,000 men.

When you engage in actual fighting, if victory is long in coming, then men's weapons will grow dull and their ardour will be damped. If you lay siege to a town, you will exhaust your strength.

Now, when your weapons are dulled, your ardour damped, your strength exhausted and your treasure spent, other chieftains will spring up to take advantage of your extremity. Then no man, however wise, will be able to avert the consequences that must ensue. Thus, though we have heard of stupid haste in war, cleverness has never been seen associated with long delays."""
                ),
                BookChapter(
                    index = 2,
                    title = "III. Attack by Stratagem",
                    content = """Sun Tzu said: In the practical art of war, the best thing of all is to take the enemy's country whole and intact; to shatter and destroy it is not so good. So, too, it is better to recapture an army entire than to destroy it.

Hence to fight and conquer in all your battles is not supreme excellence; supreme excellence consists in breaking the enemy's resistance without fighting.

Thus the highest form of generalship is to balk the enemy's plans; the next best is to prevent the junction of the enemy's forces; the next in order is to attack the enemy's army in the field; and the worst policy of all is to besiege walled cities.

Therefore the skillful leader subdues the enemy's troops without any fighting; he captures their cities without laying siege to them; he overthrows their kingdom without lengthy operations in the field."""
                )
            )
            idLower.contains("meditations") || idLower == "book-meditations" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Book I: Debts and Lessons",
                    content = """From my grandfather Verus: gentleness and the control of my temper.

From the reputation and remembrance of my father: modesty and a manly character.

From my mother: piety and beneficence, and abstinence, not only from evil deeds, but even from evil thoughts; and further, simplicity in my way of living, far removed from the habits of the rich.

From my great-grandfather: not to have frequented public schools, and to have had good teachers at home, and to understand that on such things a man should spend liberally.

From my tutor: not to be a green or blue partisan at the chariot races, nor a supporter of the lightly armed or heavily armed gladiators at the amphitheatre; to endure labour, and to need little, and to work with my own hands, and not to meddle with other people's affairs, and not to be ready to listen to slander."""
                ),
                BookChapter(
                    index = 1,
                    title = "Book II: On the River Gran, Among the Quadi",
                    content = """When you wake up in the morning, tell yourself: The people I deal with today will be meddling, ungrateful, arrogant, dishonest, jealous, and surly. They are like this because they cannot distinguish good from evil. But I have seen the beauty of good, and the ugliness of evil, and have recognized that the wrongdoer has a nature related to my own—not of the same blood or birth, but the same mind, and possessing a share of the divine. And so none of them can hurt me. No one can implicate me in ugliness.

Nor can I feel angry at my kin, or hate him. We were made to work together like hands, like feet, like the rows of the upper and lower teeth. To obstruct each other is unnatural. To feel anger at someone, to turn your back on him: these are obstructions."""
                )
            )
            idLower.contains("sherlock") || idLower == "book-sherlock-holmes" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter 1: Mr. Sherlock Holmes",
                    content = """In the year 1878 I took my degree of Doctor of Medicine of the University of London, and proceeded to Netley to go through the course prescribed for surgeons in the army. Having completed my studies there, I was duly attached to the Fifth Northumberland Fusiliers as Assistant Surgeon. The regiment was stationed in India at the time, and before I could join it, the second Afghan war had broken out.

On landing at Bombay, I learned that my corps had advanced through the passes, and was already deep in the enemy's country. I followed, however, with many other officers who were in the same situation, and succeeded in reaching Candahar in safety, where I found my regiment, and at once entered upon my new duties. The campaign brought honours and promotion to many, but for me it had nothing but misfortune and disaster.

I was removed from my brigade and attached to the Berkshires, with whom I served at the fatal battle of Maiwand. There I was struck on the shoulder by a Jezail bullet, which shattered the bone and grazed the subclavian artery. I should have fallen into the hands of the murderous Ghazis had it not been for the devotion and courage shown by Murray, my orderly, who threw me across a pack-horse, and succeeded in bringing me safely to the British lines."""
                ),
                BookChapter(
                    index = 1,
                    title = "Chapter 2: The Science of Deduction",
                    content = """We met next day as he had arranged, and inspected the rooms at No. 221B, Baker Street, of which he had spoken at our meeting. They consisted of a couple of comfortable bed-rooms and a single large airy sitting-room, cheerfully furnished, and illuminated by two broad windows. So desirable in every way were the apartments, and so moderate did the terms seem when divided between us, that the bargain was concluded upon the spot, and we at once took possession.

Sherlock Holmes was not a man who lived into difficult positions; he was quiet, regular, and had habits of astonishing regularity. It was rare for him to be up after ten at night, and he had invariably breakfasted and gone out before I rose in the morning."""
                )
            )
            idLower.contains("alice") || idLower == "book-alice-wonderland" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter I: Down the Rabbit-Hole",
                    content = """Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do: once or twice she had peeped into the book her sister was reading, but it had no pictures or conversations in it, "and what is the use of a book," thought Alice "without pictures or conversations?"

So she was considering in her own mind (as well as she could, for the hot day made her feel very sleepy and stupid), whether the pleasure of making a daisy-chain would be worth the trouble of getting up and picking the daisies, when suddenly a White Rabbit with pink eyes ran close by her."""
                )
            )
            idLower.contains("frankenstein") || idLower == "book-frankenstein" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter 1: The Spark of Being",
                    content = """It was on a dreary night of November that I beheld the accomplishment of my toils. With an anxiety that almost amounted to agony, I collected the instruments of life around me, that I might infuse a spark of being into the lifeless thing that lay at my feet. It was already one in the morning; the rain pattered dismally against the panes, and my candle was nearly burnt out, when, by the glimmer of the half-extinguished light, I saw the dull yellow eye of the creature open; it breathed hard, and a convulsive motion agitated its limbs."""
                )
            )
            idLower.contains("gatsby") || idLower == "cat-great-gatsby" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter 1: West Egg & The Green Light",
                    content = """In my younger and more vulnerable years my father gave me some advice that I've been turning over in my mind ever since.

"Whenever you feel like criticizing any one," he told me, "just remember that all the people in this world haven't had the advantages that you've had."

He stretched out his arms toward the dark water in a curious way, and, far as I was from him, I could have sworn he was trembling. Involuntarily I glanced seaward—and distinguished nothing except a single green light, minute and far away, that might have been the end of a dock."""
                )
            )
            idLower.contains("dracula") || idLower == "cat-dracula" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter 1: Jonathan Harker's Journal",
                    content = """3 May. Bistritz.—Left Munich at 8:35 P. M., on 1st May, arriving at Vienna early next morning. Buda-Pesth seems a wonderful place, from the glimpse which I got of it from the train and the little I could see of the streets.

Within the courtyard stood a tall old man, clean shaven save for a long white moustache, and clad in black from head to foot. "Welcome to my house! Enter freely and of your own will!""""
                )
            )
            idLower.contains("pride") || idLower == "cat-pride-prejudice" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter 1: Netherfield Park",
                    content = """It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.

"My dear Mr. Bennet," said his lady to him one day, "have you heard that Netherfield Park is let at last?"

Mr. Bennet replied that he had not."""
                )
            )
            idLower.contains("republic") || idLower == "cat-republic" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Book I: Justice and the Nature of Right",
                    content = """I went down yesterday to the Piraeus with Glaucon the son of Ariston, that I might offer up my prayers to the goddess; and also because I wanted to see in what manner they would celebrate the festival.

"Socrates," said Cephalus, "you ought to come oftener to the city and see us; for as my bodily strength fails, my delight in conversation and the pleasures of the mind only increase.""""
                )
            )
            else -> listOf(
                BookChapter(
                    index = 0,
                    title = if (title != null) "Introduction: $title" else "Part I: Historical & Philosophical Foundations",
                    content = """The pursuit of literature and philosophical inquiry represents the enduring effort of human thought to articulate the nature of existence, duty, truth, and society. Across classical antiquities, the Enlightenment, and the modern era, the written word serves as an uncorrupted record of human consciousness.

When reading deeply, the mind is challenged to engage not passively, but in an active dialectic with the author's arguments. Every sustained inquiry demands methodical attention: observing how initial premises are established, testing each proposition against reason and human experience, and noting the underlying tensions between individual freedom and societal duty.

As you progress through this volume, engage directly with the text through careful annotations, highlighting pivotal arguments, and reflecting on how these timeless principles illuminate modern dilemmas."""
                ),
                BookChapter(
                    index = 1,
                    title = "Part II: Analysis & Key Themes",
                    content = """The enduring quality of a masterwork is demonstrated when its central propositions continue to evoke critical scrutiny and fresh interpretation across generations.

In examining this text, attend carefully to the author's use of rhetoric, deductive structure, and thematic symbolism. A thoughtful reader interrogates the narrative: What assumptions remain unstated? How do the actions of the central figures reflect universal human instincts? In what ways does the historical context shape the boundaries of the discussion?

True literacy is the practice of contemplative reflection—transforming discrete observations into enduring wisdom."""
                )
            )
        }
    }

    private fun getArabicChapters(
        bookId: String,
        title: String?,
        author: String?,
        description: String?
    ): List<BookChapter> {
        val idLower = bookId.lowercase()
        return when {
            idLower.contains("muqaddimah") -> listOf(
                BookChapter(
                    index = 0,
                    title = "المقدمة: في فضل علم التاريخ وتحقيق مذاهبه",
                    content = """اعلم أن فن التاريخ فن عزيز المذهب، جم الفوائد، شريف الغاية، إذ هو يوقفنا على أحوال الماضين من الأمم في أخلاقهم، والأنبياء في سيرهم، والملوك في دولهم وسياستهم، حتى تتم فائدة الاقتداء في ذلك لمن يرومه في أحوال الدين والدنيا.

فهو محتاج إلى مآخذ متعددة ومعارف متنوعة، وحسن نظر وتثبت يفضيان بصاحبهما إلى الحق، وينكبان به عن المزلات والمغالط؛ لأن الأخبار إذا اعتمد فيها على مجرد النقل ولم تحكم أصول العادة وقواعد السياسة وطبيعة العمران والأحوال في الاجتماع الإنساني، ولا قيس الغائب منها بالشاهد، والحاضر بالذاهب، فربما لم يؤمن فيها من العثور ومزلة القدم والحيد عن جادة الصدق.

وكثيراً ما وقع للمؤرخين والمفسرين وأئمة النقل من المغالط في الحكايات والوقائع، لاعتمادهم فيها على مجرد النقل غثاً أو سميناً، ولم يعرضوها على أصولها ولا قاسوها بأشباهها، ولا سبروها بمعيار الحكمة والوقوف على طبائع الكائنات، وتحكيم النظر والبصيرة في الأخبار، فضلوا عن الحق وتاهوا في بيداء الوهم والغلط."""
                ),
                BookChapter(
                    index = 1,
                    title = "الفصل الأول: في العمران البشري على الجملة",
                    content = """إن الاجتماع الإنساني ضروري، ويعبر الحكماء عن هذا بقولهم: الإنسان مدني بالطبع، أي لا بد له من الاجتماع الذي هو المدنية في اصطلاحهم، وهو معنى العمران.

وبيان ذلك أن الله سبحانه خلق الإنسان وركبه على صورة لا يصح حياتها وبقاؤها إلا بالغذاء، وهداه إلى التماسه بفطرته وبما ركب فيه من القدرة على تحصيله. إلا أن قدرة الواحد من البشر قاصرة عن تحصيل حاجته من ذلك الغذاء؛ فإن أدنى ما يفرض منه وهو قوت يوم من الحنطة مثلاً، لا يحصل إلا بعلاج كثير من الطحن والعجن والطبخ، وكل واحد من هذه الأعمال الثلاثة يحتاج إلى مواعين وآلات لا تتم إلا بصنائع متعددة من حداد ونجار وخزاف.

فلا بد من اجتماع القدر الكثيرة من أبناء جنسه ليحصل القوت له ولهم، فيحصل بالتعاون قدر الكفاية من الحاجة لأكثر منهم بأضعاف. وكذلك يحتاج كل واحد منهم في الدفاع عن نفسه إلى الاستعانة بأبناء جنسه؛ لأن الله سبحانه لما ركب الطباع في الحيوانات كلها، وقسم القدر بينها، جعل قدرة الحيوانات المفترسة أوفى من قدرة الإنسان بكثير، فعوض الإنسان بالفكر واليد اللذين بهما يصنع السلاح ويدافع عن نفسه في ظل الاجتماع والتعاون."""
                ),
                BookChapter(
                    index = 2,
                    title = "الفصل الثاني: في أن الأمم والقبائل تختلف أحوالهم باختلاف نحلتهم من المعاش",
                    content = """اعلم أن اختلاف الأجيال في أحوالهم إنما هو باختلاف نحلتهم من المعاش؛ فإن اجتماعهم إنما هو للتعاون على تحصيله والابتداء بما هو ضروري منه وبسيط قبل الحاجي والكمالي.

فمنهم من يستعمل الفلاحة من الغراسة والزراعة، ومنهم من ينتحل القيام على الحيوان من الغنم والبقر واليعافير والنحل والدود ونتاجها، وهؤلاء هم أهل البدو والقفر. ثم إذا اتسعت أحوال هؤلاء المقتصرين على الضروري وجادت أحوالهم ونمت أموالهم، ارتقوا إلى الترف والدعة وتشييد القصور واختيار الملابس الفاخرة والصنائع المحكمة، وتلك هي أحوال الحضر والمدن.

فالبدو أصل للمدن والحضر وسابق عليها؛ لأن أول مطالب الإنسان الضروري، ولا ينتهي إلى الكمال والترف إلا بعد استيفاء الضروري."""
                )
            )
            idLower.contains("kalila") -> listOf(
                BookChapter(
                    index = 0,
                    title = "باب الأسد والثور: مثل المتحابين يقطع بينهما الكذوب",
                    content = """قال دبشليم الملك لبيدبا الفيلسوف: اضرب لي مثل رجلين كان بينهما مودة وألفة، فقطع بينهما الخائن الكذوب وحملهما على العداوة والبغضاء.

قال الفيلسوف: إذا ابتلي الصديقان الكريم النفس بدخول المحتال النمام بينهما لم يلبث أن ينقطع ما بينهما من حبل الإخاء والصفاء. ومثل ذلك مثل الأسد والثور اللذين كانا متحالفين متصافيين، حتى دخل بينهما دمنة الثعلب فمزق شملهما.

كان بأرض دستاوند رجل شيخ له بنون، وكانوا ذوي إسراف وتضييع، فعظهم أبوهم ونصحهم بطلب المال وصيانته والإنفاق بالقصد. فخرج أكبرهم في تجارة ومعه ثوران يقال لأحدهما شترة وللآخر بندبة. فوحل شترة في طين ولم يستطع النهوض، فتركه التاجر وأقام عليه أجيراً يرعاه، فلما مل الأجير تركه وانطلق وقال إنه مات. فرعى شترة ونشط حتى صار ذا قوة وبأس، وورد غيضة فيها أسد عظيم ومعه سباع كثيرة، فلم يلبث شترة أن خار خواراً شديداً ارتاع منه الأسد الذي لم ير ثوراً قط قبل ذلك."""
                ),
                BookChapter(
                    index = 1,
                    title = "باب الفحص عن أمر دمنة وحيلته",
                    content = """وكان في جند الأسد ثعلبان يقال لأحدهما كليلة وللآخر دمنة، وكانا ذوي دهاء وفطنة، وكان دمنة أشرسهما نفساً وأعظمهما طمعاً ورغبة في المنزلة الرفيعة.

فقال دمنة لأخيه كليلة: ما ترى في أمر هذا السلطان الذي لزم مكانه وجبن عن الصيد والحركة؟
فقال كليلة: وما أنت وهذا الشأن؟ لسنا من أهل المراتب العالية ولا أرباب المشورة، فالزم حدك ودع ما لا يعنيك، فإن القائل بما لا يعنيه كالقرد الذي تكلف قلع الوتد فوقع في شر أعماله.

فقال دمنة: إن من يطلب الشرف والرفعة لا يرضى بالخمول والضعة، وإن المرء الشريف يسمو بنفسه ولو كان حظه قليلاً، بينما الدنيء يرضى بالدون من العيش كالكلب الذي يرضى بالعظم اليابس."""
                )
            )
            idLower.contains("tawq") -> listOf(
                BookChapter(
                    index = 0,
                    title = "باب في ماهية الحب وأعراضه وعلاماته",
                    content = """الحب - أعزك الله - أوله هزل وآخره جد. دقت معانيه لجلالتها عن أن توصف، فلا تدرك حقيقتها إلا بالمعاناة. وليس بمنكر في الديانة ولا بمحظور في الشريعة، إذ القلوب بيد الله عز وجل.

وقد اختلف الناس في ماهيته وقالوا فيه وأطالوا، والذي أذهب إليه أنه اتصال بين أجزاء النفوس المقسومة في هذه الخليقة في أصل عنصرها الرفيع، لا على ما حكاه محمد بن داود رحمه الله عن بعض أهل الفلسفة من أن الأرواح أكر مقسومة، بل على مناسبة قواها في مقر عالمها العلوي ومجاورتها في هيئة تركيبها.

وللحب علامات يقفوها الفطن ويهتدي إليها الذكي، فأولها إدمان النظر، والعين باب النفس الشارع، وهي المنقبة عن سرائرها والمعبرة عن مكنوناتها. وترى الناظر لا يطرف، ينتقل بنقله، وينزوي بانزوائه، ويميل حيث مال كالحرباء مع الشمس."""
                ),
                BookChapter(
                    index = 1,
                    title = "باب في طاعة المحب وعلامات الوفاء",
                    content = """ومن علامات الحب ومظاهره العجيبة الطاعة العمياء، وانقياد المحب لحبيبه وتغيير طباعه المألوفة وخلقه المعروف، فإن كان جموحاً لان، وإن كان بخيلاً جاد، وإن كان جباناً شجع، وإن كان خرقاً تظرف.

وإني لأعرف من كان أضيق الناس خلقاً وأشدهم شكيمة، فلما ذاق طعم المحبة استحال هيناً ليناً كالسحابة الرقيقة، لا يرد أمراً ولا يكسر خاطراً لمن يهواه. كل ذلك رغبة في الرضا وخوفاً من القطيعة والصدود."""
                )
            )
            idLower.contains("bukhala") -> listOf(
                BookChapter(
                    index = 0,
                    title = "باب رسالة سهل بن هارون في تفضيل البخل والاقتصاد",
                    content = """كتب سهل بن هارون إلى بني عمه حين عاتبوه على مذهبه في ادخار المال والاحتراز من الإتلاف:

أصلحكم الله وأبقاكم، وأرشدكم وتولاكم! إنكم عبتم عليّ الاقتصاد وسميتموه بخلاً، ورأيتم الحزم ضناً، وإنما البخل وضع الشيء في غير موضعه ومنعه عن مستحقه، أما حفظ المال وصيانته عن الهلكة وسد الخلة به فهو قوام العيش وعماد المروءة.

ألم تروا أن الله جعل الأموال قياماً للناس؟ فكيف يعاب من حفظ قوام نفسه ودفع الفقر عن ساحته؟ وإني رأيت من ينفق بغير حساب ينتهي به المطاف إلى ذل السؤال والاستجداء ممن كان يفضلهم بالأمس. الحكمة هي وضع الدرهم في مكانه الصحيح دون إفراط يورث العدم ولا تقتير يورث المذمة."""
                )
            )
            idLower.contains("mutanabbi") -> listOf(
                BookChapter(
                    index = 0,
                    title = "روائع المتنبي في الفخر والحكمة وسيف الدولة",
                    content = """عَلى قَدْرِ أهْلِ العَزْم تأتي العَزائِمُ ... وَتأتِي علَى قَدْرِ الكِرامِ المَكارمُ
وَتَعْظُمُ في عَينِ الصّغيرِ صغارُها ... وَتَصْغُرُ في عَينِ العَظيمِ العَظائِمُ

يُكَلّفُ سيفُ الدّوْلَةِ الجيشَ هَمّهُ ... وَقد عَجَزَتْ عنهُ الجُيوشُ الخَضارِمُ
وَهَلْ يَبْلُغُ الإنْسانُ دُونَ نِهايَةٍ ... مَحَلّاً وفيهِ للنّفُوسِ مَزاحِمُ؟

إذا غامَرْتَ في شَرَفٍ مَرُومِ ... فَلا تَقْنَعْ بما دُونَ النّجُومِ
فطَعْمُ المَوْتِ في أمْرٍ حَقِيرٍ ... كطَعْمِ المَوْتِ في أمْرٍ عَظِيمِ
يَرَى الجُبَناءُ أنّ العَجْزَ عَقْلٌ ... وتِلْكَ خَدِيعَةُ الطّبْعِ اللّئِيمِ"""
                ),
                BookChapter(
                    index = 1,
                    title = "باب الحكمة الإنسانية وعزة النفس",
                    content = """ذو العَقْلِ يَشْقَى في النّعيمِ بعَقْلِهِ ... وَأخو الجَهالَةِ في الشّقاوَةِ يَنعَمُ
وَلا تَحْسَبَنّ المَجْدَ زِقّاً وقَيْنَةً ... فما المَجْدُ إلاّ السّيفُ والفَتْكَةُ البِكْرُ

الخَيْلُ وَاللّيْلُ وَالبَيْداءُ تَعرِفُني ... وَالسّيفُ وَالرّمحُ والقِرْطاسُ وَالقَلَمُ
صَحِبْتُ في الفَلَواتِ الوَحشَ مُنفرِداً ... حتى تَعَجّبَ منّي القُورُ وَالأكَمُ"""
                )
            )
            else -> listOf(
                BookChapter(
                    index = 0,
                    title = if (title != null) "مدخل: $title" else "القسم الأول: أصول وفلسفة الكتاب",
                    content = """الحمد لله الذي جعل القراءة مفتاح المعرفة ونور البصيرة. إن هذا السفر الجليل يمثل ركناً هاماً من أركان الأدب والفكر، حيث يبسط المؤلف فيه رؤيته المعمقة ويقدم خلاصة تجاربه وتأملاته في شؤون الحياة والمجتمع.

يتناول هذا الباب تمهيداً شاملاً للقضايا المركزية التي يرتكز عليها الكتاب، مع بيان المنهج المتبع في الاستدلال والتحليل، واستعراض السياق التاريخي والأدبي الذي أفرز هذه الرؤية الفكرية الفريدة.

إن القارئ المتأمل يجد في تضاعيف هذه السطور دعوة صريحة لإعمال العقل والتفكر في المبادئ الخالدة التي تحكم السلوك الإنساني والعمران البشري."""
                ),
                BookChapter(
                    index = 1,
                    title = "القسم الثاني: التحليل المعمق والمقاصد الكلية",
                    content = """تتجلى أصالة هذا العمل في دقته الاستقرائية وقدرته على الجمع بين بلاغة البيان وعمق الفكرة. يستعرض المؤلف هنا التطبيقات العملية والنماذج الواقعية التي تشهد لصحة مقاصده وتبرهن على شمول نظرته.

إن استيعاب هذا المتن يتطلب قراءة فاحصة تقف عند كل دلالة، وتستنبط من الإشارات والنوادر ما يعين على فهم طبائع الأشياء وتحقيق الغايات المنشودة من مدارسة العلم والأدب."""
                )
            )
        }
    }
}
