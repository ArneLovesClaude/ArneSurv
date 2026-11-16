package dev.arnesurv;

import dev.arnesurv.comfort.InventoryTweaks;
import dev.arnesurv.comfort.ScreenshotScreen;
import dev.arnesurv.comfort.Zoom;
import dev.arnesurv.config.ModConfig;
import dev.arnesurv.gui.ArneSurvScreen;
import dev.arnesurv.stats.StatsScreen;
import dev.arnesurv.waypoint.Waypoint;
import dev.arnesurv.waypoint.WaypointManager;
import dev.arnesurv.waypoint.WaypointShare;
import dev.arnesurv.worldmap.WorldMapScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Nur für die Entwicklung: Wenn im run-Ordner eine Datei "arnesurv-autotest" liegt,
 * erstellt der Mod eine Testwelt, probiert alle Funktionen aus, macht Screenshots und beendet das Spiel.
 * Im normalen Spiel ist das komplett inaktiv.
 */
final class DevAutoTest {
	private static Boolean active;
	private static int ticks;
	private static int[][] testChunks;

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
				c.overlayGridMinimap = false;
				setupWorld(mc);
				// Test-Waypoint 25 Blöcke vor dem Spieler
				var p = mc.player;
				WaypointManager.add(new Waypoint("Testbasis", p.getBlockX(), p.getBlockY(), p.getBlockZ() + 25,
					mc.level.dimension().identifier().toString(), 0xFFFFAA00));
				p.setYRot(0f);
				p.setXRot(10f);
			}
			case 60 -> {
				for (int[] ch : testChunks) dev.arnesurv.overlay.Structures.devRescan(mc.level, ch[0], ch[1]);
			}
			case 100 -> shot(mc, "01_hud_marker");
			case 105 -> Zoom.devForce = true;
			case 125 -> shot(mc, "02_zoom");
			case 130 -> Zoom.devForce = false;
			case 140 -> mc.gui.setScreen(new InventoryScreen(mc.player));
			case 150 -> {
				logInventory(mc, "vorher");
				shot(mc, "03_inventar_vorher");
			}
			case 155 -> InventoryTweaks.devSortPlayer(mc);
			case 230 -> {
				logInventory(mc, "nachher");
				shot(mc, "04_inventar_nachher");
			}
			case 240 -> {
				mc.gui.setScreen(null);
				Waypoint w = new Waypoint("Diamanten", 120, -54, -300, "minecraft:overworld", 0xFF55FFFF);
				mc.player.sendSystemMessage(Component.literal("<Kollege> " + WaypointShare.message(w)));
				mc.player.sendSystemMessage(Component.literal("<Kollege> hey " + mc.player.getGameProfile().name() + ", komm mal her"));
				mc.player.sendSystemMessage(Component.literal("<Kollege> normale Nachricht"));
			}
			case 250 -> mc.gui.setScreen(new ChatScreen("", false));
			case 265 -> shot(mc, "05_chat");
			case 270 -> {
				mc.gui.setScreen(null);
				// geteilten Waypoint per Klick-Befehl übernehmen
				mc.getConnection().sendCommand("arnesurv waypoint add 120 -54 -300 \"minecraft:overworld\" Diamanten");
			}
			case 280 -> {
				boolean ok = WaypointManager.all().stream().anyMatch(w -> w.name.equals("Diamanten"));
				ArneSurv.LOGGER.info("AUTOTEST Waypoint uebernommen: {}", ok);
				mc.gui.setScreen(new ArneSurvScreen(null));
			}
			case 285 -> {
				String n = mc.player.getGameProfile().name();
				ArneSurv.LOGGER.info("AUTOTEST Erwaehnung: chat={} system={} selbst={} teilwort={}",
					dev.arnesurv.comfort.ChatTweaks.isMention("<Kollege> hey " + n + ", komm", n),
					dev.arnesurv.comfort.ChatTweaks.isMention(n + " has made the advancement", n),
					dev.arnesurv.comfort.ChatTweaks.isMention("<" + n + "> ich bin " + n, n),
					dev.arnesurv.comfort.ChatTweaks.isMention("<Kollege> " + n + "xyz", n));
			}
			case 295 -> {
				shot(mc, "06_menue");
				mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(
					net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.REQUEST_STATS));
			}
			case 300 -> {
				var d = dev.arnesurv.stats.StatsRecorder.data();
				ArneSurv.LOGGER.info("AUTOTEST Statistik: {} bekannte Werte, Session-Summe: {}", d.lastKnown.size(),
					dev.arnesurv.stats.StatsRecorder.sumSince(dev.arnesurv.stats.StatsRecorder.sessionStart()));
				mc.gui.setScreen(new StatsScreen(null));
			}
			case 320 -> shot(mc, "07_statistik");
			case 325 -> mc.gui.setScreen(new ScreenshotScreen(null));
			case 345 -> shot(mc, "08_screenshots");
			case 350 -> mc.gui.setScreen(new WorldMapScreen(null, null));
			case 380 -> shot(mc, "09_weltkarte");
			case 390 -> {
				mc.gui.setScreen(null);
				setupComfortTests(mc);
				var p = mc.player;
				WaypointManager.add(new Waypoint("Ferne Basis", p.getBlockX() + 25, p.getBlockY(), p.getBlockZ() + 120,
					mc.level.dimension().identifier().toString(), 0xFF55FFFF));
			}
			// --- Auto-Tool: Stein vor dem Spieler, Spitzhacke in Slot 3 ---
			case 410 -> {
				mc.player.getInventory().setSelectedSlot(0);
				mc.player.setYRot(0f);
				mc.player.setXRot(4f);
			}
			case 415 -> mc.options.keyAttack.setDown(true);
			case 425 -> ArneSurv.LOGGER.info("AUTOTEST AutoTool beim Abbauen: Slot {} ({}) – erwartet 2 (Spitzhacke), Ziel: {}",
				mc.player.getInventory().getSelectedSlot(), mc.player.getInventory().getSelectedItem().getItem(), mc.hitResult);
			case 430 -> mc.options.keyAttack.setDown(false);
			case 435 -> ArneSurv.LOGGER.info("AUTOTEST AutoTool danach: Slot {} – erwartet 0",
				mc.player.getInventory().getSelectedSlot());
			// --- Einlagern in eine Kiste ---
			case 440 -> openTestChest(mc);
			case 455 -> InventoryTweaks.devQuickStack(mc);
			case 480 -> {
				logChest(mc);
				shot(mc, "10_kiste_eingelagert");
			}
			// --- Sneak bleibt im Inventar ---
			case 490 -> {
				mc.gui.setScreen(null);
				mc.options.toggleCrouch().set(false);
				mc.options.keyShift.setDown(true);
			}
			case 500 -> mc.gui.setScreen(new InventoryScreen(mc.player));
			case 510 -> ArneSurv.LOGGER.info("AUTOTEST Sneak im Inventar: {} – erwartet true", mc.player.isShiftKeyDown());
			case 515 -> mc.gui.setScreen(null);
			case 522 -> ArneSurv.LOGGER.info("AUTOTEST Sneak nach dem Schließen: {} – erwartet false (Taste nicht gedrückt)",
				mc.options.keyShift.isDown());
			// --- Statistik-Aufzeichnung löschen ---
			case 525 -> {
				int removed = dev.arnesurv.stats.StatsRecorder.deleteSince(0L);
				ArneSurv.LOGGER.info("AUTOTEST Statistik geloescht: {} Minuten-Pakete, Rest: {}", removed,
					dev.arnesurv.stats.StatsRecorder.sumSince(0L));
			}
			// --- Lichtsäulen + Chat-Formate ---
			case 530 -> {
				mc.player.setYRot(10f);
				mc.player.setXRot(-15f);
			}
			case 545 -> shot(mc, "11_lichtsaeulen");
			case 550 -> {
				String n = mc.player.getGameProfile().name();
				ArneSurv.LOGGER.info("AUTOTEST Chatformate: doppelpunkt={} rang={} pfeil={} system={} tod={}",
					dev.arnesurv.comfort.ChatTweaks.isMention("Kollege: hi " + n, n),
					dev.arnesurv.comfort.ChatTweaks.isMention("[Admin] Kollege » " + n + " komm", n),
					dev.arnesurv.comfort.ChatTweaks.isMention("Kollege > yo " + n, n),
					dev.arnesurv.comfort.ChatTweaks.isMention(n + " joined the game", n),
					dev.arnesurv.comfort.ChatTweaks.isMention(n + " was slain by Zombie", n));
			}
			case 560 -> {
				c.minimapCaveMode = "AUTO";
				c.overlaySlime = false;
				ModConfig.save();
				mc.stop();
			}
			default -> {
			}
		}
	}

	private static void logInventory(Minecraft mc, String when) {
		List<String> items = new ArrayList<>();
		for (int i = 9; i < 36; i++) {
			ItemStack s = mc.player.getInventory().getItem(i);
			items.add(s.isEmpty() ? "-" : s.getItem().toString().replace("minecraft:", "") + "x" + s.getCount());
		}
		ArneSurv.LOGGER.info("AUTOTEST Inventar {}: {}", when, String.join(" ", items));
	}

	/** Testblöcke für die Strukturerkennung und ein unordentliches Inventar. */
	private static void setupWorld(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		int px = mc.player.getBlockX(), pz = mc.player.getBlockZ();
		server.execute(() -> {
			var level = server.overworld();
			level.getChunk((px + 40) >> 4, pz >> 4);
			var bell = new BlockPos(px + 40, level.getHeight(Heightmap.Types.WORLD_SURFACE, px + 40, pz), pz);
			level.setBlockAndUpdate(bell, Blocks.BELL.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(px - 30, 20, pz + 30), Blocks.SPAWNER.defaultBlockState());

			var sp = server.getPlayerList().getPlayers().get(0);
			var inv = sp.getInventory();
			Object[][] items = {
				{12, Items.COBBLESTONE, 20}, {30, Items.DIRT, 64}, {15, Items.COBBLESTONE, 50},
				{9, Items.DIAMOND, 3}, {33, Items.OAK_LOG, 16}, {20, Items.DIRT, 10},
				{25, Items.COBBLESTONE, 64}, {11, Items.BREAD, 5}, {28, Items.DIAMOND, 2},
				{18, Items.OAK_LOG, 40}, {35, Items.TORCH, 32}, {22, Items.BREAD, 7}
			};
			for (Object[] it : items) {
				inv.setItem((Integer) it[0], new ItemStack((net.minecraft.world.item.Item) it[1], (Integer) it[2]));
			}
			sp.inventoryMenu.sendAllDataToRemote();
			// Status-Effekte, damit man sieht, ob die Minimap ausweicht
			sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 20 * 120));
			sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 20 * 120));
		});
		testChunks = new int[][]{{(px + 40) >> 4, pz >> 4}, {(px - 30) >> 4, (pz + 30) >> 4}};
	}

	private static BlockPos chestPos;

	/** Stein vor dem Spieler, Werkzeuge in der Hotbar, Kiste mit einem Bruchstein daneben. */
	private static void setupComfortTests(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null) return;
		BlockPos p = mc.player.blockPosition();
		chestPos = p.offset(-2, 0, 0);
		BlockPos stone = p.offset(0, 1, 2);
		server.execute(() -> {
			var level = server.overworld();
			level.setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
			level.setBlockAndUpdate(stone.above(), Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(p.offset(0, 1, 1), Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
			if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
				chest.setItem(0, new ItemStack(Items.COBBLESTONE, 1));
			}
			var sp = server.getPlayerList().getPlayers().get(0);
			var inv = sp.getInventory();
			inv.setItem(0, new ItemStack(Items.DIRT, 16));
			inv.setItem(1, new ItemStack(Items.DIAMOND_SHOVEL));
			inv.setItem(2, new ItemStack(Items.DIAMOND_PICKAXE));
			inv.setItem(3, new ItemStack(Items.DIAMOND_AXE));
			sp.inventoryMenu.sendAllDataToRemote();
		});
	}

	private static void openTestChest(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null || chestPos == null) return;
		server.execute(() -> {
			var sp = server.getPlayerList().getPlayers().get(0);
			if (server.overworld().getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
				sp.openMenu(chest);
			}
		});
	}

	private static void logChest(Minecraft mc) {
		if (!(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> acs)) {
			ArneSurv.LOGGER.info("AUTOTEST Kiste: kein Kistenfenster offen ({})", mc.gui.screen());
			return;
		}
		int inChest = 0, inPlayer = 0;
		for (var slot : acs.getMenu().slots) {
			ItemStack s = slot.getItem();
			if (!s.is(Items.COBBLESTONE)) continue;
			if (slot.container instanceof net.minecraft.world.entity.player.Inventory) inPlayer += s.getCount();
			else inChest += s.getCount();
		}
		ArneSurv.LOGGER.info("AUTOTEST Einlagern: Bruchstein in Kiste {} (vorher 1), im Inventar {} (vorher 134)", inChest, inPlayer);
	}

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
