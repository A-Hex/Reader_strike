package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ShareContentType
import com.example.ui.components.SocialShareModal
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

data class CommunityChallenge(
    val id: String,
    val title: String,
    val description: String,
    val tag: String,
    val participantsCount: Int,
    val daysRemaining: Int,
    val progressPercent: Float,
    val isJoined: Boolean = false,
    val rewardBadge: String
)

data class CommunityPost(
    val id: String,
    val authorName: String,
    val authorHandle: String,
    val bookTitle: String,
    val quoteOrNote: String,
    val likesCount: Int,
    val commentsCount: Int,
    val timestamp: String,
    val isLiked: Boolean = false,
    val tag: String
)

@Composable
fun CommunityScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All Feed") }
    var showShareModal by remember { mutableStateOf(false) }
    var showNewPostDialog by remember { mutableStateOf(false) }
    var newPostContent by remember { mutableStateOf("") }
    var newPostBook by remember { mutableStateOf("Meditations") }
    val streakData by viewModel.streakData.collectAsState()

    var challenges by remember {
        mutableStateOf(
            listOf(
                CommunityChallenge(
                    id = "c1",
                    title = "30-Day Stoic & Wisdom Marathon",
                    description = "Read at least 15 minutes of Marcus Aurelius or Seneca every day.",
                    tag = "Philosophy",
                    participantsCount = 1840,
                    daysRemaining = 12,
                    progressPercent = 0.65f,
                    isJoined = true,
                    rewardBadge = "👑 Stoic Philosopher"
                ),
                CommunityChallenge(
                    id = "c2",
                    title = "Classic Victorian Novels Sprint",
                    description = "Complete 2 classic 19th-century works this month.",
                    tag = "Classics",
                    participantsCount = 2410,
                    daysRemaining = 18,
                    progressPercent = 0.30f,
                    isJoined = true,
                    rewardBadge = "📜 Victorian Scholar"
                ),
                CommunityChallenge(
                    id = "c3",
                    title = "Speed Reader 500 WPM Goal",
                    description = "Practice RSVP speed reading for 5 chapters.",
                    tag = "Speed Reading",
                    participantsCount = 920,
                    daysRemaining = 6,
                    progressPercent = 0.0f,
                    isJoined = false,
                    rewardBadge = "⚡ Flash Reader"
                )
            )
        )
    }

    var posts by remember {
        mutableStateOf(
            listOf(
                CommunityPost(
                    id = "p1",
                    authorName = "Elena Vance",
                    authorHandle = "@elena_reads",
                    bookTitle = "Meditations",
                    quoteOrNote = "\"You have power over your mind - not outside events. Realize this, and you will find strength.\" This passage completely shifted how I approach stressful mornings!",
                    likesCount = 42,
                    commentsCount = 8,
                    timestamp = "2h ago",
                    isLiked = true,
                    tag = "Stoicism"
                ),
                CommunityPost(
                    id = "p2",
                    authorName = "Marcus K.",
                    authorHandle = "@marcus_thoughts",
                    bookTitle = "The Art of War",
                    quoteOrNote = "\"In the midst of chaos, there is also opportunity.\" Sun Tzu's strategic insights apply so directly to modern focus and deep work.",
                    likesCount = 35,
                    commentsCount = 5,
                    timestamp = "4h ago",
                    isLiked = false,
                    tag = "Strategy"
                ),
                CommunityPost(
                    id = "p3",
                    authorName = "Clara Bennett",
                    authorHandle = "@clara_lit",
                    bookTitle = "The Metamorphosis",
                    quoteOrNote = "Kafka's exploration of alienation feels so strikingly contemporary. Re-reading this with the Obsidian theme was pure bliss.",
                    likesCount = 29,
                    commentsCount = 3,
                    timestamp = "6h ago",
                    isLiked = false,
                    tag = "Classics"
                )
            )
        )
    }

    if (showShareModal) {
        SocialShareModal(
            contentType = ShareContentType.Stats(streakData),
            onDismiss = { showShareModal = false }
        )
    }

    if (showNewPostDialog) {
        AlertDialog(
            onDismissRequest = { showNewPostDialog = false },
            title = { Text("Share Reading Insight", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Book:", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                    OutlinedTextField(
                        value = newPostBook,
                        onValueChange = { newPostBook = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text("Your Insight or Quote:", style = MaterialTheme.typography.labelSmall, color = NaturalDarkTextMuted)
                    OutlinedTextField(
                        value = newPostContent,
                        onValueChange = { newPostContent = it },
                        placeholder = { Text("What did you learn or highlight today?") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPostContent.isNotBlank()) {
                            val newPost = CommunityPost(
                                id = "post-${System.currentTimeMillis()}",
                                authorName = "You",
                                authorHandle = "@bookworm_reader",
                                bookTitle = newPostBook,
                                quoteOrNote = newPostContent,
                                likesCount = 1,
                                commentsCount = 0,
                                timestamp = "Just now",
                                isLiked = true,
                                tag = "ReadingJourney"
                            )
                            posts = listOf(newPost) + posts
                            newPostContent = ""
                            showNewPostDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalPrimary)
                ) {
                    Text("Post to Community", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 36.dp)
    ) {
        // Community Banner Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NaturalPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = NaturalOnPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Readers Community",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Connected with global book clubs & readers worldwide",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalOchreAccent
                                )
                            }
                        }

                        IconButton(
                            onClick = { showNewPostDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NaturalPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Post", tint = NaturalOnPrimary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Text(
                        text = "Join reading challenges, share daily quotes, discuss literary classics, and climb the reader leaderboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalDarkTextMuted
                    )
                }
            }
        }

        // Active Challenges Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 Active Reading Challenges",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NaturalPrimary
                    )
                    Text(
                        text = "3 active",
                        style = MaterialTheme.typography.labelSmall,
                        color = NaturalDarkTextMuted
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(challenges) { challenge ->
                        Card(
                            modifier = Modifier
                                .width(280.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = NaturalPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = challenge.tag,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = NaturalPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = "${challenge.daysRemaining}d left",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalOchreAccent
                                    )
                                }

                                Text(
                                    text = challenge.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = challenge.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NaturalDarkTextMuted,
                                    maxLines = 2
                                )

                                // Progress
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${(challenge.progressPercent * 100).toInt()}% Done",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = NaturalPrimary
                                        )
                                        Text(
                                            text = "${challenge.participantsCount} readers",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NaturalDarkTextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { challenge.progressPercent },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = NaturalPrimary,
                                        trackColor = NaturalDarkBorder
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = challenge.rewardBadge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = NaturalSecondary
                                    )

                                    FilledTonalButton(
                                        onClick = {
                                            challenges = challenges.map {
                                                if (it.id == challenge.id) it.copy(isJoined = !it.isJoined) else it
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (challenge.isJoined) NaturalPrimary.copy(alpha = 0.2f) else NaturalPrimary
                                        )
                                    ) {
                                        Text(
                                            text = if (challenge.isJoined) "Joined ✓" else "Join",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (challenge.isJoined) NaturalPrimary else NaturalOnPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Leaderboard Highlight Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalDarkBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = NaturalOchreAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "🏆 #4",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NaturalOchreAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Your Community Rank: #4",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${streakData.currentStreakDays} days streak • ${streakData.totalMinutesRead} mins total",
                                style = MaterialTheme.typography.labelSmall,
                                color = NaturalDarkTextMuted
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showShareModal = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Share Rank", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Discussions Feed Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💬 Community Discussions & Quotes",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NaturalPrimary
                )

                Text(
                    text = "${posts.size} insights",
                    style = MaterialTheme.typography.labelSmall,
                    color = NaturalDarkTextMuted
                )
            }
        }

        // Feed Items
        items(posts) { post ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalDarkBorder.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Post Author Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NaturalSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = post.authorName.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = post.authorName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${post.authorHandle} • ${post.timestamp}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalDarkTextMuted
                                )
                            }
                        }

                        Surface(
                            color = NaturalDarkBorder,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "📖 ${post.bookTitle}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = NaturalPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Content
                    Text(
                        text = post.quoteOrNote,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            fontStyle = if (post.quoteOrNote.startsWith("\"")) FontStyle.Italic else FontStyle.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Post Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Like
                            Row(
                                modifier = Modifier.clickable {
                                    posts = posts.map {
                                        if (it.id == post.id) {
                                            it.copy(
                                                isLiked = !it.isLiked,
                                                likesCount = if (it.isLiked) it.likesCount - 1 else it.likesCount + 1
                                            )
                                        } else it
                                    }
                                },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (post.isLiked) Color(0xFFEF4444) else NaturalDarkTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${post.likesCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (post.isLiked) Color(0xFFEF4444) else NaturalDarkTextMuted
                                )
                            }

                            // Comments
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Comment",
                                    tint = NaturalDarkTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${post.commentsCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalDarkTextMuted
                                )
                            }
                        }

                        // Share action
                        IconButton(
                            onClick = { showShareModal = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = NaturalOchreAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
