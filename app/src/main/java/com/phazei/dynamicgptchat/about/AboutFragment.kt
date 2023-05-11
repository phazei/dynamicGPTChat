package com.phazei.dynamicgptchat.about

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.phazei.dynamicgptchat.R
import com.phazei.utils.IdenticonFlorash
import com.phazei.utils.Solacon

class AboutFragment : Fragment() {
    private val florashGenerator = IdenticonFlorash()
    private val solaconGenerator = Solacon()
    private lateinit var florash: ImageView
    private lateinit var solacon: ImageView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        florash = view.findViewById(R.id.florash)
        solacon = view.findViewById(R.id.solacon)

        val aboutText: TextView = view.findViewById(R.id.about_text)
        val htmlText = """
<h1>About DynamicGPTChat</h1>
<p>Welcome to <a href="https://github.com/phazei/dynamicGPTChat/">DynamicGPTChat</a>, a mobile app built for those who want a more enjoyable and tailored experience using ChatGPT. This app is designed to be open source, allowing anyone to build upon it and use it for free as long as they have an API key and comply with the terms of the <a href="https://github.com/phazei/dynamicGPTChat/blob/master/LICENSE.txt">DynamicGPTChat Software License</a>.</p>
<br /><br />
<h2>Privacy</h2>
<p>The app does not have a backend server, which means your API key is only sent directly to the OpenAI API and is never passed on to anyone else.</p>
<br /><br />
<h2>Attributions</h2>
<p>DynamicGPTChat uses the following open-source libraries:</p>
<ul>
  <li><a href="https://github.com/noties/Markwon">Markwon</a> (Apache 2.0)</li>
  <li><a href="https://github.com/aallam/openai-kotlin">openai-kotlin</a> (Apache 2.0)</li>
  <li><a href="https://github.com/tomergoldst/tooltips">Tooltips</a> (Apache 2.0)</li>
  <li><a href="https://github.com/pilgr/Paper">Paper</a> (Apache 2.0)</li>
  <li><a href="https://github.com/square/moshi">Moshi</a> (Apache 2.0)</li>
  <li><a href="Library URL">Library Name</a> (MIT)</li>
</ul>
<br /><br />--------
<p style="margin-bottom:500px">We're grateful to the developers of these libraries for their incredible work and contributions to the open-source community.</p>
<br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br />
<br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br />
<br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br />
"""
        aboutText.text = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_COMPACT)
        aboutText.movementMethod = LinkMovementMethod.getInstance()


        /**
         * Unnecessary pazzazz
         */
        val scroll: ScrollView = view.findViewById(R.id.about_scroll)
        val secret: ConstraintLayout = view.findViewById(R.id.secret_buttons)

        /*
        var originalHeight: Int = 0
        lifecycleScope.launch {
            delay(10)
            // Store the original height of the ScrollView
            originalHeight = scroll.measuredHeight
        }
        val maxHeightReduction = 500
        // */
        val path = Path().apply {
            moveTo(0f, 0f)
            // cubicTo(0.0f, 0.0f, 0.8f, 0.01f, 1f, 1f)
            lineTo(0.6f, 0.06f)
            lineTo(0.8f, 1f)
            lineTo(1f, 1f)
        }
        val interpolator = PathInterpolator(path)
        scroll.setOnScrollChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int ->
            // Shrink height of ScrollView as it scrolls down to reveal hidden panel behind it
            val maxScrollPosition = scroll.getChildAt(0).height - scroll.height
            val currentScrollPosition = scroll.scrollY

            // Calculate the percentage of how close the ScrollView is to the bottom
            val scrollPercentage = currentScrollPosition.toFloat() / maxScrollPosition
            val adjustedPercentage = interpolator.getInterpolation(scrollPercentage)

            // Calculate the new height based on the scroll percentage and maxHeightReductionPx
            // val newHeight = originalHeight - (adjustedPercentage * maxHeightReduction).toInt()
            // scroll.layoutParams.height = newHeight
            // scroll.requestLayout()

            // */
            secret.alpha = adjustedPercentage
            secret.visibility = if (adjustedPercentage > 0.1) { View.VISIBLE } else { View.GONE }
            // scroll.requestLayout()
        }
        florash.setOnClickListener {
            setFlorash()
            florash.imageTintList = null
            florash.setOnTouchListener(createMotionTouchListener(secret) { view ->
                setFlorash()
            })
            florash.setOnClickListener(null)
        }
        solacon.setOnClickListener {
            setSolacon()
            solacon.imageTintList = null
            solacon.setOnTouchListener(createMotionTouchListener(secret) { view ->
                setSolacon()
            })
            solacon.setOnClickListener(null)
        }
    }

    private fun setFlorash() {
        // the longer the hash, the more shapes
        val length = (24..128).random()
        val florashHash = (1..length).joinToString("") { listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F').random().toString() }
        val size = 512 // desired size of the identicon in pixels
        val florashBitmap = florashGenerator.generateBitmap(florashHash, size)

        florash.setImageBitmap(florashBitmap)
    }

    private fun setSolacon() {
        val solaconHash = java.util.UUID.randomUUID().toString()
        val size = 512
        val solaconBitmap = solaconGenerator.generateBitmap(solaconHash, size)

        solacon.setImageBitmap(solaconBitmap)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createMotionTouchListener(parent: View, onMove: (View) -> Unit): View.OnTouchListener {
        val velocityTracker = VelocityTracker.obtain()
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            duration = 16 // Update every 16 milliseconds
        }
        var updateCount = 0
        return View.OnTouchListener { view, motionEvent ->
            val parentWidth = parent.width - view.width
            val parentHeight = parent.height - view.height

            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    velocityTracker.clear()
                    animator.cancel()
                    view.tag = floatArrayOf(motionEvent.rawX, motionEvent.rawY, view.x, view.y)
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker.addMovement(motionEvent)
                    val initialTouchData = view.tag as FloatArray
                    val deltaX = motionEvent.rawX - initialTouchData[0]
                    val deltaY = motionEvent.rawY - initialTouchData[1]
                    view.x = initialTouchData[2] + deltaX
                    view.y = initialTouchData[3] + deltaY
                    onMove(view)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    velocityTracker.computeCurrentVelocity(1000)
                    val vx = velocityTracker.xVelocity
                    val vy = velocityTracker.yVelocity

                    val physicsModel = PhysicsModel(
                        PointF(view.x, view.y),
                        PointF(vx / 1000, vy / 1000),
                        RectF(0f, 0f, parentWidth.toFloat(), parentHeight.toFloat())
                    )

                    animator.removeAllUpdateListeners()
                    animator.addUpdateListener {
                        physicsModel.update(16f)
                        view.x = physicsModel.position.x
                        view.y = physicsModel.position.y

                        updateCount++
                        if (updateCount >= 100) {
                            onMove(view)
                            updateCount = 0
                        }
                    }
                    animator.start()
                }
            }
            true
        }
    }



    private fun Int.dpToPx(): Int {
        val resources = requireContext().resources
        val displayMetrics = resources.displayMetrics
        return (this * (displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}

data class PhysicsModel(
    var position: PointF,
    var velocity: PointF,
    var bounds: RectF
) {
    fun update(deltaTime: Float) {
        position.x += velocity.x * deltaTime
        position.y += velocity.y * deltaTime

        // Constrain position within the bounds and handle collisions
        if (position.x < bounds.left) {
            position.x = bounds.left
            velocity.x = -velocity.x
        } else if (position.x > bounds.right) {
            position.x = bounds.right
            velocity.x = -velocity.x
        }
        if (position.y < bounds.top) {
            position.y = bounds.top
            velocity.y = -velocity.y
        } else if (position.y > bounds.bottom) {
            position.y = bounds.bottom
            velocity.y = -velocity.y
        }
    }
}
