package io.infinitic.workflowManager.engine.data.branches

import io.infinitic.common.data.DateTime
import io.infinitic.taskManager.common.data.TaskId
import io.infinitic.taskManager.common.data.TaskOutput
import io.infinitic.workflowManager.engine.data.DelayId
import io.infinitic.workflowManager.engine.data.EventData
import io.infinitic.workflowManager.engine.data.EventName
import io.infinitic.workflowManager.engine.data.WorkflowId
import io.infinitic.workflowManager.engine.data.commands.Command
import io.infinitic.workflowManager.data.commands.CommandStatus
import io.infinitic.workflowManager.engine.data.commands.DispatchChildWorkflow
import io.infinitic.workflowManager.engine.data.commands.DispatchTask
import io.infinitic.workflowManager.engine.data.commands.WaitDelay
import io.infinitic.workflowManager.engine.data.commands.WaitEvent
import io.infinitic.workflowManager.engine.data.properties.Properties
import io.infinitic.workflowManager.engine.data.steps.Step

data class Branch(
    val branchId: BranchId = BranchId(),
    val branchName: BranchName,
    val branchInput: BranchInput,
    val propertiesAtStart: Properties = Properties(mapOf()),
    val dispatchedAt: DateTime = DateTime(),
    val steps: List<Step> = listOf(),
    val commands: List<Command> = listOf()
) {
    fun completeTask(taskId: TaskId, taskOutput: TaskOutput, properties: Properties): Boolean {
        // complete action if relevant
        val task = commands
            .filterIsInstance<DispatchTask>()
            .firstOrNull { a -> a.taskId == taskId && a.actionStatus != CommandStatus.COMPLETED }
        task?.taskOutput = taskOutput
        task?.actionStatus = CommandStatus.COMPLETED

        // does this task complete the current step?
        return steps.last().completeTask(taskId, properties)
    }

    fun completeChildWorkflow(childWorkflowId: WorkflowId, childWorkflowOutput: BranchOutput, properties: Properties): Boolean {
        // complete action if relevant
        val childWorkflow = commands
            .filterIsInstance<DispatchChildWorkflow>()
            .firstOrNull { a -> a.childWorkflowId == childWorkflowId && a.actionStatus != CommandStatus.COMPLETED }
        childWorkflow?.childWorkflowOutput = childWorkflowOutput
        childWorkflow?.actionStatus = CommandStatus.COMPLETED

        // does this task complete the current step?
        return steps.last().completeChildWorkflow(childWorkflowId, properties)
    }

    fun completeDelay(delayId: DelayId, properties: Properties): Boolean {
        // complete action if relevant
        val delay = commands
            .filterIsInstance<WaitDelay>()
            .firstOrNull { a -> a.delayId == delayId && a.actionStatus != CommandStatus.COMPLETED }
        delay?.actionStatus = CommandStatus.COMPLETED

        // does this task complete the current step?
        return steps.last().completeDelay(delayId, properties)
    }

    fun completeEvent(eventName: EventName, eventData: EventData, properties: Properties): Boolean {
        // complete action if relevant
        val event = commands
            .filterIsInstance<WaitEvent>()
            .firstOrNull { a -> a.eventName == eventName && a.actionStatus != CommandStatus.COMPLETED }
        event ?.eventData = eventData
        event ?.actionStatus = CommandStatus.COMPLETED

        // does this task complete the current step?
        return if (event != null) steps.last().completeEvent(event.eventId, properties) else false
    }
}