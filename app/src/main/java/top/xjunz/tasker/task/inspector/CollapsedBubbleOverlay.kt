package top.xjunz.tasker.task.inspector

import android.view.WindowManager
import androidx.core.view.isVisible
import top.xjunz.tasker.databinding.OverlayBubbleCollapsedBinding
import top.xjunz.tasker.ktx.doWhenEnd
import top.xjunz.tasker.ktx.observe
import top.xjunz.tasker.ktx.toggle
import top.xjunz.tasker.service.A11yAutomatorService
import top.xjunz.tasker.ui.widget.FloatingDraggableLayout
import top.xjunz.tasker.util.Icons

/**
 * @author xjunz 2022/10/17
 */
class CollapsedBubbleOverlay(
    inspector: FloatingInspector, private val trashBinOverlay: TrashBinOverlay,
) : BaseOverlay<OverlayBubbleCollapsedBinding>(inspector) {

    private fun desaturate() {
        rootView.background.colorFilter = Icons.desaturatedColorFilter
        binding.ibCenter.colorFilter = Icons.desaturatedColorFilter
    }

    private fun saturate() {
        rootView.background.colorFilter = null
        binding.ibCenter.colorFilter = null
    }

    override fun modifyLayoutParams(base: WindowManager.LayoutParams) {
        super.modifyLayoutParams(base)
        base.x = vm.bubbleX
        base.y = vm.bubbleY
    }

    override fun onOverlayInflated() {
        super.onOverlayInflated()
        binding.bubbleWrapper.onDragListener = { state, offsetX, offsetY ->
            when (state) {
                FloatingDraggableLayout.STATE_DRAG_STARTED -> {
                    trashBinOverlay.layout()
                    trashBinOverlay.fadeIn()
                }
                FloatingDraggableLayout.STATE_DRAGGING -> {
                    offsetViewInWindow(offsetX.toInt(), offsetY.toInt())
                    vm.bubbleX = layoutParams.x
                    vm.bubbleY = layoutParams.y
                    if (trashBinOverlay.isInside(this)) {
                        desaturate()
                    } else {
                        saturate()
                    }
                }
                FloatingDraggableLayout.STATE_DRAG_ENDED -> {
                    if (trashBinOverlay.isInside(this)) {
                        rootView.animate().alpha(0F).doWhenEnd {
                            A11yAutomatorService.require().destroyFloatingInspector()
                        }.start()
                    }
                    trashBinOverlay.fadeOut()
                }
            }
        }
        binding.ibCenter.setOnClickListener {
            vm.isCollapsed.toggle()
        }
        inspector.observe(vm.isCollapsed) {
            if (it) {
                layoutParams.x = vm.bubbleX
                layoutParams.y = vm.bubbleY
                if (rootView.isAttachedToWindow) windowManager.updateViewLayout(
                    rootView, layoutParams
                )
            }
            rootView.isVisible = it
        }
    }

    override fun onDismiss() {
        super.onDismiss()
        saturate()
        layoutParams.x = 0
        layoutParams.y = 0
        rootView.isVisible = false
    }

}