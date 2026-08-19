package com.example.util

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
    val isRtl: Boolean = false
) {
    ENGLISH("en", "English", "English", "🇺🇸", false),
    ARABIC("ar", "Arabic", "العربية", "🇸🇦", true),
    FRENCH("fr", "French", "Français", "🇫🇷", false)
}

object AppStrings {
    fun get(key: String, language: AppLanguage): String {
        return when (language) {
            AppLanguage.ARABIC -> arabicStrings[key] ?: englishStrings[key] ?: key
            AppLanguage.FRENCH -> frenchStrings[key] ?: englishStrings[key] ?: key
            AppLanguage.ENGLISH -> englishStrings[key] ?: key
        }
    }

    private val englishStrings = mapOf(
        "app_name" to "A-Hex streak",
        "tab_library" to "Library",
        "tab_club" to "Club",
        "tab_streak" to "Streak",
        "tab_highlights" to "Highlights",
        "tab_discover" to "Discover",
        "tab_settings" to "Settings",
        "search_hint" to "Search books, authors, genres...",
        "search_trusted_title" to "Trusted Book & PDF Search Engine",
        "search_trusted_desc" to "Search verified online repositories including Google Books, Google PDF search, Project Gutenberg, and Open Library.",
        "search_google_books" to "Google Books",
        "search_google_pdf" to "Google PDF Files",
        "search_gutenberg" to "Project Gutenberg",
        "search_open_library" to "Open Library",
        "search_standard_ebooks" to "Standard Ebooks",
        "language_title" to "Language / اللغة / Langue",
        "language_desc" to "Select your preferred application display language.",
        "storage_title" to "Storage & Database",
        "installed_books" to "Installed Books in Library",
        "saved_highlights" to "Total Saved Highlights",
        "offline_cache" to "Offline Local Cache",
        "features_title" to "Included Engine Features",
        "visit_instagram" to "Open @ahex0_01 on Instagram",
        "creator_desc" to "Official Creator & Developer Profile",
        "speed_reader" to "RSVP Speed Reader",
        "ai_assistant" to "Smart Reading Assistant",
        "reading_streak" to "A-HEX READING STREAK",
        "active_habit" to "Active Habit",
        "open_in_browser" to "Search Online",
        "direct_pdf_download" to "Download PDF",
        "trusted_badge" to "Verified Public Domain & Open Access"
    )

    private val arabicStrings = mapOf(
        "app_name" to "A-Hex streak",
        "tab_library" to "المكتبة",
        "tab_club" to "المجتمع",
        "tab_streak" to "الإنجاز",
        "tab_highlights" to "الاقتباسات",
        "tab_discover" to "استكشاف",
        "tab_settings" to "الإعدادات",
        "search_hint" to "ابحث عن كتب، مؤلفين، تصنيفات...",
        "search_trusted_title" to "محرك بحث الكتب وملفات PDF الموثوقة",
        "search_trusted_desc" to "ابحث في المصادر والمكتبات الموثوقة عبر الإنترنت بما في ذلك Google Books وكتب PDF وProject Gutenberg وOpen Library.",
        "search_google_books" to "كتب Google",
        "search_google_pdf" to "ملفات Google PDF",
        "search_gutenberg" to "مشروع غوتنبرغ",
        "search_open_library" to "المكتبة المفتوحة",
        "search_standard_ebooks" to "Standard Ebooks",
        "language_title" to "لغة التطبيق / Language",
        "language_desc" to "اختر لغة العرض المفضلة لديك داخل التطبيق.",
        "storage_title" to "المساحة وقاعدة البيانات",
        "installed_books" to "الكتب المثبتة بالمكتبة",
        "saved_highlights" to "إجمالي الاقتباسات المحفوظة",
        "offline_cache" to "ذاكرة التخزين المحلية المؤقتة",
        "features_title" to "الميزات المضمنة",
        "visit_instagram" to "زيارة @ahex0_01 على إنستغرام",
        "creator_desc" to "الحساب الرسمي للمطور والمصمم",
        "speed_reader" to "القراءة السريعة RSVP",
        "ai_assistant" to "مساعد القراءة الذكي",
        "reading_streak" to "سلسلة القراءة A-HEX",
        "active_habit" to "عادة نشطة",
        "open_in_browser" to "بحث على الإنترنت",
        "direct_pdf_download" to "تحميل PDF",
        "trusted_badge" to "مصدر معتمد ومرخص للقراءة الحرة"
    )

    private val frenchStrings = mapOf(
        "app_name" to "A-Hex streak",
        "tab_library" to "Bibliothèque",
        "tab_club" to "Club",
        "tab_streak" to "Série",
        "tab_highlights" to "Extraits",
        "tab_discover" to "Découvrir",
        "tab_settings" to "Paramètres",
        "search_hint" to "Rechercher livres, auteurs, genres...",
        "search_trusted_title" to "Moteur de recherche Livres & PDF fiables",
        "search_trusted_desc" to "Recherchez parmi les bibliothèques fiables : Google Books, recherche Google PDF, Project Gutenberg et Open Library.",
        "search_google_books" to "Google Livres",
        "search_google_pdf" to "Fichiers PDF Google",
        "search_gutenberg" to "Projet Gutenberg",
        "search_open_library" to "Open Library",
        "search_standard_ebooks" to "Standard Ebooks",
        "language_title" to "Langue / Language",
        "language_desc" to "Sélectionnez votre langue d'affichage préférée.",
        "storage_title" to "Stockage et base de données",
        "installed_books" to "Livres installés en bibliothèque",
        "saved_highlights" to "Total des passages surlignés",
        "offline_cache" to "Cache local hors-ligne",
        "features_title" to "Fonctionnalités incluses",
        "visit_instagram" to "Ouvrir @ahex0_01 sur Instagram",
        "creator_desc" to "Profil officiel du créateur et développeur",
        "speed_reader" to "Lecteur rapide RSVP",
        "ai_assistant" to "Assistant de lecture intelligent",
        "reading_streak" to "SÉRIE DE LECTURE A-HEX",
        "active_habit" to "Habitude active",
        "open_in_browser" to "Rechercher en ligne",
        "direct_pdf_download" to "Télécharger le PDF",
        "trusted_badge" to "Domaine public vérifié et accès libre"
    )
}
