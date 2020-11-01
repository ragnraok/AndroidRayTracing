package com.ragnarok.raytracing.ui

import android.graphics.Point
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ragnarok.raytracing.R
import com.ragnarok.raytracing.glsl.PassVariable
import com.ragnarok.raytracing.renderer.RayTracingRenderer

class SceneRenderUI : AppCompatActivity() {

    companion object {
        const val SCENE = "key_scene"
    }

    private lateinit var surfaceView: GLSurfaceView
    private lateinit var infoTextView: TextView

    private lateinit var renderer: RayTracingRenderer

    private var renderLoopStart = false
    private val handler = Handler(Looper.getMainLooper()) {
        handleRenderMsg(it)
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_scene_render_ui)

        actionBar?.hide()
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

//        val windowSize = Point()
//        window.windowManager.defaultDisplay.getSize(windowSize)
//        PassVariable.eachPassOutputHeight = PassVariable.eachPassOutputWidth * (1.0 * windowSize.y / windowSize.x)

        surfaceView = findViewById(R.id.surfaceview)
        infoTextView = findViewById(R.id.info_tv)

        val scene = intent.getIntExtra(SCENE, 0)
        renderer = RayTracingRenderer(this, scene)
        surfaceView.setEGLContextClientVersion(3)
        surfaceView.setRenderer(renderer)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    private fun handleRenderMsg(msg: Message) {
        if (msg.what == 1000 && renderLoopStart) {
            surfaceView.requestRender()
            infoTextView.text = "Sample:${renderer.frameCount()}"
            handler.sendEmptyMessageDelayed(1000, 50)
        }
    }

    override fun onResume() {
        super.onResume()
        renderLoopStart = true
        handler.sendEmptyMessage(1000)
    }


    override fun onPause() {
        super.onPause()
        renderLoopStart = false
        handler.removeCallbacksAndMessages(null)
    }
}
