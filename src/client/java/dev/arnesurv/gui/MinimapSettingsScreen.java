package dev.arnesurv.gui;

import dev.arnesurv.config.ModConfig;
import dev.arnesurv.minimap.MinimapHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Einstellungen der Minimap. */
public class MinimapSettingsScreen extends Screen {
	private final Screen parent;

	public MinimapSettingsScreen(Screen parent) {
		super(Component.literal("Minimap"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ModConfig c = ModConfig.get();
		int colW = 150, gap = 8;
		int left = width / 2 - colW - gap / 2, right = width / 2 + gap / 2;
		int y = 40;

		addRenderableWidget(ToggleButton.create("Minimap", () -> c.minimapEnabled, v -> c.minimapEnabled = v, left, y, colW));
		addRenderableWidget(CycleOption.create(() -> "Form: " + (c.minimapRound ? "Rund" : "Eckig"),
			() -> c.minimapRound = !c.minimapRound, right, y, colW));

		addRenderableWidget(ToggleButton.create("Mitdrehen", () -> c.minimapRotate, v -> c.minimapRotate = v, left, y + 24, colW));
		addRenderableWidget(CycleOption.create(() -> "Höhlenmodus: " + caveLabel(c.minimapCaveMode),
			() -> c.minimapCaveMode = switch (c.minimapCaveMode) {
				case "AUTO" -> "ON";
				case "ON" -> "OFF";
				default -> "AUTO";
			}, right, y + 24, colW));

		addRenderableWidget(CycleOption.create(() -> "Größe: " + c.minimapSize,
			() -> c.minimapSize = next(MinimapHud.SIZES, c.minimapSize), left, y + 48, colW));
		addRenderableWidget(CycleOption.create(() -> "Zoom: " + MinimapHud.zoomBlocks() + " Blöcke",
			() -> c.minimapZoom = (c.minimapZoom + 1) % MinimapHud.ZOOM_LEVELS.length, right, y + 48, colW));

		addRenderableWidget(ToggleButton.create("Spieler zeigen", () -> c.minimapPlayers, v -> c.minimapPlayers = v, left, y + 72, colW));
		addRenderableWidget(ToggleButton.create("Waypoints zeigen", () -> c.minimapWaypoints, v -> c.minimapWaypoints = v, right, y + 72, colW));

		addRenderableWidget(Button.builder(Component.literal("Position ändern (HUD anpassen)"),
			b -> minecraft.gui.setScreen(new HudEditorScreen(this))).bounds(width / 2 - 100, y + 108, 200, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
			.bounds(width / 2 - 100, height - 28, 200, 20).build());
	}

	private static String caveLabel(String mode) {
		return switch (mode) {
			case "ON" -> "§aImmer";
			case "OFF" -> "§cNie";
			default -> "§eAuto";
		};
	}

	private static int next(int[] values, int current) {
		for (int i = 0; i < values.length; i++) if (values[i] == current) return values[(i + 1) % values.length];
		return values[0];
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, "§lMinimap", width / 2, 14, 0xFFFFFFFF);
		g.centeredText(font, "§7Höhlenmodus \"Auto\": schaltet um, sobald du unter der Erde bist. Im Nether immer an.",
			width / 2, y() , 0xFFFFFFFF);
	}

	private int y() {
		return 40 + 140;
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
