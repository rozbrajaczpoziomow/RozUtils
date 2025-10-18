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
import futbol.rozbrajacz.rozutils.Reference
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.resources.I18n
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.world.GameType
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Keyboard
import java.awt.Color

object GamemodeSwitcherHandler {
	var previouslyWas: GameType? = null
	val mc: Minecraft = Minecraft.getMinecraft()

	fun getGamemode(): GameType = mc.connection!!.getPlayerInfo(mc.player.gameProfile.id).gameType

	@SubscribeEvent
	fun keyInput(e: InputEvent.KeyInputEvent) {
		if(!ConfigHandler.client.gamemodeSwitcher.enabled)
			return

		// if there's no GUI and we were holding down F3 and we've now pressed F4, update and display the GUI
		if(mc.currentScreen == null && Keyboard.isKeyDown(Keyboard.KEY_F3) && Keyboard.getEventKey() == Keyboard.KEY_F4 && Keyboard.getEventKeyState()) {
			if(previouslyWas == null)
				previouslyWas = getGamemode()
			GamemodeSwitcherGUI.instance.selectedGamemode = previouslyWas!!
			mc.displayGuiScreen(GamemodeSwitcherGUI.instance)
		}
	}

	class GamemodeSwitcherGUI : GuiScreen {
		companion object {
			val instance = GamemodeSwitcherGUI()

			// based on the texture
			private const val MAIN_WIDTH = 125
			private const val MAIN_HEIGHT = 75
			private const val SLOT_WIDTH = 26
			private const val SLOT_HEIGHT = 26
			private const val SLOT_OFFSET_X = 3
			private const val SLOT_OFFSET_Y = 27
			private const val SLOT_OFFSET_W = 5
			private const val SLOT_U = 0
			private const val SLOT_V = MAIN_HEIGHT
			private const val ITEM_OFFSET_X = 5
			private const val ITEM_OFFSET_Y = 5
			private const val SELECTED_U = SLOT_U + SLOT_WIDTH
			private const val SELECTED_V = SLOT_V
			private const val TEXT_Y = 7
			private const val TEXT_MIDDLE_X = MAIN_WIDTH shr 1
			private const val TEXT_NEXT_X = 34
			private const val TEXT_NEXT_Y = 60
		}
		private constructor() : super()

		val gamemodes = arrayOf(GameType.CREATIVE, GameType.SURVIVAL, GameType.ADVENTURE, GameType.SPECTATOR)
		val items = arrayOf(Item.getItemFromBlock(Blocks.GRASS), Items.IRON_SWORD, Items.MAP, Items.ENDER_EYE)
		var selectedGamemode
			get() = gamemodes[selectedGamemodeIdx]
			set(value) {
				selectedGamemodeIdx = gamemodes.indexOf(value)
			}
		var selectedGamemodeIdx = 0

		var guiX = 0
		var guiY = 0

		override fun initGui() {
			guiX = (width - MAIN_WIDTH) shr 1
			guiY = (height - MAIN_HEIGHT) shr 1
		}

		override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
			// setup
			GlStateManager.pushMatrix()
			mc.textureManager.bindTexture(ResourceLocation(Reference.MODID, "textures/gui/gamemode_switcher.png"))
			GlStateManager.color(1f, 1f, 1f, 1f)
			GlStateManager.enableAlpha()
			GlStateManager.enableBlend()

			// draw main bg
			drawTexturedModalRect(guiX, guiY, 0, 0, MAIN_WIDTH, MAIN_HEIGHT)

			// draw slots/selected, update selected based on mouse
			repeat(gamemodes.size) {
				val x = guiX + SLOT_OFFSET_X + (SLOT_WIDTH + SLOT_OFFSET_W) * it
				val y = guiY + SLOT_OFFSET_Y
				if(mouseX >= x && mouseY >= y && mouseX < x + SLOT_WIDTH && mouseY < y + SLOT_HEIGHT)
					selectedGamemodeIdx = it
				drawTexturedModalRect(x, y, SLOT_U, SLOT_V, SLOT_WIDTH, SLOT_HEIGHT)
				if(it == selectedGamemodeIdx)
					drawTexturedModalRect(x, y, SELECTED_U, SELECTED_V, SLOT_WIDTH, SLOT_HEIGHT)
			}

			// draw "Spectator Mode"/equivalents at the top of the GUI
			drawCenteredString(fontRenderer, I18n.format("gameMode.${selectedGamemode.getName()}"), guiX + TEXT_MIDDLE_X, guiY + TEXT_Y, Color.white.rgb)

			// draw the "[ F4 ] Next" text at the bottom of the gui (there's no translation for these in 1.12.2)
			// - there is a correct way of doing this, but I'm lazy, so TextFormatting it is
			drawString(fontRenderer, "§b[ F4 ]§r Next", guiX + TEXT_NEXT_X, guiY + TEXT_NEXT_Y, Color.white.rgb)

			// draw items in each slot
			RenderHelper.enableGUIStandardItemLighting()
			repeat(items.size) {
				val x = guiX + SLOT_OFFSET_X + ITEM_OFFSET_X + (SLOT_WIDTH + SLOT_OFFSET_W) * it
				val y = guiY + SLOT_OFFSET_Y + ITEM_OFFSET_Y
				itemRender.renderItemAndEffectIntoGUI(ItemStack(items[it]), x, y)
			}
			RenderHelper.disableStandardItemLighting()

			// finish
			GlStateManager.popMatrix()
		}

		override fun doesGuiPauseGame() = false

		override fun handleKeyboardInput() {
			// if we stop holding down F3, switch to whichever gamemode we have selected and hide it
			if(Keyboard.getEventKey() == Keyboard.KEY_F3 && !Keyboard.getEventKeyState()) {
				val currentGamemode = getGamemode()
				if(currentGamemode != selectedGamemode) {
					previouslyWas = currentGamemode
					sendChatMessage("/gamemode ${selectedGamemode.getName()}", false)
				}
				mc.displayGuiScreen(null)
			// if we press *down* F4, go to the next gamemode in the list
			} else if(Keyboard.getEventKey() == Keyboard.KEY_F4 && Keyboard.getEventKeyState())
				selectedGamemodeIdx = (selectedGamemodeIdx + 1) % gamemodes.size
		}
	}
}
