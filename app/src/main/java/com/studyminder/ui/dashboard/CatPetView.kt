package com.studyminder.ui.dashboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import kotlin.math.abs
import kotlin.random.Random

enum class CatState { WALKING, SITTING, SLEEPING, PLAYING, REACTING }

class CatPetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // ── Views ──────────────────────────────────────────────────────────────
    private val lottieView: LottieAnimationView
    private val speechBubble: TextView
    private val zzzLabel: TextView

    // ── State ──────────────────────────────────────────────────────────────
    private var catState: CatState = CatState.WALKING
    private var walkDirection = 1f          // +1 right, -1 left
    private var parentWidth = 0
    private var isDragging = false
    private var isFalling = false

    // ── Drag / Long-press ──────────────────────────────────────────────────
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private val longPressDuration = 450L
    private val lpHandler = Handler(Looper.getMainLooper())
    private var lpRunnable: Runnable? = null
    private var isLongPressed = false

    // ── Timers ─────────────────────────────────────────────────────────────
    private val handler = Handler(Looper.getMainLooper())
    private var walkAnimator: ObjectAnimator? = null
    private var stateJob: Runnable? = null
    private var bubbleJob: Runnable? = null

    // ── Messages ───────────────────────────────────────────────────────────
    companion object {
        val GREET_MSGS   = listOf("Hello! 👋", "I miss you!", "It's been a while~", "Meow! 🐱", "Hiya! ✨")
        val DONE_MSGS    = listOf("Good job! 🎉", "Great! ⭐", "Excellent! 🏆", "You're amazing! ✨", "Keep it up! 🚀")
        val MISSED_MSGS  = listOf("You don't want me? 😿", "Cheer up! 🌟", "You got this! 💪", "Don't give up! 🌈")
        val FUN_FACTS    = listOf(
            "Cats sleep 12-16 hrs/day 😴",
            "Cats can't taste sweetness 🍬",
            "A group of cats = clowder 🐱",
            "Cats have 32 ear muscles! 👂",
            "Purring can heal bones 🦴",
            "Cats jump 6× their height! 🦘",
            "Cats have 5 toes on front paws 🐾"
        )
        val IDLE_QUIPS = listOf("*yawn*", "Studying? 📚", "I'm watching you 👀", "Pet me? 🥺", "Purrrr~")
    }

    // ── Init ───────────────────────────────────────────────────────────────
    init {
        clipChildren  = false
        clipToPadding = false

        // Lottie cat  (fills the whole view)
        lottieView = LottieAnimationView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            repeatCount  = LottieDrawable.INFINITE
            scaleType    = android.widget.ImageView.ScaleType.FIT_CENTER
        }

        // Speech bubble — sits above the cat
        speechBubble = TextView(context).apply {
            textSize  = 10f
            setTextColor(0xFF2C1A0E.toInt())
            background = buildBubbleBackground()
            setPadding(dp(10), dp(5), dp(10), dp(5))
            visibility = View.GONE
            elevation  = 8f
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                it.bottomMargin = dp(4)
                it.gravity      = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
            }
        }

        // ZZZ label (only during sleep)
        zzzLabel = TextView(context).apply {
            text      = "z z Z"
            textSize  = 8f
            setTextColor(0xFF7B8FC8.toInt())
            visibility = View.GONE
            elevation  = 7f
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                it.gravity = android.view.Gravity.TOP or android.view.Gravity.END
                it.topMargin = dp(2)
            }
        }

        addView(lottieView)
        addView(speechBubble)
        addView(zzzLabel)

        setupTouch()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildBubbleBackground(): android.graphics.drawable.Drawable {
        val gd = android.graphics.drawable.GradientDrawable()
        gd.shape           = android.graphics.drawable.GradientDrawable.RECTANGLE
        gd.cornerRadius    = dp(10).toFloat()
        gd.setColor(0xFFFFF8F0.toInt())
        gd.setStroke(dp(1), 0xFFE8D5C0.toInt())
        return gd
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.postDelayed({
            showBubble(GREET_MSGS.random())
            startWalking()
            scheduleNextState()
        }, 600)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAll()
    }

    fun setParentWidth(w: Int) { parentWidth = w }

    // ── Public API ─────────────────────────────────────────────────────────
    fun showMessage(msg: String) {
        handler.post {
            if (catState == CatState.REACTING) return@post
            catState = CatState.REACTING
            cancelWalk()
            playAnimation("cat_react.json", loop = false)
            showBubble(msg)
            handler.postDelayed({
                if (!isDragging) {
                    startWalking()
                    scheduleNextState()
                }
            }, 2800)
        }
    }

    // ── States ─────────────────────────────────────────────────────────────
    private fun startWalking() {
        if (isDragging || isFalling) return
        catState = CatState.WALKING
        zzzLabel.visibility = View.GONE
        lottieView.scaleX   = walkDirection        // flip horizontally
        playAnimation("cat_walk.json", loop = true)
        animateWalk()
    }

    private fun startSitting() {
        if (isDragging || isFalling) return
        catState = CatState.SITTING
        cancelWalk()
        zzzLabel.visibility = View.GONE
        playAnimation("cat_sit.json", loop = true)
        if (Random.nextBoolean()) showBubble(FUN_FACTS.random())
        else if (Random.nextBoolean()) showBubble(IDLE_QUIPS.random())
    }

    private fun startSleeping() {
        if (isDragging || isFalling) return
        catState = CatState.SLEEPING
        cancelWalk()
        playAnimation("cat_sleep.json", loop = true)
        // Delayed zzZ appear
        handler.postDelayed({
            if (catState == CatState.SLEEPING) {
                zzzLabel.visibility = View.VISIBLE
                zzzLabel.alpha = 0f
                zzzLabel.animate().alpha(1f).setDuration(600).start()
                // float up gently
                ObjectAnimator.ofFloat(zzzLabel, "translationY", 0f, -dp(8).toFloat()).apply {
                    duration       = 2000
                    repeatCount    = ValueAnimator.INFINITE
                    repeatMode     = ValueAnimator.REVERSE
                    interpolator   = android.view.animation.DecelerateInterpolator()
                    start()
                }
                showBubble("zzZ... 💤")
            }
        }, 1000)
    }

    private fun startPlaying() {
        if (isDragging || isFalling) return
        catState = CatState.PLAYING
        cancelWalk()
        zzzLabel.visibility = View.GONE
        playAnimation("cat_play.json", loop = false)
        showBubble("Wheee! 🎉")
        lottieView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                lottieView.removeAnimatorListener(this)
                if (catState == CatState.PLAYING && !isDragging) {
                    startWalking()
                    scheduleNextState()
                }
            }
        })
    }

    private fun scheduleNextState() {
        stateJob?.let { handler.removeCallbacks(it) }
        val delay = Random.nextLong(6000L, 14000L)
        stateJob = Runnable {
            if (!isDragging && !isFalling) {
                zzzLabel.visibility = View.GONE
                when (Random.nextInt(4)) {
                    0 -> startWalking()
                    1 -> startSitting()
                    2 -> startSleeping()
                    3 -> startPlaying()
                }
                scheduleNextState()
            }
        }
        handler.postDelayed(stateJob!!, delay)
    }

    // ── Walking physics ────────────────────────────────────────────────────
    private fun animateWalk() {
        walkAnimator?.cancel()
        val catW     = width.takeIf { it > 0 } ?: dp(72)
        val maxX     = (parentWidth - catW).toFloat().coerceAtLeast(0f)
        val targetX  = if (walkDirection > 0) maxX else 0f
        val dist     = abs(targetX - x)
        val duration = (dist / 60f * 1000L).toLong().coerceIn(1200L, 7000L)

        walkAnimator = ObjectAnimator.ofFloat(this, "x", x, targetX).apply {
            this.duration  = duration
            interpolator   = android.view.animation.LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isDragging && !isFalling && catState == CatState.WALKING) {
                        walkDirection *= -1
                        lottieView.scaleX = walkDirection
                        animateWalk()
                    }
                }
            })
            start()
        }
    }

    private fun cancelWalk() { walkAnimator?.cancel() }

    // ── Lottie helper ──────────────────────────────────────────────────────
    private fun playAnimation(assetName: String, loop: Boolean) {
        lottieView.cancelAnimation()
        lottieView.setAnimationFromAsset(assetName)
        lottieView.repeatCount = if (loop) LottieDrawable.INFINITE else 0
        lottieView.playAnimation()
    }

    // ── Speech bubble ──────────────────────────────────────────────────────
    private fun showBubble(text: String) {
        bubbleJob?.let { handler.removeCallbacks(it) }
        speechBubble.text      = text
        speechBubble.visibility = View.VISIBLE
        speechBubble.alpha      = 0f
        speechBubble.scaleX     = 0.7f
        speechBubble.scaleY     = 0.7f
        speechBubble.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(220)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()

        bubbleJob = Runnable {
            speechBubble.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f)
                .setDuration(200)
                .withEndAction { speechBubble.visibility = View.GONE }
                .start()
        }
        handler.postDelayed(bubbleJob!!, 3200)
    }

    // ── Touch / drag ──────────────────────────────────────────────────────
    private fun setupTouch() {
        setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragOffsetX = ev.rawX - x
                    dragOffsetY = ev.rawY - y
                    lpRunnable = Runnable {
                        isLongPressed = true
                        isDragging    = true
                        cancelWalk()
                        zzzLabel.visibility = View.GONE
                        // Lift animation
                        animate()
                            .scaleX(1.25f).scaleY(1.25f)
                            .translationZ(16f)
                            .setDuration(180)
                            .setInterpolator(OvershootInterpolator())
                            .start()
                        playAnimation("cat_play.json", loop = true)
                        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    }
                    lpHandler.postDelayed(lpRunnable!!, longPressDuration)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        x = ev.rawX - dragOffsetX
                        y = ev.rawY - dragOffsetY
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    lpRunnable?.let { lpHandler.removeCallbacks(it) }
                    if (isDragging) {
                        isDragging    = false
                        isLongPressed = false
                        animate().scaleX(1f).scaleY(1f).translationZ(0f).setDuration(150).start()
                        showBubble("That's fun! 😸")
                        startFalling()
                    }
                    true
                }
                else -> false
            }
        }
    }

    // ── Fall physics ──────────────────────────────────────────────────────
    private fun startFalling() {
        isFalling = true
        playAnimation("cat_walk.json", loop = true) // spinning "falling" look

        val parentView = parent as? View ?: return
        val navHeight  = 0  // calculated by MainActivity
        val targetY    = (parentView.height - height - navHeight - dp(4)).toFloat()

        // Slight random horizontal drift
        val driftX = (x + Random.nextFloat() * dp(20) - dp(10))
            .coerceIn(0f, (parentWidth - width).toFloat())

        ObjectAnimator.ofFloat(this, "x", x, driftX).apply {
            duration     = 550
            interpolator = AccelerateInterpolator(0.5f)
            start()
        }
        ObjectAnimator.ofFloat(this, "y", y, targetY).apply {
            duration     = 550
            interpolator = AccelerateInterpolator(1.8f)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Bounce on landing
                    ObjectAnimator.ofFloat(this@CatPetView, "y", targetY, targetY - dp(14).toFloat(), targetY).apply {
                        duration     = 380
                        interpolator = BounceInterpolator()
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                isFalling = false
                                handler.postDelayed({
                                    if (!isDragging) {
                                        startWalking()
                                        scheduleNextState()
                                    }
                                }, 400)
                            }
                        })
                        start()
                    }
                }
            })
            start()
        }
    }

    // ── Cleanup ────────────────────────────────────────────────────────────
    private fun cancelAll() {
        walkAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        lpHandler.removeCallbacksAndMessages(null)
        lottieView.cancelAnimation()
    }
}
