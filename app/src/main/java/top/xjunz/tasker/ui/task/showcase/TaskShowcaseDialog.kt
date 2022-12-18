package top.xjunz.tasker.ui.task.showcase

import android.os.Bundle
import android.view.View
import androidx.core.view.updatePadding
import top.xjunz.tasker.databinding.DialogTaskShowcaseBinding
import top.xjunz.tasker.ktx.applySystemInsets
import top.xjunz.tasker.ui.base.BaseDialogFragment
import top.xjunz.tasker.util.AntiMonkeyUtil.setAntiMoneyClickListener

/**
 * @author xjunz 2022/07/30
 */
class TaskShowcaseDialog : BaseDialogFragment<DialogTaskShowcaseBinding>() {

    override val isFullScreen = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBar.applySystemInsets { v, insets ->
            v.updatePadding(top = insets.top)
        }
        binding.ibCreateTask.setAntiMoneyClickListener {

        }
    }

}