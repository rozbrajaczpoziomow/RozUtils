package futbol.rozbrajacz.rozutils.client
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
import futbol.rozbrajacz.rozutils.ConfigHelper
import futbol.rozbrajacz.rozutils.Reference
import futbol.rozbrajacz.rozutils.RozUtils
import futbol.rozbrajacz.rozutils.net.ArrayPacket
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import java.awt.Color

object HUDHandler {
	val coordinates = ConfigHelper({ ConfigHandler.client.hud.position }) {
		val split = it.split(',')
		split[0].toInt() to split[1].toInt()
	}

	val colourParser = { it: String ->
		Color(it.substring(1, 3).toInt(16), it.substring(3, 5).toInt(16), it.substring(5, 7).toInt(16), it.substring(7, 9).toInt(16)).rgb
	}

	val backgroundColour = ConfigHelper({ ConfigHandler.client.hud.backgroundColour }, colourParser)
	val textColour = ConfigHelper({ ConfigHandler.client.hud.textColour }, colourParser)

	@SubscribeEvent(priority = EventPriority.LOWEST)
	fun drawOverlay(ev: RenderGameOverlayEvent.Post) {
		if(!ConfigHandler.client.hud.enabled || ev.type != RenderGameOverlayEvent.ElementType.ALL || ConfigHandler.client.hud.renderF3 != Minecraft.getMinecraft().gameSettings.showDebugInfo)
			return

		val renderer = Minecraft.getMinecraft().fontRenderer

		GlStateManager.pushMatrix()
		GlStateManager.color(1f, 1f, 1f, 1f)
		GlStateManager.enableAlpha()
		GlStateManager.enableBlend()

		val (left, top) = coordinates.value
		val textOffset = (renderer.FONT_HEIGHT + 1)
		val messages = MessageHandler.getMessage()

		GuiScreen.drawRect(left, top, left + messages.maxOf { renderer.getStringWidth(it) } + 1, top + textOffset * messages.size, backgroundColour.value)

		messages.forEachIndexed { idx, str ->
			renderer.drawString(str, left + 1, top + 1 + textOffset * idx, textColour.value)
		}

		GlStateManager.popMatrix()
	}

	@SubscribeEvent
	fun serverJoin(ev: FMLNetworkEvent.ClientConnectedToServerEvent) =
		MessageHandler.reset()

	object MessageHandler {
		private var last = 0L
		private var lastMessage = ConfigHandler.client.hud.text
		private var waiting = false

		fun getMessage(): Array<String> {
			val current = Minecraft.getSystemTime()

			if(waiting && current > last + ConfigHandler.client.hud.refreshInterval * 3)
				return if(lastMessage === ConfigHandler.client.hud.text)
					arrayOf("${Reference.MOD_NAME} is not installed or is disabled on this server")
				else
					arrayOf(*lastMessage, "Server did not respond to our last request,", "Relog to refresh")

			if(waiting || current <= last + ConfigHandler.client.hud.refreshInterval)
				return lastMessage

			waiting = true
			RozUtils.networkChannel.sendToServer(ArrayPacket(ConfigHandler.client.hud.text))
			return lastMessage
		}

		fun handlePacket(packet: ArrayPacket) {
			last = Minecraft.getSystemTime()
			lastMessage = packet.arr
			waiting = false
		}

		fun reset() {
			last = 0L
			lastMessage = ConfigHandler.client.hud.text
			waiting = false
		}
	}
}
