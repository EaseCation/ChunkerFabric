package net.easecation.fabric.chunker.command

import com.hivemc.chunker.scheduling.TaskMonitorThread
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.easecation.fabric.chunker.core.FabricChunkerConverter
import net.easecation.fabric.chunker.mixin.UnmodifiableLevelPropertiesAccessor
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import net.minecraft.world.SaveProperties
import net.minecraft.world.World
import net.minecraft.world.dimension.DimensionType
import org.yaml.snakeyaml.Yaml
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
class ChunkerCommand {
    companion object {
        fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
            dispatcher.register(
                literal("chunker")
                    .then(literal("import")
                        .then(argument("name", IdentifierArgumentType.identifier())
                            .then(argument("path", StringArgumentType.string())
                                .executes { ctx ->
                                    val name = ctx.getArgument("name", Identifier::class.java)
                                    val path = ctx.getArgument("path", String::class.java)

                                    val commandSource = ctx.source
                                    val world = commandSource.world
                                    val server = world.server

                                    val saveDirectory = Path(".").resolve(path)
                                    if (!saveDirectory.exists()) return@executes -1
                                    val outputDirectory = server.getSavePath(WorldSavePath.ROOT).resolve("dimensions").resolve(name.namespace).resolve(name.path).absolute().normalize()
                                    if (outputDirectory.exists()) return@executes -1

                                    commandSource.sendFeedback(
                                        { Text.literal("Output directory is : $outputDirectory") },
                                        false
                                    )
                                    commandSource.sendFeedback(
                                        { Text.literal("Save directory is : $saveDirectory") },
                                        false
                                    )
                                    commandSource.sendFeedback(
                                        { Text.literal("Importing world using Chunker...") },
                                        false
                                    )

                                    val worldsConfigDirectory = FabricLoader.getInstance().configDir.resolve("multiworld").resolve("worlds")
                                    if (worldsConfigDirectory.exists()) {
                                        val newConfigPath = worldsConfigDirectory.resolve(name.namespace).resolve("${name.path}.yml")
                                        if (newConfigPath.exists()) return@executes -1

                                        commandSource.sendFeedback(
                                            { Text.literal("Creating multiworld configs ...") },
                                            false
                                        )
                                        newConfigPath.createParentDirectories()
                                        val yaml = Yaml()
                                        val config = HashMap<String, Any>()
                                        config["namespace"] = name.namespace
                                        config["path"] = name.path
                                        config["environment"] = "NORMAL"
                                        config["seed"] = 0
                                        val newConfig = yaml.dumpAsMap(config)
                                        newConfigPath.writeText(newConfig)
                                    }

                                    val converter = FabricChunkerConverter(
                                        keepOriginalNBT = false,
                                        preserveUnknownEntities = true,
                                        preserveUnknownBlockEntities = true,
                                        inputDirectory = saveDirectory.toFile(),
                                        outputDirectory = outputDirectory.toFile(),
                                        format = "JAVA_1_20"
                                    )
                                    val environment = converter.convert(commandSource) ?: return@executes -1
                                    var lastProgressReport = System.currentTimeMillis()
                                    val monitor = TaskMonitorThread(
                                        environment,
                                        { progress ->
                                            ctx.source.server.execute {
                                                val currentTime = System.currentTimeMillis()
                                                if (currentTime - lastProgressReport > 500) {
                                                    commandSource.sendFeedback({
                                                        Text.literal(
                                                            String.format(
                                                                "%.2f%%",
                                                                progress * 100.0
                                                            )
                                                        )
                                                    }, false)
                                                    lastProgressReport = currentTime
                                                }
                                            }
                                        },
                                        { exception ->
                                            ctx.source.server.execute {
                                                if (exception.isPresent) {
                                                    commandSource.sendFeedback({
                                                        Text.literal(
                                                            exception.get().stackTraceToString()
                                                        )
                                                    }, false)
                                                    commandSource.sendFeedback(
                                                        { Text.literal("Failed with exception") },
                                                        false
                                                    )
                                                } else {
                                                    commandSource.sendFeedback(
                                                        { Text.literal("Conversion and import completed! Please restart server to load world!") },
                                                        false
                                                    )
                                                }
                                            }
                                        }
                                    )
                                    monitor.start()

                                    return@executes 1
                                }
                            )
                        )
                    )
                    .then(literal("copy")
                        .then(argument("name", IdentifierArgumentType.identifier())
                            .executes { ctx ->
                                val name = ctx.getArgument("name", Identifier::class.java)

                                val commandSource = ctx.source
                                val world = commandSource.world
                                val server = world.server

                                commandSource.sendFeedback({ Text.literal("Saving world...") }, false)
                                world.save(null, true, false)
                                commandSource.sendFeedback({ Text.literal("World saved") }, false)

                                val saveDirectory = DimensionType.getSaveDirectory(
                                    world.registryKey,
                                    server.getSavePath(WorldSavePath.ROOT)
                                )
                                val outputDirectory = server.getSavePath(WorldSavePath.ROOT).resolve("dimensions").resolve(name.namespace).resolve(name.path).absolute().normalize()
                                if (outputDirectory.exists()) return@executes -1

                                commandSource.sendFeedback(
                                    { Text.literal("Save directory is : $saveDirectory") },
                                    false
                                )
                                commandSource.sendFeedback(
                                    { Text.literal("Output directory is : $outputDirectory") },
                                    false
                                )
                                commandSource.sendFeedback(
                                    { Text.literal("Copying world...") },
                                    false
                                )

                                outputDirectory.createParentDirectories()
                                saveDirectory.copyToRecursively(
                                    target = outputDirectory,
                                    onError = { source, target, exception ->
                                        commandSource.sendFeedback(
                                            { Text.literal("Failed to copy $source to $target: ${exception.message}") },
                                            false
                                        )
                                        OnErrorResult.TERMINATE
                                    },
                                    followLinks = true,
                                    overwrite = true
                                )

                                val worldsConfigDirectory = FabricLoader.getInstance().configDir.resolve("multiworld").resolve("worlds")
                                if (worldsConfigDirectory.exists()) {
                                    val oldConfigPath = worldsConfigDirectory.resolve(world.registryKey.value.namespace).resolve("${world.registryKey.value.path}.yml")
                                    val newConfigPath = worldsConfigDirectory.resolve(name.namespace).resolve("${name.path}.yml")
                                    if (!oldConfigPath.exists()) return@executes -1
                                    if (newConfigPath.exists()) return@executes -1

                                    commandSource.sendFeedback(
                                        { Text.literal("Creating multiworld configs ...") },
                                        false
                                    )
                                    newConfigPath.createParentDirectories()
                                    val yaml = Yaml()
                                    val config = yaml.load<MutableMap<String, Any>>(oldConfigPath.toFile().inputStream())
                                    config["namespace"] = name.namespace
                                    config["path"] = name.path
                                    val newConfig = yaml.dumpAsMap(config)
                                    newConfigPath.writeText(newConfig)
                                }

                                commandSource.sendFeedback(
                                    { Text.literal("World copied! Please restart server to load world!") },
                                    false
                                )

                                return@executes 1
                            }
                        )
                    )
                    .then(literal("export")
                        .executes { ctx ->
                            val commandSource = ctx.source
                            val world = commandSource.world
                            val server = world.server

                            commandSource.sendFeedback({ Text.literal("Saving world...") }, false)
                            world.save(null, true, false)
                            commandSource.sendFeedback({ Text.literal("World saved") }, false)

                            val saveDirectory = DimensionType.getSaveDirectory(
                                world.registryKey,
                                server.getSavePath(WorldSavePath.ROOT)
                            )
                            if (world.registryKey != World.OVERWORLD) {
                                val saveProperties = when (world.levelProperties) {
                                    is SaveProperties -> world.levelProperties as SaveProperties
                                    is UnmodifiableLevelPropertiesAccessor -> (world.levelProperties as UnmodifiableLevelPropertiesAccessor).saveProperties
                                    else -> null
                                }
                                val saveLevelData = saveDirectory.resolve("level.dat")
                                if (saveProperties == null) {
                                    commandSource.sendFeedback(
                                        { Text.literal("Can not locate save properties. Copying overworld level.dat") },
                                        false
                                    )
                                    val rootDirectory = DimensionType.getSaveDirectory(
                                        World.OVERWORLD,
                                        world.server.getSavePath(WorldSavePath.ROOT)
                                    )
                                    val rootLevelData = rootDirectory.resolve("level.dat")
                                    rootLevelData.copyTo(saveLevelData, overwrite = true)
                                } else {
                                    val nbtCompound = saveProperties.cloneWorldNbt(server.registryManager, null)
                                    val nbt = NbtCompound()
                                    nbt.put("Data", nbtCompound)
                                    NbtIo.writeCompressed(nbt, saveLevelData)
                                }
                            }
                            val subPath = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss").format(LocalDateTime.now())
                            val outputDirectory =
                                Path(".").resolve("chunker_export").resolve(subPath).absolute().normalize()
                            if (outputDirectory.exists()) return@executes -1

                            commandSource.sendFeedback(
                                { Text.literal("Save directory is : $saveDirectory") },
                                false
                            )
                            commandSource.sendFeedback(
                                { Text.literal("Output directory is : $outputDirectory") },
                                false
                            )
                            commandSource.sendFeedback(
                                { Text.literal("Exporting to bedrock world using Chunker...") },
                                false
                            )

                            val converter = FabricChunkerConverter(
                                keepOriginalNBT = false,
                                preserveUnknownEntities = true,
                                preserveUnknownBlockEntities = true,
                                inputDirectory = saveDirectory.toFile(),
                                outputDirectory = outputDirectory.toFile(),
                                format = "BEDROCK_R20"
                            )
                            val environment = converter.convert(commandSource) ?: return@executes -1
                            var lastProgressReport = System.currentTimeMillis()
                            val monitor = TaskMonitorThread(
                                environment,
                                { progress ->
                                    ctx.source.server.execute {
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - lastProgressReport > 500) {
                                            commandSource.sendFeedback({
                                                Text.literal(
                                                    String.format(
                                                        "%.2f%%",
                                                        progress * 100.0
                                                    )
                                                )
                                            }, false)
                                            lastProgressReport = currentTime
                                        }
                                    }
                                },
                                { exception ->
                                    ctx.source.server.execute {
                                        if (exception.isPresent) {
                                            commandSource.sendFeedback({
                                                Text.literal(
                                                    exception.get().stackTraceToString()
                                                )
                                            }, false)
                                            commandSource.sendFeedback(
                                                { Text.literal("Failed with exception") },
                                                false
                                            )
                                        } else {
                                            commandSource.sendFeedback(
                                                { Text.literal("Conversion and export completed!") },
                                                false
                                            )
                                        }
                                    }
                                }
                            )
                            monitor.start()
                            return@executes 1
                        })
            )
        }
    }
}
