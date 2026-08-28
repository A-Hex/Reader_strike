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

    fun getSampleChaptersForBook(bookId: String): List<BookChapter> {
        return when (bookId) {
            "book-metamorphosis" -> listOf(
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
            "book-art-of-war" -> listOf(
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
            "book-meditations" -> listOf(
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
            "book-sherlock-holmes" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter 1: Mr. Sherlock Holmes",
                    content = """In the year 1878 I took my degree of Doctor of Medicine of the University of London, and proceeded to Netley to go through the course prescribed for surgeons in the army. Having completed my studies there, I was duly attached to the Fifth Northumberland Fusiliers as Assistant Surgeon. The regiment was stationed in India at the time, and before I could join it, the second Afghan war had broken out.

On landing at Bombay, I learned that my corps had advanced through the passes, and was already deep in the enemy's country. I followed, however, with many other officers who were in the same situation, and succeeded in reaching Candahar in safety, where I found my regiment, and at once entered upon my new duties. The campaign brought honours and promotion to many, but for me it had nothing but misfortune and disaster.

I was removed from my brigade and attached to the Berkshires, with whom I served at the fatal battle of Maiwand. There I was struck on the shoulder by a Jezail bullet, which shattered the bone and grazed the subclavian artery. I should have fallen into the hands of the murderous Ghazis had it not been for the devotion and courage shown by Murray, my orderly, who threw me across a pack-horse, and succeeded in bringing me safely to the British lines.

Worn with pain, and weak from the prolonged hardships which I had undergone, I was removed to the base hospital at Peshawur. Here I rallied, and had already improved so far as to be able to walk about the wards, and even to bask a little upon the verandah, when I was struck down by enteric fever, that curse of our Indian possessions. For months my life was despaired of, and when at last I came to myself and became convalescent, I was so weak and emaciated that a medical board determined that not a day should be lost in sending me back to England."""
                ),
                BookChapter(
                    index = 1,
                    title = "Chapter 2: The Science of Deduction",
                    content = """We met next day as he had arranged, and inspected the rooms at No. 221B, Baker Street, of which he had spoken at our meeting. They consisted of a couple of comfortable bed-rooms and a single large airy sitting-room, cheerfully furnished, and illuminated by two broad windows. So desirable in every way were the apartments, and so moderate did the terms seem when divided between us, that the bargain was concluded upon the spot, and we at once took possession.

Sherlock Holmes was not a man who lived into difficult positions; he was quiet, regular, and had habits of astonishing regularity. It was rare for him to be up after ten at night, and he had invariably breakfasted and gone out before I rose in the morning. Sometimes he spent his day at the chemical laboratory, sometimes in the dissecting-rooms, and occasionally in long walks, which appeared to take him into the roughest suburbs of the city.

His energy was enthusiasm itself when the fit was on him; but about every few days a reaction would set in, and for days on end he would lie upon the sofa in the sitting-room, hardly uttering a word or moving a muscle from morning to night. On these occasions I have noticed such a dreamy, vacant expression in his eyes, that I might have suspected him of being addicted to the use of some narcotic, had not the temperance and cleanliness of his whole life forbidden such a notion."""
                ),
                BookChapter(
                    index = 2,
                    title = "Chapter 3: The Lauriston Garden Mystery",
                    content = """I confess that I was considerably startled by this fresh proof of the practical nature of my companion's theories. My respect for his powers of analysis increased wondrously. There still remained some lurking suspicion in my mind, however, that the whole thing was a prearranged episode, intended to dazzle me, though what earthly object he could have in taking me in was past my comprehension.

"Here is a telegram," said he, tossing it across the table. "It has just come. Read it."
It was from Gregson of Scotland Yard:
'There has been a bad business during the night at 3, Lauriston Gardens, off the Brixton Road. Our man on the beat saw a light there about two in the morning, and found the house empty and the front door open. In the front room a gentleman was found dead, well dressed, and having cards in his pocket bearing the name of Enoch J. Drebber, Cleveland, Ohio, U.S.A. There is no clue as to how the man met his death.'

"What do you say to that?" asked Sherlock Holmes.
"It is remarkable! What do you intend to do?"
"To go to Brixton and examine the scene of the crime," he answered, springing to his feet."""
                )
            )
            "book-alice-wonderland" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter I: Down the Rabbit-Hole",
                    content = """Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do: once or twice she had peeped into the book her sister was reading, but it had no pictures or conversations in it, "and what is the use of a book," thought Alice "without pictures or conversations?"

So she was considering in her own mind (as well as she could, for the hot day made her feel very sleepy and stupid), whether the pleasure of making a daisy-chain would be worth the trouble of getting up and picking the daisies, when suddenly a White Rabbit with pink eyes ran close by her.

There was nothing so very remarkable in that; nor did Alice think it so very much out of the way to hear the Rabbit say to itself, "Oh dear! Oh dear! I shall be late!" (when she thought it over afterwards, it occurred to her that she ought to have wondered at this, but at the time it all seemed quite natural); but when the Rabbit actually took a watch out of its waistcoat-pocket, and looked at it, and then hurried on, Alice started to her feet, for it flashed across her mind that she had never before seen a rabbit with either a waistcoat-pocket, or a watch to take out of it, and burning with curiosity, she ran across the field after it, and fortunately was just in time to see it pop down a large rabbit-hole under the hedge."""
                ),
                BookChapter(
                    index = 1,
                    title = "Chapter II: The Pool of Tears",
                    content = """"Curiouser and curiouser!" cried Alice (she was so much surprised, that for the moment she quite forgot how to speak good English); "now I'm opening out like the largest telescope that ever was! Good-bye, feet!" (for when she looked down at her feet, they seemed to be almost out of sight, they were getting so far off). "Oh, my poor little feet, I wonder who will put on your shoes and stockings for you now, dears? I'm sure I shan't be able! I shall be a great deal too far off to trouble myself about you: you must manage the best way you can;—but I must be kind to them," thought Alice, "or perhaps they won't walk the way I want to go! Let me see: I'll give them a new pair of boots every Christmas."

And she went on planning to herself how she would manage it. "They must go by the carrier," she thought; "and how funny it'll seem, sending presents to one's own feet! And how odd the directions will look!"

Just then her head struck against the roof of the hall: in fact she was now more than nine feet high, and she at once took up the little golden key and hurried off to the garden door."""
                )
            )
            "book-frankenstein" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Letter 1: Arctic Voyage",
                    content = """To Mrs. Saville, England.
St. Petersburgh, Dec. 11th, 17—.
You will rejoice to hear that no disaster has accompanied the commencement of an enterprise which you have regarded with such evil forebodings. I arrived here yesterday, and my first task is to assure my dear sister of my welfare and increasing confidence in the success of my undertaking.

I am already far north of London, and as I walk in the streets of Petersburgh, I feel a cold northern breeze play upon my cheeks, which braces my nerves and fills me with delight. Do you understand this feeling? This breeze, which has travelled from the regions towards which I am advancing, gives me a foretaste of those icy climes. Inspirited by this wind of promise, my daydreams become more fervent and vivid. I try in vain to be persuaded that the pole is the seat of frost and desolation; it ever presents itself to my imagination as the region of beauty and delight."""
                ),
                BookChapter(
                    index = 1,
                    title = "Chapter 4: The Creation of Life",
                    content = """No one can conceive the variety of feelings which bore me onwards, like a whirlwind, among the first days of my success. Life and death appeared to me ideal bounds, which I should first break through, and pour a torrent of light into our dark world. A new species would bless me as its creator and source; many happy and excellent natures would owe their being to me.

No father could claim the gratitude of his child so completely as I should deserve theirs. Pursuing these reflections, I thought that if I could bestow animation upon lifeless matter, I might in process of time (although I now found it impossible) renew life where death had apparently devoted the body to corruption.

These thoughts supported my spirits, while I pursued my undertaking with unremitting ardour. My cheek had grown pale with study, and my person had become emaciated with confinement. Sometimes, on the very brink of certainty, I failed; yet still I clung to the hope which the next day or the next hour might realise."""
                )
            )
            "cat-great-gatsby" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter 1: West Egg & The Green Light",
                    content = """In my younger and more vulnerable years my father gave me some advice that I've been turning over in my mind ever since.

"Whenever you feel like criticizing any one," he told me, "just remember that all the people in this world haven't had the advantages that you've had."

He didn't say any more, but we've always been unusually communicative in a reserved way, and I understood that he meant a great deal more than that. In consequence, I'm inclined to reserve all judgments, a habit that has opened up many curious natures to me and also made me the victim of not a few veteran bores.

When I came back from the East last autumn I felt that I wanted the world to be in uniform and at a sort of moral attention forever; I wanted no more riotous excursions with privileged glimpses into the human heart. Only Gatsby, the man who gives his name to this book, was exempt from my reaction—Gatsby, who represented everything for which I have an unaffected scorn.

If personality is an unbroken series of successful gestures, then there was something gorgeous about him, some heightened sensitivity to the promises of life, as if he were related to one of those intricate machines that register earthquakes ten thousand miles away.

He stretched out his arms toward the dark water in a curious way, and, far as I was from him, I could have sworn he was trembling. Involuntarily I glanced seaward—and distinguished nothing except a single green light, minute and far away, that might have been the end of a dock."""
                ),
                BookChapter(
                    index = 1,
                    title = "Chapter 3: The Sumptuous Parties",
                    content = """There was music from my neighbor's house through the summer nights. In his blue gardens men and girls came and went like moths among the whisperings and the champagne and the stars. At high tide in the afternoon I watched his guests diving from the tower of his raft, or taking the sun on the hot sand of his beach while his two motor-boats slit the waters of the Sound.

On week-ends his Rolls-Royce became an omnibus, bearing parties to and from the city between nine in the morning and long past midnight, while his station wagon scampered like a brisk yellow bug to meet all trains.

I believe that on the first night I went to Gatsby's house I was one of the few guests who had actually been invited. People were not invited—they went there. They got into automobiles which bore them out to Long Island, and somehow they ended up at Gatsby's door.

"I'm Gatsby," he said suddenly. "I thought you knew, old sport. I'm afraid I'm not a very good host."

He smiled understandingly—much more than understandingly. It was one of those rare smiles with a quality of eternal reassurance in it, that you may come across four or five times in life. It faced—or seemed to face—the whole external world for an instant, and then concentrated on you with an irresistible prejudice in your favor."""
                )
            )
            "cat-dracula" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter 1: Jonathan Harker's Journal",
                    content = """3 May. Bistritz.—Left Munich at 8:35 P. M., on 1st May, arriving at Vienna early next morning; should have arrived at 6:46, but train was an hour late. Buda-Pesth seems a wonderful place, from the glimpse which I got of it from the train and the little I could see of the streets. I feared to go very far from the station, as we had arrived late and would start as near the correct time as possible.

The impression I had was that we were leaving the West and entering the East; the most western of splendid bridges over the Danube, which is here of noble width and depth, took us among the traditions of Turkish rule.

We left in pretty good time, and came after nightfall to Klausenburgh. Here I stopped for the night at the Hotel Royale. I had for dinner, or rather supper, a chicken done up some way with red pepper, which was very good but thirsty. (Mem., get recipe for Mina.)

The crowd at the inn door, which had by this time swelled to a considerable size, all made the sign of the cross and pointed two fingers towards me. With some difficulty I got a fellow-passenger to tell me what they meant; he would not answer at first, but on learning that I was English, he explained that it was a charm against the evil eye."""
                ),
                BookChapter(
                    index = 1,
                    title = "Chapter 2: Castle Dracula",
                    content = """5 May.—I must have been asleep, for certainly if I had been fully awake I must have noticed the approach of so remarkable a place. In the gloom the courtyard looked of considerable size, and as several dark ways led from it under great round arches, it perhaps seemed bigger than it really is. I have not yet been seen by any servant.

Within, stood a tall old man, clean shaven save for a long white moustache, and clad in black from head to foot, without a single speck of colour about him anywhere. He held in his hand an antique silver lamp, in which the flame burned without chimney or globe of any kind, throwing long quivering shadows as it flickered in the draught of the open door.

The old man motioned me in with his right hand with a courtly gesture, saying in excellent English, but with a strange intonation:

"Welcome to my house! Enter freely and of your own will!"

He made no motion of stepping to meet me, but stood like a statue, as though his gesture of welcome had fixed him into stone. The instant, however, that I had stepped over the threshold, he moved impulsively forward, and holding out his hand grasped mine with a strength which made me wince, an effect which was not lessened by the fact that it seemed cold as ice—more like the hand of a dead than a living man."""
                )
            )
            "cat-pride-prejudice" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter 1: Netherfield Park",
                    content = """It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.

However little known the feelings or views of such a man may be on his first entering a neighbourhood, this truth is so well fixed in the minds of the surrounding families, that he is considered the rightful property of some one or other of their daughters.

"My dear Mr. Bennet," said his lady to him one day, "have you heard that Netherfield Park is let at last?"

Mr. Bennet replied that he had not.

"But it is," returned she; "for Mrs. Long has just been here, and she told me all about it."

Mr. Bennet made no answer.

"Do you not want to know who has taken it?" cried his wife impatiently.

"You want to tell me, and I have no objection to hearing it."

This was invitation enough.

"Why, my dear, you must know, Mrs. Long says that Netherfield is taken by a young man of large fortune from the north of England; that he came down on Monday in a chaise and four to see the place, and was so much delighted with it, that he agreed with Mr. Morris immediately; that he is to take possession before Michaelmas, and some of his servants are to be in the house by the end of next week."

"What is his name?"

"Bingley."

"Is he married or single?"

"Oh! Single, my dear, to be sure! A single man of large fortune; four or five thousand a year. What a fine thing for our girls!""""
                ),
                BookChapter(
                    index = 1,
                    title = "Chapter 3: The Assembly Ball",
                    content = """Not all that Mrs. Bennet, however, with the assistance of her five daughters, could ask on the subject, was sufficient to draw from her husband any satisfactory description of Mr. Bingley. They attacked him in various ways—with barefaced questions, ingenious suppositions, and distant surmises; but he eluded the skill of them all, and they were at last obliged to accept the second-hand intelligence of their neighbour, Lady Lucas.

Her report was highly favourable. Sir William had been delighted with him. He was quite young, wonderfully handsome, extremely agreeable, and, to crown the whole, he meant to be at the next assembly with a large party.

Mr. Bingley was good-looking and gentlemanlike; he had a pleasant countenance, and easy, unaffected manners. His sisters were fine women, with an air of decided fashion. His brother-in-law, Mr. Hurst, merely looked the gentleman; but his friend Mr. Darcy soon drew the attention of the room by his fine, tall person, handsome features, noble mien, and the report which was in general circulation within five minutes after his entrance, of his having ten thousand a year.

The gentlemen pronounced him to be a fine figure of a man, the ladies declared he was much handsomer than Mr. Bingley, and he was looked at with great admiration for about half the evening, till his manners gave a disgust which turned the tide of his popularity; for he was discovered to be proud; to be above his company, and above being pleased; and not all his large estate in Derbyshire could then save him from having a most forbidding, disagreeable countenance, and being unworthy to be compared with his friend."""
                )
            )
            "cat-beyond-good-evil" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter 1: On the Prejudices of Philosophers",
                    content = """The Will to Truth, which is to tempt us to many a hazardous enterprise, the famous Truthfulness of which all philosophers have hitherto spoken with respect, what questions has this Will to Truth not laid before us! What strange, perplexing, questionable questions!

Is it any wonder if we at last grow distrustful, lose patience, and turn impatiently away? That this Sphinx teaches us at last to ask questions ourselves? Who is it really that puts questions to us here? What really is this "Will to Truth" in us?

In fact we made a long halt at the question as to the origin of this Will—until at last we came to an absolute standstill before a yet more fundamental question. We inquired about the value of this Will. Granted that we want the truth: WHY NOT RATHER untruth? And uncertainty? Even ignorance?

The problem of the value of truth presented itself before us—or was it we who presented ourselves before the problem? Which of us is the Oedipus here? Which the Sphinx? It would seem to be a rendezvous of questions and notes of interrogation.

And could it be believed that it at last seems to us as if the problem had never been propounded before, as if we were the first to discern it, get a sight of it, and RISK RAISING it?"""
                ),
                BookChapter(
                    index = 1,
                    title = "Chapter 2: The Free Spirit",
                    content = """O sancta simplicitas! In what strange simplification and falsification man lives! One can never cease wondering when once one has got eyes for this marvel! How we have made everything around us clear and free and easy and simple! How we have been able to give our senses a passport to everything superficial, our thoughts a godlike desire for wanton prancing and wrong inferences!

How from the beginning, we have contrived to retain our ignorance in order to enjoy an almost inconceivable freedom, thoughtlessness, imprudence, heartiness, and gaiety—in order to enjoy life! And only on this solidified, granite-like foundation of ignorance could knowledge rear itself hitherto, the will to knowledge on the foundation of a far more powerful will: the will to ignorance, to the uncertain, to the untrue! Not as its opposite, but—as its refinement!

It is the business of the very few to be independent; it is a privilege of the strong. And whoever attempts it, even with the best right, but without being OBLIGED to do so, proves that he is probably not only strong, but also daring beyond measure."""
                )
            )
            "cat-republic" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Book I: Justice and the Nature of Right",
                    content = """I went down yesterday to the Piraeus with Glaucon the son of Ariston, that I might offer up my prayers to the goddess; and also because I wanted to see in what manner they would celebrate the festival, which was a new thing. I was delighted with the procession of the inhabitants; but that of the Thracians was equally, if not more, beautiful. When we had finished our prayers and viewed the spectacle, we turned in the direction of the city; and at that instant Polemarchus the son of Cephalus chanced to catch sight of us from a distance as we were starting on our way home, and told his servant to run and bid us wait for him.

"Socrates," said Cephalus, "you ought to come oftener to the city and see us; for I am getting old, and my bodily strength is failing, but my delight in conversation and the pleasures of the mind only increase."

"Cephalus," I said, "there is nothing which I like better than conversing with aged men; for I regard them as travellers who have gone a journey which I too may have to go, and of whom I ought to inquire, whether the way is smooth and easy, or rugged and difficult. And this is a question which I should like to ask of you who have arrived at that time which the poets call the 'threshold of old age'—Is life harder towards the end, or what report do you give of it?"

"I will tell you, Socrates," he said, "what my own feeling is. Men of my age flock together; we are birds of a feather, as the old proverb says; and at our meetings the tale of my acquaintance commonly is—I cannot eat, I cannot drink; the pleasures of youth and love are fled away. But in my opinion, he who is of a calm and happy nature will hardly feel the pressure of age.""""
                ),
                BookChapter(
                    index = 1,
                    title = "Book VII: The Allegory of the Cave",
                    content = """And now, I said, let me show in a figure how far our nature is enlightened or unenlightened:—Behold! human beings living in an underground den, which has a mouth open towards the light and reaching all along the den; here they have been from their childhood, and have their legs and necks chained so that they cannot move, and can only see before them, being prevented by the chains from turning round their heads.

Above and behind them a fire is blazing at a distance, and between the fire and the prisoners there is a raised way; and you will see, if you look, a low wall built along the way, like the screen which marionette players have in front of them, over which they show the puppets.

"I see."

And do you see, I said, men passing along the wall carrying all sorts of vessels, and statues and figures of animals made of wood and stone and various materials, which appear over the wall? Some of them are talking, others silent.

"You have shown me a strange image, and they are strange prisoners."

"Like ourselves," I replied; "and they see only their own shadows, or the shadows of one another, which the fire throws on the opposite wall of the cave?"

"True," he said; "how could they see anything but the shadows if they were never allowed to move their heads?""""
                )
            )
            "cat-letters-stoic" -> listOf(
                BookChapter(
                    index = 0,
                    title = "Letter I: On Saving Time",
                    content = """Seneca greets his friend Lucilius.

Continue to act thus, my dear Lucilius: set yourself free for your own sake; gather and save your time, which till lately has been forced from you, or filched away, or has merely slipped between your fingers. Make yourself believe the truth of my words—that certain moments are torn from us, that some are gently removed, and that others glide beyond our reach. The most disgraceful kind of loss, however, is that due to carelessness.

If you will pay close heed to the problem, you will find that the largest portion of our life passes while we are doing ill, a goodly share while we are doing nothing, and the whole while we are doing that which is not to the purpose.

What man can you show me who places any value on his time, who reckons the worth of each day, who understands that he is dying daily? For we are mistaken when we look forward to death; the major part of death has already passed. Whatever years lie behind us are in death's hands.

Therefore, Lucilius, do what you write me you are doing: hold every hour in your grasp. Lay hold of today's task, and you will not need to depend so much upon tomorrow's. While we are postponing, life speeds by."""
                ),
                BookChapter(
                    index = 1,
                    title = "Letter II: On Discursiveness in Reading",
                    content = """Judging by what you write me and by what I hear, I am full of good hope for you. You do not run about or stir yourself with new residences; for such restlessness is the sign of a disordered spirit. The primary indication, to my mind, of a well-ordered mind is a man's ability to remain in one place and linger in his own company.

Be careful, however, lest this reading of many authors and books of every sort may tend to make you discursive and unsteady. You must linger among a limited number of master-thinkers, and digest their works, if you would derive ideas which shall win firm hold in your mind.

Everywhere means nowhere. When a person spends all his time in foreign travel, he ends by having many acquaintances, but no friends. And the same thing must hold true of men who seek intimate acquaintance with no single author, but visit them all in a fleeting, hurried manner.

Food does no good and is not assimilated into the body if it is left in the stomach only a moment; nothing hinders a cure so much as frequent change of medicine. To be always changing books is like never giving medicine time to work. Read therefore from the most respected authors, and if at any time you turn aside for a moment to others, return immediately to the masters."""
                )
            )
            "cat-dorian-gray" -> listOf(
                BookChapter(
                    index = 0,
                    title = "The Preface & Chapter 1: The Studio",
                    content = """The artist is the creator of beautiful things. To reveal art and conceal the artist is art's aim. The critic is he who can translate into another manner or a new material his impression of beautiful things.

The highest as the lowest form of criticism is a mode of autobiography. Those who find ugly meanings in beautiful things are corrupt without being charming. This is a fault. Those who find beautiful meanings in beautiful things are the cultivated. For these there is hope. They are the elect to whom beautiful things mean only beauty.

There is no such thing as a moral or an immoral book. Books are well written, or badly written. That is all.

The studio was filled with the rich odour of roses, and when the light summer wind stirred amidst the trees of the garden, there came through the open door the heavy scent of the lilac, or the more delicate perfume of the pink-flowering thorn.

From the corner of the divan of Persian saddle-bags on which he was lying, smoking, as was his custom, innumerable cigarettes, Lord Henry Wotton could just catch the gleam of the honey-sweet and honey-coloured blossoms of a laburnum, whose tremulous branches seemed hardly able to bear the burden of a beauty so flame-like as theirs.

In the centre of the room, clamped to an upright easel, stood the full-length portrait of a young man of extraordinary personal beauty, and in front of it, some little distance away, was sitting the artist himself, Basil Hallward."""
                ),
                BookChapter(
                    index = 1,
                    title = "Chapter 2: The Philosophy of Lord Henry",
                    content = """As they entered they saw Dorian Gray. He was seated at the piano, with his back to them, turning over the pages of a volume of Schumann's "Forest Scenes."

"You must lend me these, Basil," he cried. "I want to learn them. They are perfectly charming."

"That entirely depends on how you sit to-day, Dorian."

"Oh, I am tired of sitting, and I don't want a life-sized portrait of myself," answered the boy, swinging round on the music-stool in a wilful, petulant manner. When he caught sight of Lord Henry, a faint blush coloured his cheeks for a moment, and he started up. "I beg your pardon, Basil, but I didn't know you had any one with you."

"This is Lord Henry Wotton, an old Oxford friend of mine. I have just been telling him what a capital sitter you were, and now you have spoiled everything."

"You have only a few years in which to live really, perfectly, and fully," whispered Lord Henry, leaning close to Dorian. "When your youth goes, your beauty will go with it, and then you will suddenly discover that there are no triumphs left for you. Time is jealous of you, and wars against your lilies and your roses. You will become sallow, and hollow-cheeked, and dull-eyed. You will suffer horribly... Ah! Realize your youth while you have it!""""
                )
            )
            else -> listOf(
                BookChapter(
                    index = 0,
                    title = "Part I: Historical & Philosophical Foundations",
                    content = """The pursuit of literature and philosophical inquiry represents the enduring effort of human thought to articulate the nature of existence, duty, truth, and society. Across classical antiquities, the Enlightenment, and the modern era, the written word serves as an uncorrupted record of human consciousness.

When reading deeply, the mind is challenged to engage not passively, but in an active dialectic with the author's arguments. Every sustained inquiry demands methodical attention: observing how initial premises are established, testing each proposition against reason and human experience, and noting the underlying tensions between individual freedom and societal duty.

As you progress through this volume, engage directly with the text through careful annotations, highlighting pivotal arguments, and reflecting on how these timeless principles illuminate modern dilemmas."""
                ),
                BookChapter(
                    index = 1,
                    title = "Part II: Analysis & Critical Reflections",
                    content = """The enduring quality of a masterwork is demonstrated when its central propositions continue to evoke critical scrutiny and fresh interpretation across generations.

In examining this text, attend carefully to the author's use of rhetoric, deductive structure, and thematic symbolism. A thoughtful reader interrogates the narrative: What assumptions remain unstated? How do the actions of the central figures reflect universal human instincts? In what ways does the historical context shape the boundaries of the discussion?

True literacy is the practice of contemplative reflection—transforming discrete observations into enduring wisdom."""
                )
            )
        }
    }
}
