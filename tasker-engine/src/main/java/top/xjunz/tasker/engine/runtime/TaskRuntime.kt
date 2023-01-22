/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.runtime

import androidx.core.util.Pools.SimplePool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import top.xjunz.shared.ktx.casted
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.task.XTask


/**
 * The structure storing runtime information of a running [XTask].
 *
 * @author xjunz 2022/08/09
 */
class TaskRuntime private constructor() {

    private object Pool : SimplePool<TaskRuntime>(25)

    interface Observer {

        fun onStarted(victim: Applet, runtime: TaskRuntime) {}

        fun onTerminated(victim: Applet, runtime: TaskRuntime) {}

        fun onSkipped(victim: Applet, runtime: TaskRuntime) {}
    }

    companion object {

        fun XTask.obtainRuntime(
            eventScope: EventScope,
            coroutineScope: CoroutineScope,
            events: Array<out Event>
        ): TaskRuntime {
            val instance = Pool.acquire() ?: TaskRuntime()
            instance.task = this
            instance.runtimeScope = coroutineScope
            instance.target = events
            instance._events = events
            instance._eventScope = eventScope
            return instance
        }

        fun drainPool() {
            while (Pool.acquire() != null) {
                /* no-op */
            }
            Event.drainPool()
        }
    }

    val isActive get() = runtimeScope?.isActive == true

    var isSuspended = false

    lateinit var task: XTask

    private var _eventScope: EventScope? = null

    private val eventScope: EventScope get() = _eventScope!!

    private var _events: Array<out Event>? = null

    val events: Array<out Event> get() = _events!!

    private var runtimeScope: CoroutineScope? = null

    /**
     * Target is for applet to use in runtime via [TaskRuntime.getTarget].
     */
    private lateinit var target: Any

    val tracker = AppletIndexer()

    var observer: Observer? = null

    lateinit var hitEvent: Event

    private var _result: AppletResult? = null

    val result: AppletResult get() = _result!!

    fun ensureActive() {
        runtimeScope?.ensureActive()
    }

    fun halt() {
        runtimeScope?.cancel()
    }

    /**
     * Get or put a global variable if absent. The variable can be shared across tasks. More specific,
     * within an [EventScope].
     */
    fun <V : Any> getGlobal(key: Long, initializer: (() -> V)? = null): V {
        if (initializer == null) {
            return eventScope.registry.getValue(key).casted()
        }
        return eventScope.registry.getOrPut(key, initializer).casted()
    }

    /**
     * Values are keyed by applets' remarks.
     */
    private val referents = mutableMapOf<String, Any?>()

    lateinit var currentApplet: Applet

    lateinit var currentFlow: Flow

    /**
     * Whether the applying of current applet is successful.
     */
    var isSuccessful = true

    /**
     * Get all arguments from registered results, which were registered by [registerResult].
     */
    fun getArguments(applet: Applet): Array<Any?> {
        return if (applet.references.isEmpty()) {
            emptyArray()
        } else {
            Array(applet.references.size) {
                getResultByRefid(applet.references[it])
            }
        }
    }

    /**
     * Register all results of an applet.
     */
    fun registerResult(applet: Applet, result: AppletResult) {
        _result = result
        if (result.isSuccessful && result.values != null) {
            registerReferents(applet, *result.values!!)
        } else {
            eventScope.registerFailure(applet, result.expected, result.actual)
        }
    }

    fun registerReferents(applet: Applet, vararg values: Any?) {
        applet.refids.forEach { (index, refid) ->
            referents[refid] = values[index]
        }
    }

    fun getFailure(applet: Applet): Pair<Any?, Any?>? {
        return eventScope.failures[applet]
    }

    fun getResultByRefid(refid: String?): Any? {
        if (refid == null) return null
        return referents[refid]
    }

    fun setTarget(any: Any) {
        target = any
    }

    fun getRawTarget(): Any {
        return target
    }

    fun <T> getTarget(): T {
        return target.casted()
    }

    fun recycle() {
        _eventScope = null
        _events = null
        runtimeScope = null
        referents.clear()
        tracker.reset()
        isSuspended = false
        observer = null
        _result = null
        Pool.release(this)
    }

    override fun toString(): String {
        return "TaskRuntime@${hashCode().toString(16)}(${task.title})"
    }
}