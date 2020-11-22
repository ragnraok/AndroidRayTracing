package com.ragnarok.raytracing.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ragnarok.raytracing.R
import com.ragnarok.raytracing.renderer.Scenes
import com.ragnarok.raytracing.renderer.bvh.BVH
import de.javagl.obj.ObjReader
import de.javagl.obj.ObjUtils
import kotlin.concurrent.thread

class MainUI : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainui)

        val intent = Intent()
        intent.setClass(this, SceneRenderUI::class.java)

        findViewById<Button>(R.id.cornell_box).setOnClickListener {
            intent.putExtra(SceneRenderUI.SCENE, Scenes.CORNELL_BOX)
            startActivity(intent)
        }

        findViewById<Button>(R.id.pbr_sphere).setOnClickListener {
            intent.putExtra(SceneRenderUI.SCENE, Scenes.PBR_SPHERE)
            startActivity(intent)
        }

        findViewById<Button>(R.id.pbr_sphere_dof).setOnClickListener {
            intent.putExtra(SceneRenderUI.SCENE, Scenes.PBR_SPHERE_DOF)
            startActivity(intent)
        }

        findViewById<Button>(R.id.glass).setOnClickListener {
            intent.putExtra(SceneRenderUI.SCENE, Scenes.GLASS)
            startActivity(intent)
        }

        findViewById<Button>(R.id.texture_sphere).setOnClickListener {
            intent.putExtra(SceneRenderUI.SCENE, Scenes.TEXTURE_SPHERE)
            startActivity(intent)
        }

        findViewById<Button>(R.id.model).setOnClickListener {
            val modelIntent = Intent()
            modelIntent.setClass(this, ModelRenderUI::class.java)
            startActivity(modelIntent)
        }
    }

}
