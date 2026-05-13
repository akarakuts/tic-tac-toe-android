package ru.bars.tictactoe.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

/** Lightweight synthesized UI bleeps — no bundled assets (parity with macOS AVAudioEngine tones). */
object GameSoundFx {
    @Volatile
    var soundEffectsEnabled: Boolean = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val sampleRate = 44_100

    private fun attrs(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun format(): AudioFormat = AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    fun playWinFanfare() {
        scope.launch {
            playToneSuspend(523.25, 0.09, 0.16f)
            delay(90)
            playToneSuspend(659.25, 0.10, 0.17f)
            delay(100)
            playToneSuspend(783.99, 0.14, 0.18f)
        }
    }

    fun playInvalidMove() {
        scope.launch(Dispatchers.IO) {
            playToneSuspend(165.0, 0.11, 0.22f)
        }
    }

    fun playMoveTap() {
        scope.launch(Dispatchers.IO) {
            playToneSuspend(920.0, 0.038, 0.14f)
        }
    }

    private suspend fun playToneSuspend(frequency: Double, durationSec: Double, volume: Float) {
        withContext(Dispatchers.IO) {
            playToneBlocking(frequency, durationSec, volume)
        }
    }

    private fun playToneBlocking(frequency: Double, durationSec: Double, volume: Float) {
        if (!soundEffectsEnabled) return
        if (durationSec <= 0) return
        val frameCount = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val samples = ShortArray(frameCount)
        val twoPi = 2 * PI
        val attack = (sampleRate * 0.004).toInt().coerceAtLeast(1)
        val release = (sampleRate * 0.022).toInt().coerceAtLeast(1)
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            var env = 1.0
            if (i < attack) env = i.toDouble() / attack
            val tailStart = frameCount - release
            if (i > tailStart) {
                env *= ((frameCount - i).toDouble() / release).coerceAtLeast(0.0)
            }
            val v = sin(twoPi * frequency * t) * env * volume.toDouble()
            samples[i] = (v * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufSize = maxOf(minBuf, samples.size * 2)
        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs())
            .setAudioFormat(format())
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        try {
            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep((durationSec * 1000).toLong().coerceAtLeast(1) + 80)
        } catch (_: Exception) {
            // ignore
        } finally {
            try {
                track.stop()
            } catch (_: Exception) {
            }
            track.release()
        }
    }
}
