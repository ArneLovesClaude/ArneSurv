package dev.arnesurv;

import com.mojang.blaze3d.platform.InputConstants;
import dev.arnesurv.comfort.AutoTool;
import dev.arnesurv.comfort.InventoryTweaks;
import dev.arnesurv.comfort.SneakKeeper;
import dev.arnesurv.comfort.Zoom;
import dev.arnesurv.config.ModConfig;
import dev.arnesurv.gui.ArneSurvScreen;
import dev.arnesurv.gui.WaypointEditScreen;
import dev.arnesurv.gui.WaypointListScreen;
import dev.arnesurv.hud.CompassHud;
import dev.arnesurv.hud.InfoHud;
import dev.arnesurv.hud.WaypointMarkers;
import dev.arnesurv.minimap.MapCache;
import dev.arnesurv.minimap.MinimapHud;
import dev.arnesurv.overlay.Structures;
import dev.arnesurv.stats.StatsRecorder;
import dev.arnesurv.waypoint.WaypointManager;
import dev.arnesurv.waypoint.WaypointShare;
import dev.arnesurv.worldmap.WorldMapScreen;
import dev.arnesurv.worldmap.WorldMapStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArneSurv implements ClientModInitializer {
	public static final String MOD_ID = "arnesurv";
	public static final Logger LOGGER = LoggerFactory.getLogger("ArneSurv");

	private static KeyMapping openMenuKey;
	private static KeyMapping waypointListKey;
	private static KeyMapping newWaypointKey;
	private static KeyMapping zoomInKey;
	private static KeyMapping zoomOutKey;
	private static KeyMapping worldMapKey;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static KeyMapping key(String name, int defaultKey, KeyMapping.Category category) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.arnesurv." + name, InputConstants.Type.KEYBOARD, defaultKey, category));
	}

	@Override
	public void onInitializeClient() {
		ModConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(id("main"));
		openMenuKey = key("menu", InputConstants.KEY_O, category);
		waypointListKey = key("waypoints", InputConstants.KEY_B, category);
		newWaypointKey = key("new_waypoint", InputConstants.KEY_N, category);
		zoomInKey = key("zoom_in", InputConstants.KEY_EQUALS, category);
		zoomOutKey = key("zoom_out", InputConstants.KEY_MINUS, category);
		worldMapKey = key("world_map", InputConstants.KEY_M, category);
		Zoom.init(key("zoom", InputConstants.KEY_C, category));

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("waypoint_markers"), WaypointMarkers::render);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("info_hud"), InfoHud.INSTANCE::renderHud);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("compass"), CompassHud.INSTANCE::renderHud);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("minimap"), MinimapHud.INSTANCE::renderHud);

		ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
			InventoryTweaks.onScreenInit(mc, screen);
			UpdateChecker.onScreenInit(mc, screen);
		});
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> WaypointShare.registerCommands(dispatcher));

		ClientLifecycleEvents.CLIENT_STARTED.register(mc -> UpdateChecker.checkAsync());
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
			WorldMapStore.saveAndUnloadAll();
			StatsRecorder.shutdown();
		});

		ClientTickEvents.END_CLIENT_TICK.register(ArneSurv::onTick);

		LOGGER.info("ArneSurv {} geladen", UpdateChecker.currentVersion());
	}

	private static void changeZoom(int delta) {
		ModConfig cfg = ModConfig.get();
		cfg.minimapZoom = Math.max(0, Math.min(MinimapHud.ZOOM_LEVELS.length - 1, cfg.minimapZoom + delta));
		ModConfig.save();
	}

	private static void onTick(Minecraft mc) {
		WaypointManager.tick(mc);
		WorldMapStore.tick(mc);
		Structures.tick(mc);
		StatsRecorder.tick(mc);
		MapCache.tick(mc);
		MinimapHud.INSTANCE.tick(mc);
		Zoom.tick();
		AutoTool.tick(mc);
		SneakKeeper.tick(mc);
		InventoryTweaks.tick(mc);
		DevAutoTest.tick(mc);

		while (worldMapKey.consumeClick()) {
			if (mc.player != null && ModConfig.get().worldMapEnabled) mc.gui.setScreen(new WorldMapScreen(null, worldMapKey));
		}
		while (zoomInKey.consumeClick()) changeZoom(-1);
		while (zoomOutKey.consumeClick()) changeZoom(1);
		while (openMenuKey.consumeClick()) {
			mc.gui.setScreen(new ArneSurvScreen(null));
		}
		while (waypointListKey.consumeClick()) {
			if (mc.player != null) mc.gui.setScreen(new WaypointListScreen(null));
		}
		while (newWaypointKey.consumeClick()) {
			if (mc.player != null) mc.gui.setScreen(WaypointEditScreen.forCurrentPosition(null));
		}
	}
}
