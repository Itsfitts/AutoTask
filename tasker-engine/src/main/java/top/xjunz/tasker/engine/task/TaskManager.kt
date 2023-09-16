/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.engine.task

import java.util.Collections

/**
 * @author xjunz 2022/12/25
 */
abstract class TaskManager<TaskIdentifier, TaskCarrier> {

    protected val tasks: MutableList<XTask> = mutableListOf()

    protected abstract fun asTask(carrier: TaskCarrier): XTask

    protected abstract val TaskCarrier.identifier: TaskIdentifier

    protected abstract fun List<XTask>.indexOfTask(identifier: TaskIdentifier): Int

    private var onTaskPausedStateChangedListener: XTask.OnTaskPausedStateChangedListener? = null

    private val globalOnTaskPausedStateChangedListener =
        XTask.OnTaskPausedStateChangedListener { checksum ->
            onTaskPausedStateChangedListener?.onTaskPauseStateChanged(checksum)
        }

    open fun setOnTaskPausedStateChangedListener(listener: XTask.OnTaskPausedStateChangedListener?) {
        onTaskPausedStateChangedListener = listener
        tasks.forEach {
            setOnTaskPauseStateChangedListenerSticky(it)
        }
    }

    private fun setOnTaskPauseStateChangedListenerSticky(task: XTask) {
        if (onTaskPausedStateChangedListener != null) {
            task.setOnPausedStateChangedListener(globalOnTaskPausedStateChangedListener)
        } else {
            task.setOnPausedStateChangedListener(null)
        }
    }

    fun getEnabledResidentTasks(): List<XTask> {
        return tasks.filter {
            it.isResident
        }
    }

    open fun removeTask(identifier: TaskIdentifier) {
        val index = tasks.indexOfTask(identifier)
        if (index >= 0) {
            tasks[index].halt(true)
            tasks.removeAt(index)
        }
    }

    open fun enableResidentTask(carrier: TaskCarrier) {
        val task = asTask(carrier)
        check(!tasks.contains(task)) {
            "Task [${task.title}] already enabled!"
        }
        tasks.add(task)
        setOnTaskPauseStateChangedListenerSticky(task)
    }

    open fun addOneshotTaskIfAbsent(carrier: TaskCarrier) {
        if (tasks.indexOfTask(carrier.identifier) < 0) {
            tasks.add(asTask(carrier))
        }
    }

    open fun updateTask(previousChecksum: Long, updated: TaskCarrier) {
        val task = tasks.find {
            it.checksum == previousChecksum
        }
        if (task != null) {
            task.halt(true)
            Collections.replaceAll(tasks, task, asTask(updated))
            task.snapshots.clear()
        }
    }

    open fun clearSnapshots(id: TaskIdentifier) {
        requireTask(id).snapshots.clear()
    }

    private fun findTask(id: TaskIdentifier): XTask? {
        return tasks.getOrNull(tasks.indexOfTask(id))
    }

    fun requireTask(id: TaskIdentifier): XTask {
        return tasks[tasks.indexOfTask(id)]
    }

    open fun getSnapshotCount(id: TaskIdentifier): Int {
        return findTask(id)?.snapshots?.size ?: 0
    }

    open fun getAllSnapshots(id: TaskIdentifier): Array<TaskSnapshot> {
        return requireTask(id).snapshots.toTypedArray()
    }

    fun clearAllSnapshots() {
        tasks.forEach {
            it.snapshots.clear()
        }
    }

    open fun clearLog(checksum: Long, snapshotId: String) {
        tasks.find {
            it.checksum == checksum
        }?.snapshots?.find {
            it.id == snapshotId
        }?.clearLog()
    }

    open fun getTaskPauseInfo(identifier: TaskIdentifier): LongArray {
        val task = requireTask(identifier)
        return longArrayOf(task.pauseStartTime, task.pauseFor)
    }
}