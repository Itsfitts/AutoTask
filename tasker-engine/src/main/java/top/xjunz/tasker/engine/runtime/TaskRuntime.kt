/*
 * Copyright (c) 2022 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.runtime

import android.util.ArrayMap
import androidx.core.util.Pools.SynchronizedPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import top.xjunz.shared.ktx.casted
import top.xjunz.shared.trace.logcat
import top.xjunz.shared.utils.illegalArgument
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.AppletResult
import top.xjunz.tasker.engine.applet.base.Flow
import top.xjunz.tasker.engine.applet.base.WaitFor
import top.xjunz.tasker.engine.task.XTask
import java.util.zip.CRC32

/**
 * The structure storing runtime information of a running [XTask].
 *
 * @author xjunz 2022/08/09
 */
class TaskRuntime private constructor() {

    private object Pool : SynchronizedPool<TaskRuntime>(10)

    private class IndexedReferent(val which: Int, private val referent: Any?) {

        fun getReferentValue(): Any? {
            return if (referent is Referent) referent.getReferredValue(which) else referent
        }
    }

    interface Observer {

        fun onAppletStarted(victim: Applet, runtime: TaskRuntime) {}

        fun onAppletTerminated(victim: Applet, runtime: TaskRuntime) {}

        fun onAppletSkipped(victim: Applet, runtime: TaskRuntime) {}
    }

    companion object {

        const val GLOBAL_SCOPE_TASK = 1
        const val GLOBAL_SCOPE_EVENT = 2

        fun CoroutineScope.obtainTaskRuntime(task: XTask): TaskRuntime {
            val instance = Pool.acquire() ?: TaskRuntime()
            instance.task = task
            instance.runtimeScope = this
            return instance
        }

        fun CoroutineScope.obtainTaskRuntime(
            task: XTask, registry: ValueRegistry, events: Array<Event>
        ): TaskRuntime {
            val instance = obtainTaskRuntime(task)
            instance.target = events
            instance._events = events
            instance.eventValueRegistry = registry
            return instance
        }

        fun drainPool() {
            while (Pool.acquire() != null) {
                /* no-op */
            }
        }
    }

    val tracker = AppletIndexer()

    val isActive get() = runtimeScope?.isActive == true

    val result: AppletResult get() = _result!!

    val events: Array<out Event> get() = _events!!

    /**
     * Identify all arguments applied to the task runtime. This is helpful for judging whether
     * runtimes are identical when hitting the same applet.
     */
    val fingerprint: Long get() = fingerprintCrc.value

    var isSuspending = false

    var observer: Observer? = null

    /**
     * Whether the applying of current applet is successful.
     */
    var isSuccessful = true

    /**
     * Whether the applying of current condition [If] successful, can be null if no [If] is ever hit.
     */
    var ifSuccessful: Boolean? = null

    /**
     * The current waiting [WaitFor], whose [WaitFor.remind] will be called when new events arrive.
     *
     * @see onNewEvents
     */
    var waitingFor: WaitFor? = null

    lateinit var currentApplet: Applet

    lateinit var currentFlow: Flow

    private val fingerprintCrc = CRC32()

    private val referents = ArrayMap<String, IndexedReferent>()

    private var _events: Array<Event>? = null

    private var _result: AppletResult? = null

    private var eventValueRegistry: ValueRegistry? = null

    @get:Synchronized
    @set:Synchronized
    private var runtimeScope: CoroutineScope? = null

    /**
     * Target is for applets in a specific flow to use in runtime.
     */
    private var target: Any? = null

    private lateinit var task: XTask

    fun updateFingerprint(any: Any?) {
        logcat("update fingerprint: $any")
        fingerprintCrc.update(any.hashCode())
    }

    fun ensureActive() {
        runtimeScope?.ensureActive()
    }

    fun halt() {
        runtimeScope?.cancel()
    }

    /**
     * Get or put a global variable if absent. The variable is stored as per the [scope]. More specific,
     * within an [ValueRegistry].
     */
    fun <V : Any> getScopedValue(scope: Int, key: Any, initializer: () -> V): V {
        val registry = when (scope) {
            GLOBAL_SCOPE_EVENT -> eventValueRegistry
            GLOBAL_SCOPE_TASK -> task
            else -> illegalArgument("scope", scope)
        } // fallback to task registry if there is no event value registry
            ?: task
        return if (key is ValueRegistry.WeakKey) {
            registry.getWeakValue(key, initializer)
        } else {
            registry.getValue(key, initializer)
        }
    }

    fun getReferentOf(applet: Applet, which: Int): Any? {
        val name = applet.references[which]
        if (name != null) {
            return getReferentByName(name)
        }
        return null
    }

    fun getAllReferentOf(applet: Applet): Array<Any?> {
        return if (applet.references.isEmpty()) {
            emptyArray()
        } else {
            Array(applet.references.size) {
                getReferentByName(applet.references[it])
            }
        }
    }

    fun registerReferent(referent: Any?) {
        registerReferentFor(currentApplet, referent)
    }

    private fun registerReferentFor(applet: Applet, referent: Any?) {
        applet.referents.forEach { (which, name) ->
            referents[name] = IndexedReferent(which, referent)
        }
    }

    fun registerResult(applet: Applet, result: AppletResult) {
        _result = result
        if (result.isSuccessful && result.returned != null) {
            registerReferentFor(applet, result.returned!!)
        }
    }

    private fun getReferentByName(name: String?): Any? {
        return referents[name]?.getReferentValue()
    }

    fun setTarget(any: Any?) {
        target = any
    }

    fun getRawTarget(): Any? {
        return target
    }

    fun <T> getTarget() = requireNotNull(target) { "Target is not set!" }.casted<T>()

    /**
     * Called when new events arrive while this runtime is still active.
     */
    fun onNewEvents(newEvents: Array<Event>) {
        if (waitingFor != null) {
            newEvents.forEach {
                it.lock()
            }
            _events?.forEach {
                it.unlock()
            }
            _events = newEvents
            waitingFor?.remind()
        }
    }

    fun recycle() {
        eventValueRegistry = null
        _events = null
        _result = null
        runtimeScope = null
        referents.clear()
        tracker.reset()
        isSuspending = false
        observer = null
        target = null
        isSuccessful = true
        ifSuccessful = null
        waitingFor = null
        fingerprintCrc.reset()
        Pool.release(this)
    }

    override fun toString(): String {
        return "TaskRuntime@${hashCode().toString(16)}(${task.title})"
    }
}