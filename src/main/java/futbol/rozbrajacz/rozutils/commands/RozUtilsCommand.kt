@file:Suppress("NOTHING_TO_INLINE")

package futbol.rozbrajacz.rozutils.commands
/*
	Copyright (c) rozbrajaczpoziomow

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU Affero General Public License version 3
	as published by the Free Software Foundation.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Affero General Public License for more details.

	You should have received a copy of the GNU Affero General Public License
	along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

import futbol.rozbrajacz.rozutils.ConfigHandler
import futbol.rozbrajacz.rozutils.RozUtils
import net.minecraft.command.CommandBase
import net.minecraft.command.CommandException
import net.minecraft.command.ICommandSender
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityList
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.Style
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.TextFormatting.*
import net.minecraft.util.text.event.ClickEvent
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.server.command.CommandTreeBase
import java.util.*

object RozUtilsCommand : CommandTreeBase() {
	override fun getName() =
		"rozutils"

	override fun getAliases() =
		listOf("ru")

	init {
		addSubcommand(Help)
		addSubcommand(Stats)
		addSubcommand(Entities)
		addSubcommand(Players)
		addSubcommand(Version)
	}

	override fun getUsage(sender: ICommandSender) =
		"meow"

	// technically only used in CommandBase but doesn't hurt
	override fun getRequiredPermissionLevel() =
		if(ConfigHandler.server.command.op)
			4
		else
			0

	override fun checkPermission(server: MinecraftServer, sender: ICommandSender): Boolean {
		if(!ConfigHandler.server.command.op || server.isSinglePlayer)
			return true

		return sender !is EntityPlayer || server.playerList.oppedPlayers.getPermissionLevel(sender.gameProfile) != 0
	}

	object Help : CommandBase() {
		override fun getName() =
			"help"

		override fun getUsage(sender: ICommandSender) =
			"$name meow"

		override fun execute(server: MinecraftServer, sender: ICommandSender, args: Array<out String?>) {
			sender.reply("/rozutils help - send help")
			sender.reply("/rozutils stats - show TPS/MSPT stats for the server, your current dimension and the dimension with the longest MSPT", "stats")
			sender.reply("/rozutils stats [...dimensionIds] - show TPS/MSPT stats for the specified dimensions")
			sender.reply("/rozutils entities - show entity stats for the server, your current dimension and the dimension with the most entities", "entities")
			sender.reply("/rozutils entities [dimensionId] - show a list of entities in the specified dimension")
			sender.reply("/rozutils entities [dimensionId] [entity] - show a chunk breakdown for the specified entity being present in the specified dimension")
			sender.reply("/rozutils players - show currently online players, operator status, gamemode, dimension and coordinates", "players")
			sender.reply("/rozutils version - show currently running RozUtils version", "version")
			sender.reply("You can also click on most messages to automatically run a more interesting command, if there is one.")
		}
	}

	object Stats : CommandBase() {
		// leaving original format comment I wrote:
		// Server: %.2f (coloured, 20 - green, != 20 - yellow, < 18 - red) TPS, %.1f ms/t (coloured, < 50 - green, < 55 - yellow, else - red) average
		// Current dim (%d %s {name}): {^}
		// Worst dim (%d %s): {^} {worst in terms of slowest mspt}

		override fun getName() =
			"stats"

		override fun getUsage(sender: ICommandSender) =
			"$name meow"

		// code similar to net/ServerHandler.kt which was itself stolen from forge's TpsCommand
		private inline fun LongArray.formatTPSData(): String {
			val mspt = average() * 1e-6
			val tps = (1000 / mspt).coerceAtMost(20.0)
			return "%s%.2f$RESET TPS, %s%.1f$RESET mspt average".format(
				if(tps < 18) RED else if(tps < 20) YELLOW else GREEN,
				tps,
				if(mspt < 50) GREEN else if(mspt < 55) YELLOW else RED,
				mspt
			)
		}

		override fun execute(server: MinecraftServer, sender: ICommandSender, args: Array<out String>) {
			// /rozutils stats - overall info
			// /rozutils stats <...dims> - info for specified dimensions
			if(args.isNotEmpty()) {
				args.forEach {
					val id = it.int()
					val times = server.worldTickTimes[id] ?: throw CommandException("Dimension $id is not loaded")
					sender.reply("Dim ${id.getDimInfo()}: ${times.formatTPSData()}", "stats $id")
				}
				return
			}

			sender.reply("Server: ${server.tickTimeArray.formatTPSData()}")
			sender.reply("Loaded dimensions: ${server.worlds.size}")

			sender.commandSenderEntity?.dimension?.let { dimId ->
				sender.reply("Current dim (${dimId.getDimInfo()}): ${server.worldTickTimes[dimId]!!.formatTPSData()}", "stats $dimId")
			}

			val worstDim = server.worldTickTimes.maxBy { it.value.average() }
			sender.reply("Worst dim (${worstDim.key.getDimInfo()}): ${worstDim.value.formatTPSData()}", "stats ${worstDim.key}")
		}
	}

	object Entities : CommandBase() {
		override fun getName() =
			"entities"

		override fun getUsage(sender: ICommandSender) =
			"$name meow"

		private inline fun <T : Any>List<T>.stat(): List<Pair<Class<out T>, Int>> {
			val frequency = hashMapOf<Class<out T>, Int>()
			forEach {
				frequency.compute(it::class.java) { k, v -> (v ?: 0) + 1 }
			}
			return frequency.entries.sortedByDescending { it.value }.map { it.key to it.value }
		}

		private inline fun Pair<Class<out Entity>, Int>.fmt() = "${second}x ${first.name()}"

		private inline fun Class<out Entity>.name() = EntityList.getKey(this) ?: (if(EntityPlayer::class.java.isAssignableFrom(this)) "minecraft:player" else simpleName)

		private inline fun List<Entity>.getEntityStat() = "$size entit${size.s("y", "ies")} (most: ${stat()[0].fmt()})"

		override fun execute(server: MinecraftServer, sender: ICommandSender, args: Array<out String>) {
			// /rozutils entities - overall info
			// /rozutils entities <dim> - dimension-specific entity list
			// /rozutils entities <dim> <entity> - dimension-specific entity-specific chunk appearance list
			if(args.isNotEmpty()) {
				val id = args[0].int()
				if(!DimensionManager.isDimensionRegistered(id))
					throw CommandException("Dimension $id is not loaded")

				val worldEntities = server.getWorld(id).loadedEntityList

				if(args.size > 1) {
					val ourEntity = EntityList.getClass(ResourceLocation(args[1])) ?: throw CommandException("Entity '${args[1]}' doesn't exist")
					val entities = worldEntities.filter { it::class.java === ourEntity }
					sender.reply("Dim ${id.getDimInfo()} - ${entities.size} ${ourEntity.name()} entit${entities.size.s("y", "ies")}")
					val chunks = entities.map { it.chunkCoordX to it.chunkCoordZ }
					// cannot use stat() as it has to compare by class instead of hashcode
					val frequency = hashMapOf<Pair<Int, Int>, Int>()
					chunks.forEach {
						frequency.compute(it) { k, v -> (v ?: 0) + 1 }
					}
					frequency.entries.sortedByDescending { it.value }.forEach { (pair, cnt) ->
						val x = pair.first shl 4
						val z = pair.second shl 4
						sender.replyOther("Chunk ${pair.first} ${pair.second} - $cnt entit${cnt.s("y", "ies")} [Chunk X: $x Z: $z]", "$x ~ $z".tp(id, sender))
					}
					return
				}

				val stat = worldEntities.stat()
				sender.reply("Dim ${id.getDimInfo()} - ${worldEntities.size} entit${worldEntities.size.s("y", "ies")}")
				val padding = " ".repeat(stat[0].second.toString().length - 1)
				stat.forEach { (entity, count) ->
					sender.reply("$padding${count}x ${entity.name()}", "entities $id ${entity.name()}")
				}
				return
			}

			val serverEntityList = server.worlds.flatMap { dim -> dim.loadedEntityList }
			sender.reply("Server: ${serverEntityList.getEntityStat()}")

			sender.commandSenderEntity?.dimension?.let { dimId ->
				val entityList = server.getWorld(dimId).loadedEntityList
				sender.reply("Current dim (${dimId.getDimInfo()}): ${entityList.getEntityStat()}", "entities $dimId")
			}

			val worstDim = DimensionManager.getIDs().maxBy { server.getWorld(it).loadedEntityList.size }
			val entityList = DimensionManager.getWorld(worstDim).loadedEntityList
			sender.reply("Worst dim (${worstDim.getDimInfo()}): ${entityList.getEntityStat()}", "entities $worstDim")
		}
	}

	object Players : CommandBase() {
		override fun getName() =
			"players"

		override fun getUsage(sender: ICommandSender) =
			"$name meow"

		override fun execute(server: MinecraftServer, sender: ICommandSender, args: Array<out String>) {
			// /rozutils players - overall info
			server.playerList.players.forEach {
				sender.replyOther("${if(server.isSinglePlayer || server.playerList.oppedPlayers.getPermissionLevel(it.gameProfile) != 0) GOLD else ""}${it.name}$RESET [${it.interactionManager.gameType.shortName.replaceFirstChar(Char::uppercaseChar)}], ${it.position.x} ${it.position.y} ${it.position.z} in dim ${it.dimension.getDimInfo()}", "/tp @s ${it.uniqueID}")
			}
		}
	}

	object Version : CommandBase() {
		override fun getName() =
			"version"

		override fun getUsage(sender: ICommandSender) =
			"$name meow"

		override fun execute(server: MinecraftServer, sender: ICommandSender, args: Array<out String?>) {
			// /rozutils version - server version info (client version info provided by ClientEventHandler#runCommand)
			sender.reply(RozUtils.formatVersion("Server"))
		}
	}
}

internal inline fun ICommandSender.reply(msg: String) =
	sendMessage(TextComponentString(msg))

internal inline fun ICommandSender.reply(msg: String, command: String) =
	replyOther(msg, "/rozutils $command")

internal inline fun ICommandSender.replyOther(msg: String, command: String) =
	sendMessage(TextComponentString(msg).setStyle(Style().setClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, command))))

private inline fun Int.getDimInfo(): String {
	val provider = DimensionManager.getProviderType(this)
	return toString() + (if(provider == null) "" else " ${provider.name.lowercase(Locale.getDefault())}")
}

private inline fun String.int(): Int {
	try {
		return toInt()
	} catch(_: Exception) {
		throw CommandException("'$this' is not a number")
	}
}

private inline fun Int.s(singular: String, plural: String) =
	if(this == 1)
		singular
	else
		plural

private inline fun String.tp(dim: Int, sender: ICommandSender) =
	if(sender is EntityPlayer)
		if(sender.dimension == dim)
			"/tp @s $this"
		else
			"/forge setdim @s $dim $this"
	else
		""
