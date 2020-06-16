package com.zenaton.workflowengine.pulsar.topics.workflows.dispatcher

import com.zenaton.workflowengine.pulsar.topics.Topic
import com.zenaton.workflowengine.pulsar.topics.workflows.messages.WorkflowMessageContainer
import com.zenaton.workflowengine.topics.workflows.dispatcher.WorkflowDispatcherInterface
import com.zenaton.workflowengine.topics.workflows.interfaces.WorkflowMessageInterface
import org.apache.pulsar.client.impl.schema.JSONSchema
import org.apache.pulsar.functions.api.Context
import java.util.concurrent.TimeUnit

class WorkflowDispatcher(val context: Context) :
    WorkflowDispatcherInterface {
    override fun dispatch(msg: WorkflowMessageInterface, after: Float) {
        val msgBuilder = context
            .newOutputMessage(Topic.WORKFLOWS.get(), JSONSchema.of(WorkflowMessageContainer::class.java))
            .key(msg.getStateId())
            .value(WorkflowMessageContainer(msg))

        if (after > 0) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        msgBuilder.send()
    }
}