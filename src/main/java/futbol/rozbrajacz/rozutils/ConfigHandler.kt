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

import net.minecraftforge.common.config.Config
import net.minecraftforge.common.config.ConfigManager
import net.minecraftforge.fml.client.event.ConfigChangedEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@Config(modid = Reference.MODID, name = Reference.MODID)
object ConfigHandler {
	@JvmField
	@Config.Name("Client settings")
	val client = Client()

	class Client {
		@JvmField
		@Config.Name("HUD settings")
		val hud = HUD()

		class HUD {
			@JvmField
			@Config.Name("Enable the HUD")
			var enabled = false

			@JvmField
			@Config.Name("HUD Position")
			@Config.Comment("Coordinates for the top-left of the HUD (x,y)")
			var position = "0,0"

			@JvmField
			@Config.Name("HUD Background Colour")
			@Config.Comment("Colour of the background behind the HUD (#RRGGBBAA)")
			var backgroundColour = "#7F7F7F7F"

			@JvmField
			@Config.Name("HUD Text Colour")
			@Config.Comment("Colour of the text (#RRGGBBAA)")
			var textColour = "#F0F0F0FF"

			@JvmField
			@Config.Name("HUD Text")
			@Config.Comment(
				"Text displayed in the HUD",
				"Available formats:", // when you change this, remember to change server.hud.formats
				"- Server-wide: {tps}, {mspt}, {tick}, {ram_used}, {ram_max}, {cpu_usage}, {ping}",
				"- Current dimension: {dim_id}, {dim_tps}, {dim_mspt}, {dim_entity_count}, {dim_tile_entity_count}, {dim_chunk_count}",
				"- Current chunk: {ch_entity_count}, {ch_tile_entity_count}"
			)
			var text = arrayOf(
				"TPS {tps}, {mspt} ms",
				"E {dim_entity_count} TE {dim_tile_entity_count} Ch {dim_chunk_count}",
				"RAM {ram_used} / {ram_max} GB; CPU {cpu_usage}%"
			)

			@JvmField
			@Config.Name("Show HUD with F3")
			@Config.Comment(
				"If enabled, the HUD is only shown when the F3 menu is open, if disabled, the HUD is hidden when the F3 menu is open",
				"If you choose to enable this, I'd recommend changing the default HUD position to not interfere with the F3 menu"
			)
			var renderF3 = false

			@JvmField
			@Config.Name("Refresh Interval")
			@Config.Comment("Refresh the data that gets shown on the client every x (ms)")
			@Config.RangeInt(min = 100, max = 5000)
			var refreshInterval = 500
		}

		@JvmField
		@Config.Name("Gamemode Switcher settings")
		val gamemodeSwitcher = GamemodeSwitcher()

		class GamemodeSwitcher {
			@JvmField
			@Config.Name("Enable the Gamemode Switcher from 1.16+")
			var enabled = true

			@JvmField
			@Config.Name("Change the keybind in the Controls screen instead of the default F3+F4")
			@Config.RequiresMcRestart
			var changeKeybind = false
		}
	}

	@JvmField
	@Config.Name("Server settings")
	val server = Server()

	class Server {
		@JvmField
		@Config.Name("HUD settings")
		val hud = HUD()

		class HUD {
			@JvmField
			@Config.Name("Enable the HUD")
			@Config.Comment("Allows the usage of the HUD for clients")
			var enabled = true

			@JvmField
			@Config.RequiresMcRestart
			@Config.Name("Enabled formats")
			@Config.Comment(
				"Formats allowed to use, separated by commas (,)", // this is not an `arrayOf()` because it's just simpler
				"Possible formats:", // when you change this, remember to change client.hud.text
				"- Server-wide: tps, mspt, tick, ram_used, ram_max, cpu_usage, ping",
				"- Current dimension: dim_id, dim_tps, dim_mspt, dim_entity_count, dim_tile_entity_count, dim_chunk_count",
				"- Current chunk: ch_entity_count, ch_tile_entity_count"
			)
			var formats = "tps,mspt,tick,ram_used,ram_max,cpu_usage,ping,dim_id,dim_tps,dim_mspt,dim_chunk_count,ch_entity_count,ch_tile_entity_count"

			@JvmField
			@Config.RequiresMcRestart
			@Config.Name("Operators bypass enabled formats")
			@Config.Comment("Make operators bypass the enabled formats")
			var opBypass = true
		}

		@JvmField
		@Config.Name("Command settings")
		val command = Command()

		class Command {
			@JvmField
			@Config.RequiresMcRestart
			@Config.Name("Enable the /rozutils command")
			var enabled = true

			@JvmField
			@Config.Name("Require operator permissions")
			@Config.Comment("Only allow usage of the /rozutils command for operators, highly recommended.")
			var op = true
		}
	}

	@Mod.EventBusSubscriber(modid = Reference.MODID)
	object ConfigEventHandler {
		@SubscribeEvent
		fun onConfigChangedEvent(event: ConfigChangedEvent.OnConfigChangedEvent) {
			if(event.modID == Reference.MODID)
				ConfigManager.sync(Reference.MODID, Config.Type.INSTANCE)
		}
	}
}
