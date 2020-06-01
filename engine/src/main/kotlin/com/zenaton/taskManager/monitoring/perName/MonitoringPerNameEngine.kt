package com.zenaton.taskManager.monitoring.perName

import com.zenaton.taskManager.data.JobStatus
import com.zenaton.taskManager.dispatcher.Dispatcher
import com.zenaton.taskManager.logger.Logger
import com.zenaton.taskManager.monitoring.global.JobCreated

class MonitoringPerNameEngine {
    lateinit var storage: MonitoringPerNameStorage
    lateinit var dispatcher: Dispatcher
    lateinit var logger: Logger

    fun handle(message: MonitoringPerNameMessage) {
        // get associated state
        val oldState = storage.getState(message.jobName)
        val newState = oldState?.copy() ?: MonitoringPerNameState(message.jobName)

        when (message) {
            is JobStatusUpdated -> handleTaskStatusUpdated(message, newState)
        }

        // Update stored state if needed and existing
        if (newState != oldState) {
            storage.updateState(message.jobName, newState, oldState)
        }

        // It's a new task type
        if (oldState == null) {
            val tsc = JobCreated(jobName = message.jobName)

            dispatcher.dispatch(tsc)
        }
    }

    private fun handleTaskStatusUpdated(message: JobStatusUpdated, state: MonitoringPerNameState) {
        when (message.oldStatus) {
            JobStatus.RUNNING_OK -> state.runningOkCount--
            JobStatus.RUNNING_WARNING -> state.runningWarningCount--
            JobStatus.RUNNING_ERROR -> state.runningErrorCount--
            JobStatus.TERMINATED_COMPLETED -> state.terminatedCompletedCount--
            JobStatus.TERMINATED_CANCELED -> state.terminatedCanceledCount--
            null -> Unit
        }

        when (message.newStatus) {
            JobStatus.RUNNING_OK -> state.runningOkCount++
            JobStatus.RUNNING_WARNING -> state.runningWarningCount++
            JobStatus.RUNNING_ERROR -> state.runningErrorCount++
            JobStatus.TERMINATED_COMPLETED -> state.terminatedCompletedCount++
            JobStatus.TERMINATED_CANCELED -> state.terminatedCanceledCount++
        }
    }
}
