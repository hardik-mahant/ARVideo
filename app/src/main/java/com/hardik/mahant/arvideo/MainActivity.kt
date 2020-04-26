package com.hardik.mahant.arvideo

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode
import com.hardik.mahant.arvideo.common.helper.SnackbarHelper
import java.util.function.Consumer


class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ARVideoFragment
    private lateinit var imgFitToScan: ImageView

    private lateinit var externalTexture: ExternalTexture
    private lateinit var videoModelRenderable: ModelRenderable
    private var mediaPlayer: MediaPlayer? = null

    private var detectedImageName: String = ""
    private val videoModelName = "chroma_key_video.sfb"
    private val chromeKeyColor = Color(0.01843f, 1f, 0.098f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ARVideoFragment
        imgFitToScan = findViewById(R.id.imgFitToScan)
        externalTexture = ExternalTexture()

        initMediaPlayer()

        ModelRenderable
            .builder()
            .setSource(this@MainActivity, Uri.parse(videoModelName))
            .build()
            .thenAccept(Consumer { modelRenderable: ModelRenderable ->
                modelRenderable.material.setExternalTexture(
                    "videoTexture",
                    externalTexture
                )
                modelRenderable.material.setFloat4(
                    "keyColor",
                    chromeKeyColor
                )
                videoModelRenderable = modelRenderable
            }).exceptionally {

                return@exceptionally null
            }

        arFragment.arSceneView.scene.addOnUpdateListener(this::onARFrameUpdate)
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this@MainActivity, R.raw.for_bigger_blazes)
        mediaPlayer?.setSurface(externalTexture.surface)
        mediaPlayer?.isLooping = true
    }

    override fun onResume() {
        super.onResume()
        if (detectedImageName.isEmpty()) {
            imgFitToScan.visibility = View.VISIBLE
        } else {
            if (mediaPlayer != null) {
                mediaPlayer?.start()
                Log.i("MEDIAPLAYER", "STARTING")
            } else {
                Log.i("MEDIAPLAYER", "NULL")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer != null) {
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer?.pause()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer?.reset()
            mediaPlayer = null
        }
    }

    private fun onARFrameUpdate(frameTime: FrameTime) {
        val frame = arFragment.arSceneView.arFrame ?: return

        val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
        augmentedImages.forEach { augmentedImage ->

            when (augmentedImage.trackingState) {
                TrackingState.PAUSED -> {
                    SnackbarHelper.getInstance()
                        .showMessage(this, getString(R.string.msg_marker_img_detected))
                }
                TrackingState.TRACKING -> {
                    imgFitToScan.visibility = View.GONE

                    if (detectedImageName.isEmpty()) {
                        playVideoOnMarker(
                            augmentedImage
                        )
                        detectedImageName = augmentedImage.name
                    }
                }
                TrackingState.STOPPED -> {
                    detectedImageName = ""
                }
            }
        }
    }

    private fun playVideoOnMarker(augmentedImage: AugmentedImage) {
        val anchor = augmentedImage.createAnchor(augmentedImage.centerPose)
        val extentX = augmentedImage.extentX
        val extentZ = augmentedImage.extentZ

        mediaPlayer?.start()
        val anchorNode = AnchorNode(anchor)
        val node = Node()
        node.setParent(anchorNode)
        node.localScale = Vector3(extentX, extentX * 0.75f, extentZ)
        node.localRotation = Quaternion.axisAngle(Vector3(1f, 0f, 0f), -90f)
        node.localPosition = Vector3(node.localPosition.x, node.localPosition.y, extentZ / 2)
//        val node = TransformableNode(arFragment.transformationSystem)
//        node.setParent(anchorNode)
//        node.localScale = Vector3(extentX, extentX, extentZ)
//        node.localPosition = Vector3()
//        node.localRotation = Quaternion.axisAngle(Vector3(1f, 0f, 0f), 90f)
//        node.disableTouchInputs()
        externalTexture.surfaceTexture.setOnFrameAvailableListener {
            node.renderable = videoModelRenderable
            externalTexture.surfaceTexture.setOnFrameAvailableListener(null)
        }
        arFragment.arSceneView.scene.addChild(anchorNode)
    }

    /*private fun TransformableNode.disableTouchInputs(){
        this.rotationController.isEnabled = false
        this.scaleController.isEnabled = false
        this.translationController.isEnabled = false
    }*/
}
