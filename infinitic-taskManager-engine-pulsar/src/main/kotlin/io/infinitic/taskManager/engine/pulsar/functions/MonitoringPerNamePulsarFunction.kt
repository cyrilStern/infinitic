package io.infinitic.taskManager.engine.pulsar.functions

import io.infinitic.taskManager.engine.avroClasses.AvroMonitoringPerName
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import io.infinitic.taskManager.engine.pulsar.dispatcher.PulsarAvroDispatcher
import io.infinitic.taskManager.engine.pulsar.storage.PulsarAvroStorage
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class MonitoringPerNamePulsarFunction : Function<AvroEnvelopeForMonitoringPerName, Void> {

    var monitoring = AvroMonitoringPerName()

    override fun process(input: AvroEnvelopeForMonitoringPerName, context: Context?): Void? {
        val ctx = context ?: throw NullPointerException("Null Context received")

        try {
            monitoring.logger = ctx.logger
            monitoring.avroStorage = PulsarAvroStorage(ctx)
            monitoring.avroDispatcher = PulsarAvroDispatcher(ctx)

            monitoring.handle(input)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return null
    }
}