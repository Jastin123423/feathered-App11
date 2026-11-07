package com.unera.social.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.unera.social.R
import com.unera.social.model.UserReaction
import kotlin.math.abs

/**
 * Facebook 3D Reaction Definition
 * Exactly 6 official Facebook 3D reaction emojis: Like, Love, Haha, Wow, Sad, Angry.
 */
data class FacebookReaction(
    val type: String,
    val label: String,
    val resId: Int,
    val emoji: String,
    val color: Color
) {
    // Backward-compatible alias for any existing callers referencing gifRes
    val gifRes: Int get() = resId
}

val FACEBOOK_REACTIONS = listOf(
    FacebookReaction("like", "Like", R.drawable.reaction_fb_3d_like, "👍", Color(0xFF1877F2)),
    FacebookReaction("love", "Love", R.drawable.reaction_fb_3d_love, "❤️", Color(0xFFF43F5E)),
    FacebookReaction("haha", "Haha", R.drawable.reaction_fb_3d_haha, "😆", Color(0xFFFBBF24)),
    FacebookReaction("wow", "Wow", R.drawable.reaction_fb_3d_wow, "😮", Color(0xFFFBBF24)),
    FacebookReaction("sad", "Sad", R.drawable.reaction_fb_3d_sad, "😢", Color(0xFF38BDF8)),
    FacebookReaction("angry", "Angry", R.drawable.reaction_fb_3d_angry, "😡", Color(0xFFEA580C))
)

fun getFacebookReaction(type: String?): FacebookReaction? {
    if (type.isNullOrBlank()) return null
    val match = FACEBOOK_REACTIONS.find { it.type.equals(type, ignoreCase = true) }
    if (match != null) return match
    // Graceful fallback for legacy 'care' reaction
    if (type.equals("care", ignoreCase = true)) return FACEBOOK_REACTIONS.find { it.type == "love" }
    return null
}

/**
 * Facebook 3D Animated / Movable Reactions Dock
 *
 * Appears when the user holds / long-presses on the React button.
 * Features:
 * - Exactly 6 high quality 3D Facebook reaction emojis
 * - Dynamic movable 3D animations:
 *   - Hover spring bounce and upward float
 *   - Type-specific living motion (Like rocks, Love beats, Haha bounces, Wow wobbles, Sad sways, Angry shakes)
 *   - Subtle ambient breathing float when resting
 * - Smooth horizontal drag scrubbing with labeled tooltip bubbles
 */
@Composable
fun FacebookReactionDock(
    onSelectReaction: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var dockWidthPx by remember { mutableStateOf(0f) }

    // Entrance animation
    var isReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isReady = true }

    val entranceScale by animateFloatAsState(
        targetValue = if (isReady) 1.0f else 0.65f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "entranceScale"
    )

    val entranceAlpha by animateFloatAsState(
        targetValue = if (isReady) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 180),
        label = "entranceAlpha"
    )

    // Floating Dock container
    Surface(
        shape = RoundedCornerShape(36.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.98f),
        border = BorderStroke(1.2.dp, Color(0xFF334155)),
        shadowElevation = 22.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = entranceScale
                scaleY = entranceScale
                alpha = entranceAlpha
            }
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .onGloballyPositioned { coords ->
                dockWidthPx = coords.size.width.toFloat()
            }
            .pointerInput(dockWidthPx) {
                if (dockWidthPx > 0f) {
                    val itemCount = FACEBOOK_REACTIONS.size
                    val itemWidth = dockWidthPx / itemCount

                    detectDragGestures(
                        onDragStart = { offset ->
                            val idx = (offset.x / itemWidth).toInt().coerceIn(0, itemCount - 1)
                            hoveredIndex = idx
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val idx = (change.position.x / itemWidth).toInt().coerceIn(0, itemCount - 1)
                            hoveredIndex = idx
                        },
                        onDragEnd = {
                            hoveredIndex?.let { idx ->
                                if (idx in FACEBOOK_REACTIONS.indices) {
                                    onSelectReaction(FACEBOOK_REACTIONS[idx].type)
                                }
                            }
                            onDismiss()
                        },
                        onDragCancel = {
                            hoveredIndex = null
                            onDismiss()
                        }
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            FACEBOOK_REACTIONS.forEachIndexed { index, reaction ->
                val isHovered = hoveredIndex == index
                val isNeighbor = hoveredIndex != null && abs(hoveredIndex!! - index) == 1

                // Wave spring scale animation
                val scale by animateFloatAsState(
                    targetValue = when {
                        isHovered -> 1.85f
                        isNeighbor -> 1.25f
                        else -> 1.0f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "reaction_scale_$index"
                )

                // Upward elevation translation on hover
                val offsetY by animateFloatAsState(
                    targetValue = when {
                        isHovered -> -22f
                        isNeighbor -> -6f
                        else -> 0f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "reaction_offset_$index"
                )

                // Movable 3D Motion Physics via InfiniteTransition
                val infiniteTransition = rememberInfiniteTransition(label = "motion_$index")

                // Idle ambient breathing float (staggered by index)
                val ambientFloat by infiniteTransition.animateFloat(
                    initialValue = -1.5f,
                    targetValue = 1.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1400 + index * 120, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ambient_float_$index"
                )

                // Active motion when hovered
                val hoverMotionRotation by infiniteTransition.animateFloat(
                    initialValue = -8f,
                    targetValue = 8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 400, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "hover_rotation_$index"
                )

                val hoverMotionScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "hover_scale_$index"
                )

                val hoverMotionBounce by infiniteTransition.animateFloat(
                    initialValue = -4f,
                    targetValue = 4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "hover_bounce_$index"
                )

                val hoverMotionTremor by infiniteTransition.animateFloat(
                    initialValue = -2.5f,
                    targetValue = 2.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 120, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "hover_tremor_$index"
                )

                // Compute specific 3D movement properties based on reaction type
                val currentRotation = when {
                    isHovered && reaction.type == "like" -> hoverMotionRotation
                    isHovered && reaction.type == "sad" -> hoverMotionRotation * 0.4f
                    isHovered && reaction.type == "wow" -> hoverMotionRotation * 0.6f
                    else -> 0f
                }

                val currentPulseScale = when {
                    isHovered && reaction.type == "love" -> hoverMotionScale
                    else -> 1.0f
                }

                val currentBounceY = when {
                    isHovered && reaction.type == "haha" -> hoverMotionBounce
                    !isHovered -> ambientFloat
                    else -> 0f
                }

                val currentTremorX = when {
                    isHovered && reaction.type == "angry" -> hoverMotionTremor
                    else -> 0f
                }

                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .clickable {
                            onSelectReaction(reaction.type)
                            onDismiss()
                        }
                        .zIndex(if (isHovered) 5f else 1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .offset(
                                x = currentTremorX.dp,
                                y = (offsetY + currentBounceY).dp
                            )
                            .padding(horizontal = 2.dp)
                    ) {
                        // Tooltip label bubble shown above hovered reaction
                        if (isHovered) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF020617).copy(alpha = 0.95f))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = reaction.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // High Quality 3D Movable Emoji Icon
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .scale(scale * currentPulseScale)
                                .graphicsLayer {
                                    rotationZ = currentRotation
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = reaction.resId),
                                contentDescription = reaction.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Facebook Reaction Action Button
 *
 * Tap = Like / Unlike
 * Hold (Long Press) = Shows Facebook 3D Movable Reactions Dock
 */
@Composable
fun FacebookReactionButton(
    isLiked: Boolean,
    myReaction: String?,
    likesCount: Int,
    onLike: () -> Unit,
    onReact: (String) -> Unit,
    showReactionDock: Boolean,
    onToggleReactionDock: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentReaction = getFacebookReaction(myReaction)

    Box(modifier = modifier) {
        // Floating 3D animated movable dock when activated
        if (showReactionDock) {
            Box(
                modifier = Modifier
                    .offset(y = (-68).dp)
                    .align(Alignment.BottomStart)
                    .zIndex(20f)
            ) {
                FacebookReactionDock(
                    onSelectReaction = { type ->
                        onReact(type)
                        onToggleReactionDock(false)
                    },
                    onDismiss = {
                        onToggleReactionDock(false)
                    }
                )
            }
        }

        // The button itself
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(showReactionDock) {
                    detectTapGestures(
                        onLongPress = {
                            onToggleReactionDock(true)
                        },
                        onTap = {
                            if (showReactionDock) {
                                onToggleReactionDock(false)
                            } else {
                                onLike()
                            }
                        }
                    )
                }
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentReaction != null) {
                    // Show active high quality 3D reaction icon
                    Box(
                        modifier = Modifier.size(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = currentReaction.resId),
                            contentDescription = currentReaction.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (likesCount > 0) "$likesCount" else currentReaction.label,
                        color = currentReaction.color,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isLiked) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Liked",
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(22.dp)
                    )
                    if (likesCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = likesCount.toString(),
                            color = Color(0xFFF43F5E),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = Color(0xFFF8FAFC),
                        modifier = Modifier.size(22.dp)
                    )
                    if (likesCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = likesCount.toString(),
                            color = Color(0xFFF8FAFC),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Facebook 3D Non-Moving Reactions Summary Row for Cards
 *
 * Renders 3 overlapping high quality 3D non-moving reaction emoji icons
 * (Like, Love, Haha, or the post's top reactions) followed by "${count} Reactions".
 * Matches Facebook's exact feed post engagement layout with clean 3D circle badges.
 */
@Composable
fun FacebookReactionSummaryRow(
    likesCount: Int,
    reactions: List<UserReaction> = emptyList(),
    onOpenReactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine the 3 reaction types to display
    val displayTypes = remember(reactions) {
        if (reactions.isNotEmpty()) {
            val topFromPost = reactions.groupBy { it.type.lowercase() }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .map { it.first }
                .filter { type -> FACEBOOK_REACTIONS.any { it.type.equals(type, ignoreCase = true) } }
            if (topFromPost.isNotEmpty()) {
                (topFromPost + listOf("like", "love", "haha")).distinct().take(3)
            } else {
                listOf("like", "love", "haha")
            }
        } else {
            listOf("like", "love", "haha")
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onOpenReactions)
            .padding(vertical = 2.dp)
    ) {
        // Overlapping 3D non-moving emoji circle icons
        Row(
            horizontalArrangement = Arrangement.spacedBy((-5).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            displayTypes.forEachIndexed { index, type ->
                val reaction = getFacebookReaction(type) ?: FACEBOOK_REACTIONS[index % FACEBOOK_REACTIONS.size]
                Box(
                    modifier = Modifier
                        .zIndex((4 - index).toFloat())
                        .size(19.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(1.2.dp, Color(0xFF0B1120), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = reaction.resId),
                        contentDescription = reaction.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = if (likesCount == 1) "1 Reaction" else "$likesCount Reactions",
            color = Color(0xFFF8FAFC),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
