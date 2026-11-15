package dev.arnesurv;

import com.mojang.blaze3d.platform.InputConstants;
import dev.arnesurv.config.ModConfig;
import dev.arnesurv.gui.ArneSurvScreen;
import dev.arnesurv.gui.WaypointEditScreen;
import dev.arnesurv.gui.WaypointListScreen;
import dev.arnesurv.hud.CompassHud;
import dev.arnesurv.hud.InfoHud;
import dev.arnesurv.waypoint.WaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
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

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitializeClient() {
		ModConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(id("main"));
		openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.arnesurv.menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_O, category));
		waypointListKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.arnesurv.waypoints", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, category));
		newWaypointKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.arnesurv.new_waypoint", InputConstants.Type.KEYBOARD, InputConstants.KEY_N, category));

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("info_hud"), InfoHud.INSTANCE::renderHud);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, id("compass"), CompassHud.INSTANCE::renderHud);

		ClientTickEvents.END_CLIENT_TICK.register(ArneSurv::onTick);

		LOGGER.info("ArneSurv geladen");
	}

	private static void onTick(Minecraft mc) {
		WaypointManager.tick(mc);

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
