package ru.akarakuts.tictactoe.audio

/** Abstraction over UI sound effects so ViewModels can be tested without [AudioTrack]. */
interface SoundPlayer {
    var soundEffectsEnabled: Boolean
    var volume: Float

    fun playMoveTap()
    fun playAiMove()
    fun playInvalidMove()
    fun playWinFanfare()
    fun playLoss()
    fun playThemeUnlock()
}

/** Default implementation delegating to [GameSoundFx]. */
object DefaultSoundPlayer : SoundPlayer {
    override var soundEffectsEnabled: Boolean
        get() = GameSoundFx.soundEffectsEnabled
        set(value) { GameSoundFx.soundEffectsEnabled = value }

    override var volume: Float
        get() = GameSoundFx.volume
        set(value) { GameSoundFx.volume = value }

    override fun playMoveTap() = GameSoundFx.playMoveTap()
    override fun playAiMove() = GameSoundFx.playAiMove()
    override fun playInvalidMove() = GameSoundFx.playInvalidMove()
    override fun playWinFanfare() = GameSoundFx.playWinFanfare()
    override fun playLoss() = GameSoundFx.playLoss()
    override fun playThemeUnlock() = GameSoundFx.playThemeUnlock()
}
