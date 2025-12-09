package net.easecation.fabric.chunker.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.StringArgumentType
import net.easecation.fabric.chunker.core.Location
import net.easecation.fabric.chunker.core.PositionLayout
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
import net.minecraft.util.JsonReaderUtils
import net.minecraft.util.math.BlockPos

class ECPositionCommand {
    companion object {
        private val playerLayoutEnableList: MutableList<String> = ArrayList()
        private val playerLayoutMap: MutableMap<String, PositionLayout> = HashMap()

        fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
            val ecposition = dispatcher.register(literal("ecposition")
                .executes { ctx ->
                    val player = ctx.source.player ?: return@executes 0
                    sendPlayerPosition(player)
                    return@executes 1
                }
                .then(literal("enable")
                    .executes { ctx ->
                        val player = ctx.source.player ?: return@executes 0
                        if (!playerLayoutEnableList.contains(player.nameForScoreboard)) {
                            playerLayoutEnableList.add(player.nameForScoreboard)
                        }
                        player.sendMessage(Text.of("[ECBuildingHelper] 已开启选点模式"))
                        return@executes 1
                    }
                )
                .then(literal("disable")
                    .executes { ctx ->
                        val player = ctx.source.player ?: return@executes 0
                        playerLayoutEnableList.remove(player.nameForScoreboard)
                        player.sendMessage(Text.of("[ECBuildingHelper] 已关闭选点模式"))
                        return@executes 1
                    }
                )
                .then(literal("format")
                    .executes { ctx ->
                        val player = ctx.source.player ?: return@executes 0
                        val layout: PositionLayout? = playerLayoutMap[player.nameForScoreboard]
                        if (layout == null || layout === PositionLayout.COLON) {
                            playerLayoutMap[player.nameForScoreboard] = PositionLayout.COMMA
                            player.sendMessage(Text.of("[ECBuildingHelper] 选点模式已切换至 逗号模式"))
                        } else if (layout === PositionLayout.COMMA) {
                            playerLayoutMap[player.nameForScoreboard] = PositionLayout.COMMA_MOVE
                            player.sendMessage(Text.of("[ECBuildingHelper] 选点模式已切换至 逗号模式(精确)"))
                        } else if (layout === PositionLayout.COMMA_MOVE) {
                            playerLayoutMap[player.nameForScoreboard] = PositionLayout.COLON
                            player.sendMessage(Text.of("[ECBuildingHelper] 选点模式已切换至 冒号模式"))
                        }
                        return@executes 1
                    }
                    .then(argument("mode", StringArgumentType.string())
                        .executes { ctx ->
                            val player = ctx.source.player ?: return@executes 0
                            val mode = ctx.getArgument("mode", String::class.java)
                            when (mode) {
                                "comma", "逗号" -> {
                                    playerLayoutMap[player.nameForScoreboard] = PositionLayout.COMMA
                                    player.sendMessage(Text.of("[ECBuildingHelper] 已启用选点 (逗号模式)"))
                                }
                                "colon", "冒号" -> {
                                    playerLayoutMap[player.nameForScoreboard] = PositionLayout.COLON
                                    player.sendMessage(Text.of("[ECBuildingHelper] 已启用选点 (冒号模式)"))
                                }
                                "comma_camera", "camera", "相机" -> {
                                    playerLayoutMap[player.nameForScoreboard] = PositionLayout.COMMA_CAMERA
                                    player.sendMessage(Text.of("[ECBuildingHelper] 已启用选点 (相机逗号模式)"))
                                }
                                "comma_move", "move", "移动" -> {
                                    playerLayoutMap[player.nameForScoreboard] = PositionLayout.COMMA_MOVE
                                    player.sendMessage(Text.of("[ECBuildingHelper] 已启用选点 (逗号精确模式)"))
                                }
                                else -> player.sendMessage(Text.of("[ECBuildingHelper] 请输入正确的模式: comma(逗号) 或 colon(冒号)"))
                            }
                            return@executes 1
                        }
                    )
                )
            )
            dispatcher.register(literal("ecpos")
                .executes { ctx ->
                    val player = ctx.source.player ?: return@executes 0
                    sendPlayerPosition(player)
                    return@executes 1
                }
                .redirect(ecposition)
            )
        }

        fun getPlayerLayoutEnableList(): List<String> {
            return playerLayoutEnableList
        }

        fun getPlayerLayout(player: PlayerEntity): PositionLayout {
            return playerLayoutMap.getOrDefault(player.nameForScoreboard, PositionLayout.COMMA)
        }

        fun sendPlayerPosition(player: PlayerEntity) {
            val loc = Location(player)
            val text: String = getPlayerLayout(player).format(loc)
            val rawMessage = "[{\"text\":\"你的坐标: \",\"color\":\"gray\"},{\"text\":\"$text\",\"color\":\"white\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"点击复制\"},\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$text\"}}]"
            player.sendMessage(JsonReaderUtils.parse(StringReader(rawMessage) ,TextCodecs.CODEC))
        }

        fun sendBlockPosition(player: PlayerEntity, block: BlockPos) {
            val text: String = getPlayerLayout(player).formatPos(Location(block))
            val rawMessage = "[{\"text\":\"方块坐标: \",\"color\":\"gray\"},{\"text\":\"$text\",\"color\":\"green\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"点击复制\"},\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$text\"}}]"
            player.sendMessage(JsonReaderUtils.parse(StringReader(rawMessage) ,TextCodecs.CODEC))
        }
    }
}
