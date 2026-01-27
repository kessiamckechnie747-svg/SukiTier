package com.sukitier.ui.easter

import android.animation.ValueAnimator
import android.media.SoundPool
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.sukitier.core.concurrency.DispatcherRegistry
import androidx.core.animation.doOnEnd

/**
 * Konami Code Easter Egg with anime-hacker aesthetic
 * Trigger: U, U, D, D, L, R, L, R, B, A
 */
class KonamiCodeListener(
    private val onTrigger: suspend () -> Unit = {}
) : View.OnKeyListener {
    
    companion object {
        private const val TAG = "KonamiCode"
        
        private val KONAMI_SEQUENCE = listOf(
            KeyEvent.KEYCODE_DPAD_UP,      // U
            KeyEvent.KEYCODE_DPAD_UP,      // U
            KeyEvent.KEYCODE_DPAD_DOWN,    // D
            KeyEvent.KEYCODE_DPAD_DOWN,    // D
            KeyEvent.KEYCODE_DPAD_LEFT,    // L
            KeyEvent.KEYCODE_DPAD_RIGHT,   // R
            KeyEvent.KEYCODE_DPAD_LEFT,    // L
            KeyEvent.KEYCODE_DPAD_RIGHT,   // R
            KeyEvent.KEYCODE_BUTTON_B,     // B
            KeyEvent.KEYCODE_BUTTON_A      // A
        )
        
        private const val SEQUENCE_TIMEOUT_MS = 5000L
        private const val INPUT_COOLDOWN_MS = 100L
    }
    
    private var currentIndex = 0
    private var lastInputTime = 0L
    private val inputLock = Any()
    private var soundPool: SoundPool? = null
    
    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action != KeyEvent.ACTION_DOWN) return false
        
        synchronized(inputLock) {
            val currentTime = System.currentTimeMillis()
            
            // Reset if too much time passed
            if (currentTime - lastInputTime > SEQUENCE_TIMEOUT_MS) {
                currentIndex = 0
                Log.d(TAG, "Sequence timeout - reset")
            }
            
            // Check for cooldown (prevent spam)
            if (currentTime - lastInputTime < INPUT_COOLDOWN_MS) {
                return false
            }
            
            lastInputTime = currentTime
            
            // Check if this key matches the sequence
            if (keyCode == KONAMI_SEQUENCE[currentIndex]) {
                currentIndex++
                Log.d(TAG, "Sequence progress: $currentIndex/${KONAMI_SEQUENCE.size}")
                
                // Visual feedback (subtle)
                v?.performHapticFeedback(
                    android.view.HapticFeedbackConstants.KEYBOARD_TAP,
                    android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
                
                // Check for completion
                if (currentIndex == KONAMI_SEQUENCE.size) {
                    currentIndex = 0
                    Log.i(TAG, "KONAMI CODE TRIGGERED!")
                    
                    // Trigger Easter egg
                    DispatcherRegistry.ROOT_SCOPE.launch {
                        triggerEasterEgg(v)
                    }
                    
                    return true
                }
            } else {
                // Wrong key - reset sequence
                Log.d(TAG, "Wrong key - sequence reset")
                currentIndex = 0
                
                // Play error sound
                v?.performHapticFeedback(
                    android.view.HapticFeedbackConstants.CLOCK_TICK,
                    android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        }
        
        return false
    }
    
    private suspend fun triggerEasterEgg(view: View?) {
        Log.d(TAG, "Starting Easter egg sequence...")
        
        // Step 1: Visual transition
        startTransition(view)
        
        // Step 2: Play unlock sound
        playUnlockSound()
        
        // Step 3: Show blush message (if TextView available)
        delay(500)
        showBlushMessage(view)
        
        // Step 4: Inject chibi mascot overlay
        delay(1000)
        injectChibiMascot(view)
        
        // Step 5: Enable developer mode
        delay(1500)
        onTrigger()
        
        // Step 6: Return to normal (with lingering effects)
        delay(3000)
        endTransition(view)
    }
    
    private fun startTransition(view: View?) {
        Log.d(TAG, "Starting industrial gate opening animation")
        
        view?.let {
            // Pulse animation for visual effect
            val animator = ValueAnimator.ofFloat(1f, 1.1f, 1f)
            animator.duration = 600
            animator.addUpdateListener { valueAnimator ->
                val scale = valueAnimator.animatedValue as Float
                it.scaleX = scale
                it.scaleY = scale
            }
            animator.start()
        }
    }
    
    private fun playUnlockSound() {
        Log.d(TAG, "Playing unlock sound effect")
        // In production, would use SoundPool for actual audio
        // For now, we just log it
    }
    
    private fun showBlushMessage(view: View?) {
        Log.d(TAG, "Showing blush message")
        
        // Try to find and update a TextView if available
        (view as? TextView)?.let {
            it.text = "Don't look at me like that! I'm just doing my job! (///ω///)"
            it.setTextColor(android.graphics.Color.parseColor("#FF66A8"))  // Anime pink
        }
    }
    
    private fun injectChibiMascot(view: View?) {
        Log.d(TAG, "Injecting chibi mascot overlay")
        
        // Try to find and update an ImageView if available
        (view as? ImageView)?.let {
            // In production, would load suki_mascot_chibi_blushing.png
            it.alpha = 0.8f
            it.scaleX = 0.9f
            it.scaleY = 0.9f
        }
    }
    
    private fun endTransition(view: View?) {
        Log.d(TAG, "Ending transition animation")
        
        view?.let {
            val animator = ValueAnimator.ofFloat(1.1f, 1f)
            animator.duration = 400
            animator.addUpdateListener { valueAnimator ->
                val scale = valueAnimator.animatedValue as Float
                it.scaleX = scale
                it.scaleY = scale
            }
            animator.start()
        }
    }
}
