package com.jimipurple.himichat.ui.settings

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.jimipurple.himichat.BaseFragment
import com.jimipurple.himichat.NavigationActivity
import com.jimipurple.himichat.R
import com.jimipurple.himichat.ui.adapters.SettingsPageAdapter
import com.squareup.picasso.RequestCreator
import kotlinx.android.synthetic.main.fragment_settings.*
import java.io.ByteArrayOutputStream
import java.io.File


class SettingsFragment : BaseFragment() {

    private val RESULT_LOAD_IMAGE = 1
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2
    private val GET_FROM_GALLERY = 3
    private var storage: FirebaseStorage = FirebaseStorage.getInstance()
    private var fragmentAdapter: SettingsPageAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        val data = mapOf("id" to mAuth!!.uid!!)

        fragmentAdapter = SettingsPageAdapter(this.childFragmentManager, c!!.applicationContext)
        viewpager_main.adapter = fragmentAdapter
        SettingsTabs.setupWithViewPager(viewpager_main)
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        Log.i(tag, "onActivityResult $requestCode $resultCode $data")
//    }
}
