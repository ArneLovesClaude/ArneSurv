package dev.arnesurv.gui;

import dev.arnesurv.comfort.ScreenshotScreen;
import dev.arnesurv.stats.StatsScreen;
import dev.arnesurv.worldmap.WorldMapScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Hauptmenü von ArneSurv – von hier aus erreicht man alles. */
public class ArneSurvScreen extends Screen {
	private final Screen parent;

	public ArneSurvScreen(Screen parent) {
		super(Component.literal("ArneSurv"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int w = 150, gap = 8;
		int left = width / 2 - w - gap / 2, right = width / 2 + gap / 2;
		int y = Math.max(40, height / 2 - 60);
		boolean inWorld = minecraft.player != null;

		// Links: Karte & Orientierung
		addRenderableWidget(Button.builder(Component.literal("Waypoints"),
			b -> minecraft.gui.setScreen(new WaypointListScreen(this))).bounds(left, y, w, 20).build()).active = inWorld;
		addRenderableWidget(Button.builder(Component.literal("Weltkarte"),
			b -> minecraft.gui.setScreen(new WorldMapScreen(this, null))).bounds(left, y + 24, w, 20).build()).active = inWorld;
		addRenderableWidget(Button.builder(Component.literal("Minimap"),
			b -> minecraft.gui.setScreen(new MinimapSettingsScreen(this))).bounds(left, y + 48, w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Overlays"),
			b -> minecraft.gui.setScreen(new OverlaySettingsScreen(this))).bounds(left, y + 72, w, 20).build());

		// Rechts: Komfort & Sonstiges
		addRenderableWidget(Button.builder(Component.literal("Komfort & Chat"),
			b -> minecraft.gui.setScreen(new ComfortSettingsScreen(this))).bounds(right, y, w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Statistiken"),
			b -> minecraft.gui.setScreen(new StatsScreen(this))).bounds(right, y + 24, w, 20).build()).active = inWorld;
		addRenderableWidget(Button.builder(Component.literal("Screenshots"),
			b -> minecraft.gui.setScreen(new ScreenshotScreen(this))).bounds(right, y + 48, w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("HUD anpassen"),
			b -> minecraft.gui.setScreen(new HudEditorScreen(this))).bounds(right, y + 72, w, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Module & HUD-Zeilen"),
			b -> minecraft.gui.setScreen(new SettingsScreen(this))).bounds(width / 2 - 100, y + 104, 200, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Fertig"),
			b -> onClose()).bounds(width / 2 - 100, y + 128, 200, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int y = Math.max(40, height / 2 - 60);
		g.centeredText(font, "§6§lArneSurv", width / 2, y - 28, 0xFFFFFFFF);
		g.centeredText(font, "§7Survival-Essentials für jeden Server · v" + dev.arnesurv.UpdateChecker.currentVersion(),
			width / 2, y - 16, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
