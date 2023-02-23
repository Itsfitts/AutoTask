/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.ui.task.selector

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import top.xjunz.tasker.R
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.ControlFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.ScopeFlow
import top.xjunz.tasker.engine.applet.util.controlFlow
import top.xjunz.tasker.engine.applet.util.isContainer
import top.xjunz.tasker.ktx.eq
import top.xjunz.tasker.ktx.format
import top.xjunz.tasker.ktx.toast
import top.xjunz.tasker.task.applet.flow.PhantomFlow
import top.xjunz.tasker.task.applet.option.AppletOption
import top.xjunz.tasker.task.applet.option.AppletOptionFactory
import top.xjunz.tasker.ui.task.editor.FlowViewModel
import java.util.*

/**
 * @author xjunz 2022/10/22
 */
class AppletSelectorViewModel(states: SavedStateHandle) : FlowViewModel(states) {

    private val factory = AppletOptionFactory

    var animateItems = true

    var title: CharSequence? = null

    var isScoped = false

    lateinit var onCompletion: (List<Applet>) -> Unit

    lateinit var registryOptions: Array<AppletOption>

    val selectedFlowRegistry: LiveData<Int> = MutableLiveData()

    val showClearConfirmation = MutableLiveData<Boolean>()

    val options = mutableListOf<AppletOption>()

    val onAppletAdded = MutableLiveData<Int>()

    val requestAppendOption = MutableLiveData<Pair<AppletOption, Int>>()

    /**
     * The nearest non-container parent of this applet, may be itself.
     */
    private val Applet.scopeFlow: Flow
        get() {
            if (this is Flow && !isContainer) return this
            return requireParent().scopeFlow
        }

    fun setScope(origin: Flow) {
        val scope = origin.scopeFlow
        // Find its control flow, we need its control flow's option title to be shown
        val controlFlow = if (scope is ControlFlow) scope else scope.controlFlow
        checkNotNull(controlFlow) {
            "ControlFlow not found!"
        }
        title = factory.requireOption(controlFlow).rawTitle
        if (scope is ScopeFlow<*>) {
            isScoped = true
            registryOptions = arrayOf(factory.requireRegistryOption(scope.appletId))
        } else {
            isScoped = false
            registryOptions = factory.flowRegistry.getRegistryOptions(controlFlow)
        }
    }

    fun selectFlowRegistry(index: Int) {
        if (selectedFlowRegistry eq index) return
        options.clear()
        options.addAll(
            factory.requireRegistryById(registryOptions[index].appletId).categorizedOptions
        )
        (selectedFlowRegistry as MutableLiveData).value = index
    }

    fun appendApplet(applet: Applet): Boolean {
        val flowOption = factory.requireRegistryOption(applet.registryId)
        val last = flow.lastOrNull()
        if (last !is Flow || (flowOption.appletId != last.appletId)) {
            val newFlow = flowOption.yield() as Flow
            if (newFlow !is PhantomFlow) {
                if (!flow.addSafely(newFlow)) return false
                if (!newFlow.addSafely(applet)) return false
            } else {
                if (!flow.addSafely(applet)) return false
            }
        } else {
            if (!last.addSafely(applet)) return false
            // Divider changed
            onAppletChanged.value = last.getOrNull(last.lastIndex - 1)
        }
        notifyFlowChanged()
        return true
    }

    fun acceptApplets(applets: List<Applet>) {
        val flowOption = factory.requireRegistryOption(applets.first().registryId)

        if (!flow.addSafely(flowOption.yield() as Flow)) return
        var count = 0
        applets.forEach {
            if (!appendApplet(it)) return@forEach
            count++
        }
        toast(R.string.options_from_inspector_added.format(count))
        notifyFlowChanged()
    }

    fun clearAllCandidates() {
        flow.clear()
        notifyFlowChanged()
    }

    fun complete() {
        if (flow.isEmpty()) {
            toast(R.string.no_rule_added)
        } else {
            onCompletion.invoke(mergeCandidates())
        }
    }

    private fun mergeCandidates(): List<Applet> {
        val ret = mutableListOf<Applet>()
        flow.forEach {
            if (isScoped && it is Flow) ret.addAll(it) else ret.add(it)
        }
        return ret
    }

    override fun flatmapFlow(): List<Applet> {
        val ret = ArrayList<Applet>()
        flow.forEachIndexed { index, child ->
            child.index = index
            child.parent = this.flow
            ret.add(child)
            if (child is Flow && !isCollapsed(child))
                child.forEachIndexed { i, applet ->
                    applet.index = i
                    applet.parent = child
                    ret.add(applet)
                }
        }
        ret.trimToSize()
        return ret
    }

    private fun Flow.addSafely(applet: Applet): Boolean {
        return addAllSafely(Collections.singleton(applet))
    }

    private fun Flow.addAllSafely(applets: Collection<Applet>): Boolean {
        if (size + applets.size <= maxSize) {
            addAll(applets)
            return true
        }
        toast(R.string.error_over_max_applet_size)
        return false
    }
}