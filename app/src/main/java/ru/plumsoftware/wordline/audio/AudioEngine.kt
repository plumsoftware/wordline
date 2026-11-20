package ru.plumsoftware.wordline.audio

import android.content.Context
import android.media.*
import android.os.Build
import ru.plumsoftware.wordline.R
import ru.plumsoftware.wordline.data.GameSettings

enum class Cue { SELECT, SUCCESS, BONUS, REWARD, NEUTRAL }
class AudioEngine(context: Context) {
    private val context=context.applicationContext
    private val manager=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val loaded=mutableSetOf<Int>()
    private val pool=SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attributes).build().apply {setOnLoadCompleteListener {_,id,status ->if(status==0) loaded+=id}}
    private val sounds=mapOf(Cue.SELECT to R.raw.game_select,Cue.SUCCESS to R.raw.game_success,Cue.BONUS to R.raw.game_bonus,Cue.REWARD to R.raw.game_reward,Cue.NEUTRAL to R.raw.game_neutral).mapValues {pool.load(this.context,it.value,1)}
    private var music: MediaPlayer?=null
    private var settings=GameSettings()
    private var active=false
    private var focused=false
    private var requested=false
    private val listener=AudioManager.OnAudioFocusChangeListener {change ->
        focused=change==AudioManager.AUDIOFOCUS_GAIN
        if(focused && active && settings.music) music?.start() else music?.pause()
    }
    private val focusRequest=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setOnAudioFocusChangeListener(listener).build()
    fun update(settings: GameSettings,active: Boolean) {
        this.settings=settings;this.active=active
        if(!active || (!settings.music && !settings.effects)) {music?.pause();if(requested) manager.abandonAudioFocusRequest(focusRequest);requested=false;focused=false;return}
        if(!requested) {focused=manager.requestAudioFocus(focusRequest)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;requested=true}
        if(settings.music && focused) {
            if(music==null) music=MediaPlayer.create(context,R.raw.game_ambient)?.apply {isLooping=true;setVolume(.18f,.18f)}
            music?.start()
        } else music?.pause()
    }
    fun play(cue: Cue) {val sound=sounds[cue] ?: return;if(active && settings.effects && focused && sound in loaded) pool.play(sound,.3f,.3f,0,0,1f)}
    fun release() {active=false;music?.release();music=null;pool.release();manager.abandonAudioFocusRequest(focusRequest)}
}
