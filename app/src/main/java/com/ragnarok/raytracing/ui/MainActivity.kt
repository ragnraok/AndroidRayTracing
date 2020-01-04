package com.ragnarok.raytracing.ui

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.ragnarok.raytracing.R
import com.ragnarok.raytracing.renderer.RayTracingRenderer

class MainActivity : AppCompatActivity() {

    private lateinit var surfaceView: GLSurfaceView

    private val renderer = RayTracingRenderer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        actionBar?.hide()
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        surfaceView = findViewById(R.id.surfaceview)

        surfaceView.setEGLContextClientVersion(3)
        surfaceView.setRenderer(renderer)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

}
