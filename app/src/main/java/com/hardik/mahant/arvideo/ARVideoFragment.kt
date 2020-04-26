package com.hardik.mahant.arvideo

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.ux.ArFragment
import com.hardik.mahant.arvideo.common.helper.SnackbarHelper
import java.io.IOException


class ARVideoFragment: ArFragment() {

    val markerImageName = "for_bigger_blazes.jpg"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        // Turn off the plane discovery since we're only looking for images
        planeDiscoveryController.hide();
        planeDiscoveryController.setInstructionView(null);
        arSceneView.planeRenderer.isEnabled = false;
        return view
    }

    override fun getSessionConfiguration(session: Session?): Config {
        val config = super.getSessionConfiguration(session)
        //setting camera on auto focus mode
        config.focusMode = Config.FocusMode.AUTO

        if (!setupAugmentedImageDatabase(config, session)) {
            SnackbarHelper.getInstance()
                .showError(activity, "Could not setup augmented image database");
        }

        return config
    }

    private fun setupAugmentedImageDatabase(config: Config, session: Session?): Boolean{
        val assetManager =
            if (context != null) context!!.assets else null
        if (assetManager == null) {
            Log.e(TAG, "Context is null, cannot intitialize image database.")
            return false
        }

        val augmentedImageBitmap: Bitmap = loadAugmentedImageBitmap(assetManager) ?: return false

        val augmentedImageDatabase = AugmentedImageDatabase(session)
        augmentedImageDatabase.addImage(markerImageName, augmentedImageBitmap)

        config.augmentedImageDatabase = augmentedImageDatabase;
        return true;
    }

    private fun loadAugmentedImageBitmap(assetManager: AssetManager): Bitmap? {
        try {
            assetManager.open(markerImageName)
                .use { inputStream -> return BitmapFactory.decodeStream(inputStream) }
        } catch (e: IOException) {
            Log.e(TAG, "IO exception loading augmented image bitmap.", e)
        }
        return null
    }

    companion object{
        const val TAG = "ARVideoFragment"
    }

}