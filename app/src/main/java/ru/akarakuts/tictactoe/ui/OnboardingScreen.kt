package ru.akarakuts.tictactoe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.theme.BoardPalette

private data class OnboardingSlide(
    val glyph: String,
    val titleRes: Int,
    val bodyRes: Int,
)

private val slides = listOf(
    OnboardingSlide("✕○", R.string.onboarding_slide1_title, R.string.onboarding_slide1_body),
    OnboardingSlide("3—6", R.string.onboarding_slide2_title, R.string.onboarding_slide2_body),
    OnboardingSlide("🎨", R.string.onboarding_slide3_title, R.string.onboarding_slide3_body),
    OnboardingSlide("🏆", R.string.onboarding_slide4_title, R.string.onboarding_slide4_body),
)

/**
 * First-launch onboarding: a short swipeable intro to rules, modes, theme unlocks, and stats.
 * The last slide's button closes the flow via [onDone].
 */
@Composable
fun OnboardingScreen(palette: BoardPalette, onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == slides.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.sceneBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .widthIn(max = 460.dp)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val slide = slides[page]
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = palette.panelFill,
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.panelStroke),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = slide.glyph,
                            color = palette.crossMark,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(slide.titleRes),
                        color = palette.statusText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(slide.bodyRes),
                        color = palette.captionText,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 20.dp),
                ) {
                    repeat(slides.size) { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (selected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) palette.pillStrokeSelected
                                    else palette.pillStroke.copy(alpha = palette.pillStroke.alpha * 0.5f)
                                ),
                        )
                    }
                }
                HudPrimaryButton(
                    text = if (isLast) stringResource(R.string.onboarding_done)
                    else stringResource(R.string.onboarding_next),
                    palette = palette,
                    onClick = {
                        if (isLast) {
                            onDone()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                )
            }
        }
    }
}
