package com.zenaton.workflowManager.engine

import com.zenaton.workflowManager.data.DecisionData
import com.zenaton.workflowManager.data.DecisionId
import com.zenaton.workflowManager.data.DecisionInput
import com.zenaton.workflowManager.data.branches.Branch
import com.zenaton.workflowManager.data.branches.BranchName
import com.zenaton.workflowManager.data.properties.PropertyStore
import com.zenaton.workflowManager.dispatcher.Dispatcher
import com.zenaton.workflowManager.messages.ChildWorkflowCompleted
import com.zenaton.workflowManager.messages.DecisionCompleted
import com.zenaton.workflowManager.messages.DelayCompleted
import com.zenaton.workflowManager.messages.DispatchWorkflow
import com.zenaton.workflowManager.messages.EventReceived
import com.zenaton.workflowManager.messages.TaskCompleted
import com.zenaton.workflowManager.messages.WorkflowCompleted
import com.zenaton.workflowManager.messages.envelopes.ForWorkflowEngineMessage
import com.zenaton.workflowManager.messages.CancelWorkflow
import com.zenaton.workflowManager.messages.ChildWorkflowCanceled
import com.zenaton.workflowManager.messages.DecisionDispatched
import com.zenaton.workflowManager.messages.DispatchDecision
import com.zenaton.workflowManager.messages.TaskCanceled
import com.zenaton.workflowManager.messages.TaskDispatched
import com.zenaton.workflowManager.messages.WorkflowCanceled
import org.slf4j.Logger

class WorkflowEngine {
    lateinit var logger: Logger
    lateinit var storage: WorkflowEngineStateStorage
    lateinit var dispatcher: Dispatcher

    fun handle(msg: ForWorkflowEngineMessage) {
        // discard immediately messages that are not processed
        when (msg) {
            is DecisionDispatched -> return
            is TaskDispatched -> return
            is WorkflowCanceled -> return
            is WorkflowCompleted -> return
        }

        // get associated state
        val oldState = storage.getState(msg.workflowId)

        // discard message it workflow is already terminated
        if (oldState == null && msg !is DispatchWorkflow) return

        // store message (except DecisionCompleted) if a decision is ongoing
        if (oldState?.ongoingDecisionId != null && msg !is DecisionCompleted) {
            val newState = bufferMessage(oldState, msg)
            storage.updateState(msg.workflowId, newState, oldState)
            return
        }

        val newState =
            if (oldState == null)
                dispatchWorkflow(msg as DispatchWorkflow)
            else when (msg) {
                is CancelWorkflow -> cancelWorkflow(oldState, msg)
                is ChildWorkflowCanceled -> childWorkflowCanceled(oldState, msg)
                is ChildWorkflowCompleted -> childWorkflowCompleted(oldState, msg)
                is DecisionCompleted -> decisionCompleted(oldState, msg)
                is DelayCompleted -> delayCompleted(oldState, msg)
                is EventReceived -> eventReceived(oldState, msg)
                is TaskCanceled -> taskCanceled(oldState, msg)
                is TaskCompleted -> taskCompleted(oldState, msg)
                else -> throw Exception("Unknown ForWorkflowEngineMessage: ${msg::class.qualifiedName}")
            }

        // store state if modified
        if (newState != oldState) {
            storage.updateState(msg.workflowId, newState, oldState)
        }
    }

    private fun bufferMessage(state: WorkflowEngineState, msg: ForWorkflowEngineMessage): WorkflowEngineState {
        // buffer this message to handle it after decision returns
        // val bufferedMessages = oldState.bufferedMessages.add(msg)
        // oldState.bufferedMessages.add(msg)
        TODO()
    }

    private fun cancelWorkflow(state: WorkflowEngineState, msg: CancelWorkflow): WorkflowEngineState {
        TODO()
    }

    private fun childWorkflowCanceled(state: WorkflowEngineState, msg: ChildWorkflowCanceled): WorkflowEngineState {
        TODO()
    }

    private fun dispatchWorkflow(msg: DispatchWorkflow): WorkflowEngineState {
        val state = WorkflowEngineState(workflowId = msg.workflowId)
        val decisionId = DecisionId()
        // define branch
        val branch = Branch(
            branchName = BranchName("handle"),
            branchInput = msg.workflowInput
        )
        // initialize state
        state.ongoingDecisionId = decisionId
        state.runningBranches.add(branch)
        // create DecisionDispatched message
        val decisionInput = DecisionInput(listOf(branch), filterStore(state.store, listOf(branch)))
        val m = DispatchDecision(
            decisionId = decisionId,
            workflowId = msg.workflowId,
            workflowName = msg.workflowName,
            decisionData = DecisionData(listOf()) // AvroSerDe.serialize(decisionInput))
        )
        // dispatch decision
        dispatcher.toDeciders(m)
        // log event
        val dd = DecisionDispatched(
            decisionId = decisionId,
            workflowId = msg.workflowId,
            workflowName = msg.workflowName,
            decisionData = DecisionData(listOf()) // AvroSerDe.serialize(decisionInput))
        )
        dispatcher.toWorkflowEngine(dd)
        // save state
        storage.updateState(msg.workflowId, state, null)

        return state
    }

    private fun decisionCompleted(state: WorkflowEngineState, msg: DecisionCompleted): WorkflowEngineState {
        TODO()
    }

    private fun delayCompleted(state: WorkflowEngineState, msg: DelayCompleted): WorkflowEngineState {
        TODO()
    }

    private fun taskCanceled(state: WorkflowEngineState, msg: TaskCanceled): WorkflowEngineState {
        TODO()
    }

    private fun taskCompleted(state: WorkflowEngineState, msg: TaskCompleted): WorkflowEngineState {
        TODO()
    }

    private fun childWorkflowCompleted(state: WorkflowEngineState, msg: ChildWorkflowCompleted): WorkflowEngineState {
        TODO()
    }

    private fun completeDelay(state: WorkflowEngineState, msg: DelayCompleted): WorkflowEngineState {
        TODO()
    }

    private fun eventReceived(state: WorkflowEngineState, msg: EventReceived): WorkflowEngineState {
        TODO()
    }

    private fun filterStore(store: PropertyStore, branches: List<Branch>): PropertyStore {
        // Retrieve properties at step at completion in branches
        val listProperties1 = branches.flatMap {
            b ->
            b.steps.filter { it.propertiesAfterCompletion != null }.map { it.propertiesAfterCompletion!! }
        }
        // Retrieve properties when starting in branches
        val listProperties2 = branches.map {
            b ->
            b.propertiesAtStart
        }
        // Retrieve List<PropertyHash?> relevant for branches
        val listHashes = listProperties1.union(listProperties2).flatMap { it.properties.values }
        // Keep only relevant keys
        val properties = store.properties.filterKeys { listHashes.contains(it) }

        return PropertyStore(properties)
    }
}