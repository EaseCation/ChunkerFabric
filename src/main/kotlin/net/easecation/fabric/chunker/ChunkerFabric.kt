package net.easecation.fabric.chunker

import net.easecation.fabric.chunker.command.ChunkerCommand
import net.easecation.fabric.chunker.command.ECPositionCommand
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import org.slf4j.LoggerFactory

object ChunkerFabric : ModInitializer {
    private val logger = LoggerFactory.getLogger("chunker-fabric")

	override fun onInitialize() {
		CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
			ChunkerCommand.register(dispatcher)
			ECPositionCommand.register(dispatcher)
		}
		UseItemCallback.EVENT.register { player, world, hand ->
			if (ECPositionCommand.getPlayerLayoutEnableList().contains(player.nameForScoreboard)) {
				ECPositionCommand.sendPlayerPosition(player)
			}
			TypedActionResult.pass(ItemStack.EMPTY)
		}
		UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
			if (ECPositionCommand.getPlayerLayoutEnableList().contains(player.nameForScoreboard) && hand == Hand.OFF_HAND) {
				ECPositionCommand.sendBlockPosition(player, hitResult.blockPos)
			}
			ActionResult.PASS
		}
	}
}