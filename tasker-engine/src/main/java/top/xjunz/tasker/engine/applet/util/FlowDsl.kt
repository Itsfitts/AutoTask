/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.applet.util

import top.xjunz.tasker.engine.applet.action.emptyArgAction
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.Do
import top.xjunz.tasker.engine.applet.base.DslFlow
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.If
import top.xjunz.tasker.engine.applet.base.RootFlow
import top.xjunz.tasker.engine.applet.criterion.DslCriterion
import top.xjunz.tasker.engine.runtime.TaskRuntime

/**
 * @author xjunz 2022/08/13
 */
@DslMarker
internal annotation class FlowDsl

@FlowDsl
internal fun DslRootFlow(initialTarget: Any? = null, init: RootFlow.() -> Unit): RootFlow {
    return DslFlow(initialTarget).apply(init)
}

@FlowDsl
internal fun Flow.If(block: If.() -> Unit) {
    add(If().apply(block))
}

@FlowDsl
internal fun Flow.Then(block: Do.() -> Unit) {
    add(Do().apply(block))
}

@FlowDsl
internal fun Do.Action(block: () -> Boolean) {
    add(emptyArgAction {
        block()
    })
}

@FlowDsl
internal fun Flow.DslFlow(block: Flow.() -> Unit) {
    add(Flow().apply(block))
}

@FlowDsl
internal fun Flow.EmptyFlow() {
    add(Flow())
}

@FlowDsl
internal fun Flow.DslApplet(id: Int = Applet.NO_ID, block: (DSLApplet.() -> Unit) = {}) {
    add(DSLApplet(id).apply(block))
}

@FlowDsl
internal fun DSLApplet.ReferTo(referent: String) {
    addReference(referent)
}

@FlowDsl
internal fun DSLApplet.WithReferent(name: String) {
    withReferent(name)
}

private fun Applet.addReference(referent: String) {
    if (references.isEmpty()) {
        references = mutableMapOf()
    }
    (references as MutableMap)[references.size] = referent
}

private fun Applet.withReferent(referent: String) {
    if (referents.isEmpty()) {
        referents = mutableMapOf()
    }
    (referents as MutableMap)[referents.size] = referent
}

@FlowDsl
internal fun <T : Any, V : Any> Flow.DslCriterion(block: DslCriterion<T, V>.() -> Unit) {
    add(DslCriterion<T, V>().apply(block))
}

@FlowDsl
internal fun <T : Any> Flow.UnaryCriterion(block: DslCriterion<T, T>.() -> Unit) {
    DslCriterion(block)
}

@FlowDsl
internal fun <T : Any, V : Any> DslCriterion<T, V>.Matcher(block: (T, V) -> Boolean) {
    matcher = block
}

internal class DSLApplet(id: Int = NO_ID) : Applet() {

    init {
        this.id = id
    }

    override suspend fun apply(runtime: TaskRuntime): AppletResult {
        return AppletResult.EMPTY_SUCCESS
    }

}
