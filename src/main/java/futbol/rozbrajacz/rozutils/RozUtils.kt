package futbol.rozbrajacz.rozutils
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

import futbol.rozbrajacz.rozutils.client.ClientEventHandler
import futbol.rozbrajacz.rozutils.client.GamemodeSwitcherHandler
import futbol.rozbrajacz.rozutils.client.HUDHandler
import futbol.rozbrajacz.rozutils.commands.RozUtilsCommand
import futbol.rozbrajacz.rozutils.net.ArrayPacket
import futbol.rozbrajacz.rozutils.net.ClientHandler
import futbol.rozbrajacz.rozutils.net.ServerHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.event.FMLServerStartingEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side

@Mod(
	modid = Reference.MODID,
	name = Reference.MOD_NAME,
	version = Reference.VERSION,
	dependencies = RozUtils.DEPENDENCIES,
	modLanguageAdapter = "io.github.chaosunity.forgelin.KotlinAdapter",
	acceptableRemoteVersions = "*"
)
object RozUtils {
	const val DEPENDENCIES = "required-after:forgelin_continuous@[${Reference.KOTLIN_VERSION},);"

	val networkChannel: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MODID)

	@Suppress("unused")
	@Mod.EventHandler
	fun preInit(e: FMLPreInitializationEvent) {
		if(FMLCommonHandler.instance().effectiveSide.isClient) {
			MinecraftForge.EVENT_BUS.register(HUDHandler)
			MinecraftForge.EVENT_BUS.register(GamemodeSwitcherHandler)
			MinecraftForge.EVENT_BUS.register(ClientEventHandler)
		}
		networkChannel.registerMessage(ServerHandler::class.java, ArrayPacket::class.java, 0, Side.SERVER)
		networkChannel.registerMessage(ClientHandler::class.java, ArrayPacket::class.java, 0, Side.CLIENT)
	}

	@Suppress("unused")
	@Mod.EventHandler
	fun init(e: FMLInitializationEvent) {
		if(FMLCommonHandler.instance().effectiveSide.isClient)
			GamemodeSwitcherHandler.addKeybinds()
	}

	@Mod.EventHandler
	fun serverStarting(e: FMLServerStartingEvent) {
		if(ConfigHandler.server.command.enabled)
			e.registerServerCommand(RozUtilsCommand)
	}

	const val VERSION = "${Reference.MOD_NAME} [%s] ${Reference.VERSION} (${Reference.GIT_COMMIT_HASH_SHORT}) built on ${Reference.BUILD_DATE}"

	internal fun formatVersion(side: String) =
		VERSION.format(side)
}
