package top.xjunz.tasker.engine.task

import android.os.Parcel
import android.os.Parcelable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.xjunz.shared.ktx.md5
import top.xjunz.tasker.engine.applet.base.Applet
import top.xjunz.tasker.engine.applet.base.RootFlow
import top.xjunz.tasker.engine.runtime.Event
import top.xjunz.tasker.engine.runtime.Snapshot
import top.xjunz.tasker.engine.runtime.TaskRuntime
import top.xjunz.tasker.engine.runtime.TaskRuntime.Companion.obtainRuntime

/**
 * The abstraction of an automator task.
 *
 * **XTask** is the abbr of "XJUNZ-TASK", rather cool isn't it? :)
 *
 * @author xjunz 2022/07/12
 */
class XTask {

    companion object {
        const val TYPE_RESIDENT = 0
        const val TYPE_ONESHOT = 1
    }

    inline val checksum get() = metadata.checksum

    inline val isPreload get() = metadata.isPreload

    lateinit var metadata: Metadata

    var flow: RootFlow? = null

    /**
     * Whether the task is active or not. Even if set to `false`, the task may continue executing until
     * its latest [Applet] is completed. You can observe [OnStateChangedListener.onCancelled] to
     * get notified. Inactive tasks will no longer response to any further [Event] from [launch].
     */
    var isEnabled = false
        private set

    var onStateChangedListener: OnStateChangedListener? = null

    /**
     * Whether the task is traversing its [flow].
     */
    private val isExecuting get() = currentRuntime?.isActive == true

    private var currentRuntime: TaskRuntime? = null

    class FlowFailureException(reason: String) : RuntimeException(reason)

    interface OnStateChangedListener {

        fun onStarted(task: XTask) {}

        /**
         * When the task completes due to an unexpected error.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onError(runtime: TaskRuntime, t: Throwable) {}

        /**
         * When the flow completes failed.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onFailure(runtime: TaskRuntime) {}

        /**
         * When the task completes successful.
         *
         * **Note**: It's the caller's duty to recycle the [runtime].
         */
        fun onSuccess(runtime: TaskRuntime) {}

        /**
         * When the task is cancelled.
         */
        fun onCancelled(runtime: TaskRuntime) {}
    }

    fun requireFlow(): RootFlow = requireNotNull(flow) {
        "RootFlow is not initialized!"
    }

    fun enable(stateListener: OnStateChangedListener? = null) {
        check(!isEnabled) {
            "Task has already been activated!"
        }
        onStateChangedListener = stateListener
        isEnabled = true
        onStateChangedListener?.onStarted(this)
    }

    fun disable() {
        check(isEnabled) {
            "The task is not enabled!"
        }
        isEnabled = false
        if (!isExecuting) {
            currentRuntime?.halt()
        }
    }

    /**
     * Called when an event is received.
     *
     * @return `true` if the task starts executed and `false` otherwise
     */
    suspend fun launch(
        snapshot: Snapshot,
        scope: CoroutineScope,
        events: Array<out Event>,
        observer: TaskRuntime.Observer? = null
    ): Boolean {
        // if (!isEnabled) return false
        // Cancel if executing //TODO: 单线程情况下，Will this hit?
        if (isExecuting) {
            currentRuntime?.halt()
        }
        val runtime = obtainRuntime(snapshot, scope, events)
        runtime.observer = observer
        try {
            currentRuntime = runtime
            onStateChangedListener?.onStarted(this)
            requireFlow().apply(runtime)
            if (runtime.isSuccessful) {
                onStateChangedListener?.onSuccess(runtime)
            } else {
                onStateChangedListener?.onFailure(runtime)
            }
            return runtime.isSuccessful
        } catch (t: Throwable) {
            when (t) {
                is FlowFailureException -> onStateChangedListener?.onFailure(runtime)
                is CancellationException -> onStateChangedListener?.onCancelled(runtime)
                else -> onStateChangedListener?.onError(runtime, t)
            }
            return false
        } finally {
            currentRuntime = null
            runtime.recycle()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as XTask

        if (checksum != other.checksum) return false

        return true
    }

    override fun hashCode(): Int {
        return checksum.hashCode()
    }

    @Serializable
    data class Metadata(
        @SerialName("ti") var title: String,

        @SerialName("ty") var taskType: Int = TYPE_RESIDENT,

        @SerialName("d")
        var description: String? = null,

        @SerialName("c")
        var creationTimestamp: Long = -1,

        @SerialName("m")
        var modificationTimestamp: Long = -1,

        @SerialName("s")
        var checksum: Long = -1,

        @SerialName("a")
        var author: String? = null,

        @SerialName("p")
        var isPreload: Boolean = false
    ) : Parcelable {

        inline val identifier get() = checksum.toString().md5.substring(0, 7)

        constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readInt(),
            parcel.readString(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readString(),
            parcel.readByte() != 0.toByte()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(title)
            parcel.writeInt(taskType)
            parcel.writeString(description)
            parcel.writeLong(creationTimestamp)
            parcel.writeLong(modificationTimestamp)
            parcel.writeLong(checksum)
            parcel.writeString(author)
            parcel.writeByte(if (isPreload) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Metadata> {
            override fun createFromParcel(parcel: Parcel): Metadata {
                return Metadata(parcel)
            }

            override fun newArray(size: Int): Array<Metadata?> {
                return arrayOfNulls(size)
            }
        }

    }
}