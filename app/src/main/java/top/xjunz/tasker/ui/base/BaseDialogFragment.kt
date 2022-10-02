/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.base

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.viewbinding.ViewBinding
import top.xjunz.shared.ktx.unsafeCast
import top.xjunz.tasker.R
import top.xjunz.tasker.ktx.createMaterialShapeDrawable
import top.xjunz.tasker.ktx.dpFloat
import top.xjunz.tasker.util.ReflectionUtil.superClassFirstParameterizedType

/**
 * @author xjunz 2022/04/20
 */
open class BaseDialogFragment<T : ViewBinding> : DialogFragment(),
    HasDefaultViewModelProviderFactory {

    protected lateinit var binding: T

    open val bindingRequiredSuperClassDepth = 1

    open val isFullScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            if (isFullScreen) STYLE_NO_FRAME else STYLE_NORMAL,
            if (isFullScreen) R.style.Base_FragmentDialog else R.style.Base_FragmentDialog_Min
        )
    }

    protected open fun onBackPressed(): Boolean {
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        var superClass: Class<*> = javaClass
        for (i in 1 until bindingRequiredSuperClassDepth) {
            superClass = superClass.superclass
        }
        binding = superClass.superClassFirstParameterizedType().getDeclaredMethod(
            "inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java
        ).invoke(null, layoutInflater, container, false)!!.unsafeCast()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val window = dialog!!.window!!
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (!isFullScreen) {
            val decorView = window.peekDecorView()
            // Set the background as a [MaterialAlertDialog]'s
            decorView.background = requireContext().createMaterialShapeDrawable(
                fillColorRes = com.google.android.material.R.attr.colorSurface,
                elevation = decorView.elevation, cornerSize = 24.dpFloat
            )
        }
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                return@setOnKeyListener onBackPressed()
            }
            return@setOnKeyListener false
        }
    }

    override fun getDefaultViewModelProviderFactory() = InnerViewModelFactory
}