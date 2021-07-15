/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.pulsar.workers

import io.infinitic.common.data.Name
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.exceptions.thisShouldNotHappen
import io.infinitic.pulsar.topics.TaskTopic
import io.infinitic.pulsar.topics.TopicType
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarMessageToProcess
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.tasks.engine.storage.TaskStateStorage
import io.infinitic.tasks.engine.worker.startTaskEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import org.apache.pulsar.client.api.Consumer

typealias PulsarTaskEngineMessageToProcess = PulsarMessageToProcess<TaskEngineMessage>

@Suppress("UNCHECKED_CAST")
fun CoroutineScope.startPulsarTaskEngines(
    name: Name,
    consumerName: String,
    concurrency: Int,
    storage: TaskStateStorage,
    consumerFactory: PulsarConsumerFactory,
    output: PulsarOutput
) {
    repeat(concurrency) {

        val eventsInputChannel = Channel<PulsarTaskEngineMessageToProcess>()
        val eventsOutputChannel = Channel<PulsarTaskEngineMessageToProcess>()
        val commandsInputChannel = Channel<PulsarTaskEngineMessageToProcess>()
        val commandsOutputChannel = Channel<PulsarTaskEngineMessageToProcess>()

        startTaskEngine(
            "task-engine:$it",
            storage,
            eventsInputChannel = eventsInputChannel,
            eventsOutputChannel = eventsOutputChannel,
            commandsInputChannel = commandsInputChannel,
            commandsOutputChannel = commandsOutputChannel,
            output.sendToClient(),
            output.sendToTaskTagEngine(TopicType.EXISTING),
            output.sendToTaskEngineAfter(name),
            output.sendToWorkflowEngine(TopicType.EXISTING),
            output.sendToTaskExecutors(name),
            output.sendToMetricsPerName()
        )

        // Pulsar consumers
        val existingConsumer = when (name) {
            is TaskName -> consumerFactory.newConsumer(
                consumerName = "$consumerName:$it",
                taskTopic = TaskTopic.ENGINE_EXISTING,
                taskName = name
            )
            is WorkflowName -> consumerFactory.newConsumer(
                consumerName = "$consumerName:$it",
                workflowTaskTopic = WorkflowTaskTopic.ENGINE_EXISTING,
                workflowName = name
            )
            else -> throw thisShouldNotHappen()
        } as Consumer<TaskEngineEnvelope>

        val newConsumer = when (name) {
            is TaskName -> consumerFactory.newConsumer(
                consumerName = "$consumerName:$it",
                taskTopic = TaskTopic.ENGINE_NEW,
                taskName = name
            )
            is WorkflowName -> consumerFactory.newConsumer(
                consumerName = "$consumerName:$it",
                workflowTaskTopic = WorkflowTaskTopic.ENGINE_NEW,
                workflowName = name
            )
            else -> throw thisShouldNotHappen()
        } as Consumer<TaskEngineEnvelope>

        // coroutine pulling pulsar events messages
        pullMessages(existingConsumer, eventsInputChannel)

        // coroutine pulling pulsar commands messages
        pullMessages(newConsumer, commandsInputChannel)

        // coroutine acknowledging pulsar event messages
        acknowledgeMessages(existingConsumer, eventsOutputChannel)

        // coroutine acknowledging pulsar commands messages
        acknowledgeMessages(newConsumer, commandsOutputChannel)
    }
}
