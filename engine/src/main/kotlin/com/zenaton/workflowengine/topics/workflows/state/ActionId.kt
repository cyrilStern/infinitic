package com.zenaton.workflowengine.topics.workflows.state

import com.zenaton.commons.data.interfaces.IdInterface
import com.zenaton.taskManager.data.JobId
import com.zenaton.workflowengine.data.DelayId
import com.zenaton.workflowengine.data.EventId
import com.zenaton.workflowengine.data.WorkflowId
import java.util.UUID

data class ActionId(override val id: String = UUID.randomUUID().toString()) : IdInterface {
    constructor(jobId: JobId) : this(jobId.id)
    constructor(delayId: DelayId) : this(delayId.id)
    constructor(workflowId: WorkflowId) : this(workflowId.id)
    constructor(eventId: EventId) : this(eventId.id)
}
