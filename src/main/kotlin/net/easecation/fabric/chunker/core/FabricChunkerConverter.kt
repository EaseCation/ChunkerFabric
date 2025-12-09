package net.easecation.fabric.chunker.core

import com.hivemc.chunker.cli.messenger.Messenger
import com.hivemc.chunker.conversion.WorldConverter
import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.scheduling.task.TrackedTask
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import java.io.File
import java.text.MessageFormat
import java.util.*
import kotlin.jvm.optionals.getOrNull

data class FabricChunkerConverter(
    val keepOriginalNBT: Boolean,
    val preserveUnknownEntities: Boolean,
    val preserveUnknownBlockEntities: Boolean,
    val inputDirectory: File,
    val outputDirectory: File,
    val format: String
) {
    fun convert(commandSource: ServerCommandSource): TrackedTask<Void>? {
        val worldConverter = WorldConverter(UUID.randomUUID())
        worldConverter.setAllowNBTCopying(this.keepOriginalNBT)
        worldConverter.setPreserveUnknownEntities(this.preserveUnknownEntities)
        worldConverter.setPreserveUnknownBlockEntities(this.preserveUnknownBlockEntities)
        val reader = EncodingType.findReader(this.inputDirectory, worldConverter).getOrNull()
        val writer = Messenger.findWriter(this.format, worldConverter, this.outputDirectory).getOrNull()
        if (reader == null) {
            commandSource.sendFeedback({ Text.literal("Failed to find suitable reader for the world.") }, false)
            return null
        }
        if (writer == null) {
            commandSource.sendFeedback({ Text.literal("Failed to find suitable writer for the world.") }, false)
            return null
        }
        commandSource.sendFeedback({
            Text.literal(
                MessageFormat.format(
                    "Converting from {0} {1} to {2} {3}",
                    reader.encodingType.name,
                    reader.version,
                    writer.encodingType.name,
                    writer.version
                )
            )
        }, false)
        worldConverter.setCompactionSignal { started ->
            if (started) {
                commandSource.sendFeedback({ Text.literal("Compacting world, this may take a while...") }, false)
            } else {
                commandSource.sendFeedback({ Text.literal("Finished compacting world.") }, false)
            }
        }
        val conversionTask = worldConverter.convert(reader, writer)
        return conversionTask
    }
}