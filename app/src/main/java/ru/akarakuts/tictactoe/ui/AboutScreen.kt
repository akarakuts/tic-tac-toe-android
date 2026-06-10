package ru.akarakuts.tictactoe.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.akarakuts.tictactoe.BuildConfig
import ru.akarakuts.tictactoe.R
import ru.akarakuts.tictactoe.theme.BoardPalette

/** About tab: version, license, support contact, privacy policy link. */
@Composable
fun AboutScreen(palette: BoardPalette) {
    val context = LocalContext.current
    val privacyUrl = stringResource(R.string.about_privacy_url)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.sceneBackground),
    ) {
        val contentWidth = (maxWidth - 32.dp).coerceAtMost(560.dp)
        Column(
            modifier = Modifier
                .width(contentWidth)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.screen_about_subtitle),
                color = palette.captionText,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.app_name),
                color = palette.statusText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                color = palette.captionText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.about_description),
                color = palette.statusText.copy(alpha = palette.statusText.alpha * 0.92f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.about_license),
                color = palette.captionText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            HudPrimaryButton(
                text = stringResource(R.string.about_contact),
                palette = palette,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:aleksey@karakuts.com")
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            HudPrimaryButton(
                text = stringResource(R.string.about_privacy),
                palette = palette,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
