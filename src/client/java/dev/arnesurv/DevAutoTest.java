package dev.arnesurv;

import dev.arnesurv.config.ModConfig;
import dev.arnesurv.gui.HudEditorScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.nio.file.Files;

/**
 * Nur für die Entwicklung: Wenn im run-Ordner eine Datei "arnesurv-autotest" liegt,
 * macht der Mod in einer Welt automatisch Screenshots von Minimap & Co. und beendet das Spiel.
 * Im normalen Spiel ist das komplett inaktiv.
 */
final class DevAutoTest {
	private static Boolean active;
	private static int ticks;

	private DevAutoTest() {
	}

	static void tick(Minecraft mc) {
		if (active == null) {
			active = FabricLoader.getInstance().isDevelopmentEnvironment()
				&& Files.exists(mc.gameDirectory.toPath().resolve("arnesurv-autotest"));
		}
		if (!active) return;
		if (mc.player == null || mc.level == null) {
			createWorldIfOnTitle(mc);
			return;
		}
		ModConfig c = ModConfig.get();
		mc.options.pauseOnLostFocus = false;
		if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.PauseScreen) mc.gui.setScreen(null);
		ticks++;
		switch (ticks) {
			case 1 -> {
				c.minimapRound = false;
				c.minimapRotate = true;
				c.minimapCaveMode = "OFF";
				c.overlaySlime = true;
				c.overlayGridMinimap = true;
				placeTestBlocks(mc);
			}
			case 60 -> {
				// Die Testblöcke wurden erst nach dem ersten Scan gesetzt – Chunks erneut prüfen
				for (int[] ch : testChunks) dev.arnesurv.overlay.Structures.devRescan(mc.level, ch[0], ch[1]);
			}
			case 200 -> shot(mc, "1_eckig");
			case 210 -> {
				c.minimapRound = true;
				mc.player.setYRot(45f);
			}
			case 240 -> shot(mc, "2_rund_gedreht");
			case 250 -> mc.gui.setScreen(new HudEditorScreen(null));
			case 270 -> shot(mc, "3_editor");
			case 280 -> {
				mc.gui.setScreen(null);
				c.minimapCaveMode = "ON";
				c.minimapRound = false;
			}
			case 380 -> shot(mc, "4_hoehle");
			case 390 -> {
				c.minimapCaveMode = "AUTO";
				mc.gui.setScreen(new dev.arnesurv.worldmap.WorldMapScreen(null, null));
			}
			case 430 -> shot(mc, "5_weltkarte");
			case 440 -> {
				mc.gui.setScreen(null);
				ModConfig.save();
				mc.stop();
			}
			default -> {
			}
		}
	}

	/** Setzt eine Glocke und einen Spawner in einen noch nicht gescannten Chunk, um die Strukturerkennung zu testen. */
	private static void placeTestBlocks(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		int px = mc.player.getBlockX(), pz = mc.player.getBlockZ();
		server.execute(() -> {
			var level = server.overworld();
			level.getChunk((px + 40) >> 4, pz >> 4); // Chunk laden, damit die Höhe stimmt
			var bell = new net.minecraft.core.BlockPos(px + 40, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, px + 40, pz), pz);
			level.setBlockAndUpdate(bell, net.minecraft.world.level.block.Blocks.BELL.defaultBlockState());
			var spawner = new net.minecraft.core.BlockPos(px - 30, 20, pz + 30);
			level.setBlockAndUpdate(spawner, net.minecraft.world.level.block.Blocks.SPAWNER.defaultBlockState());
		});
		testChunks = new int[][]{{(px + 40) >> 4, pz >> 4}, {(px - 30) >> 4, (pz + 30) >> 4}};
	}

	private static int[][] testChunks;

	private static int titleTicks;
	private static boolean createRequested;

	/** Öffnet "Neue Welt" und klickt auf Erstellen (per Reflection, nur Entwicklung). */
	private static void createWorldIfOnTitle(Minecraft mc) {
		var screen = mc.gui.screen();
		if (screen instanceof net.minecraft.client.gui.screens.TitleScreen && !createRequested) {
			if (++titleTicks > 40) {
				createRequested = true;
				net.minecraft.client.gui.screens.worldselection.CreateWorldScreen.openFresh(mc, () -> {
				});
			}
		} else if (screen instanceof net.minecraft.client.gui.screens.worldselection.CreateWorldScreen cws) {
			if (++titleTicks > 100) {
				titleTicks = -100000;
				try {
					var m = cws.getClass().getDeclaredMethod("onCreate");
					m.setAccessible(true);
					m.invoke(cws);
				} catch (Exception e) {
					ArneSurv.LOGGER.error("Autotest: Welt erstellen fehlgeschlagen", e);
				}
			}
		}
	}

	private static void shot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, "autotest_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, msg -> {
		});
	}
}
