package dev.arnesurv.gui;

import dev.arnesurv.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Alle Module und HUD-Elemente einzeln an/aus. */
public class SettingsScreen extends Screen {
	private final Screen parent;

	public SettingsScreen(Screen parent) {
		super(Component.literal("Einstellungen"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ModConfig c = ModConfig.get();
		int colW = 150, gap = 8;
		int left = width / 2 - colW - gap / 2, right = width / 2 + gap / 2;
		int y = 46;

		// Linke Spalte: Module
		addRenderableWidget(ToggleButton.create("Info-HUD", () -> c.hudEnabled, v -> c.hudEnabled = v, left, y, colW));
		addRenderableWidget(ToggleButton.create("Kompassleiste", () -> c.compassEnabled, v -> c.compassEnabled = v, left, y + 24, colW));
		addRenderableWidget(ToggleButton.create("Waypoints", () -> c.waypointsEnabled, v -> c.waypointsEnabled = v, left, y + 48, colW));
		addRenderableWidget(ToggleButton.create("Todes-Waypoint", () -> c.deathWaypoints, v -> c.deathWaypoints = v, left, y + 72, colW));

		// Rechte Spalte: HUD-Inhalte
		addRenderableWidget(ToggleButton.create("Koordinaten", () -> c.hudCoords, v -> c.hudCoords = v, right, y, colW));
		addRenderableWidget(ToggleButton.create("Portal-Koords", () -> c.hudPortalCoords, v -> c.hudPortalCoords = v, right, y + 24, colW));
		addRenderableWidget(ToggleButton.create("Biom", () -> c.hudBiome, v -> c.hudBiome = v, right, y + 48, colW));
		addRenderableWidget(ToggleButton.create("Richtung", () -> c.hudDirection, v -> c.hudDirection = v, right, y + 72, colW));
		addRenderableWidget(ToggleButton.create("Uhrzeit", () -> c.hudTime, v -> c.hudTime = v, right, y + 96, colW));
		addRenderableWidget(ToggleButton.create("Tag-Zähler", () -> c.hudDay, v -> c.hudDay = v, right, y + 120, colW));
		addRenderableWidget(ToggleButton.create("FPS", () -> c.hudFps, v -> c.hudFps = v, right, y + 144, colW));
		addRenderableWidget(ToggleButton.create("Ping", () -> c.hudPing, v -> c.hudPing = v, right, y + 168, colW));
		addRenderableWidget(ToggleButton.create("Hintergrund", () -> c.hudBackground, v -> c.hudBackground = v, left, y + 120, colW));

		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
			.bounds(width / 2 - 100, height - 28, 200, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, "§lEinstellungen", width / 2, 12, 0xFFFFFFFF);
		g.centeredText(font, "§7Module", width / 2 - 79, 34, 0xFFFFFFFF);
		g.centeredText(font, "§7HUD-Anzeigen", width / 2 + 79, 34, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
