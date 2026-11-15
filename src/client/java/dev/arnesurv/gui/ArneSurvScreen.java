package dev.arnesurv.gui;

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
		int w = 200, x = width / 2 - w / 2, y = height / 4 + 10;
		addRenderableWidget(Button.builder(Component.literal("Waypoints"),
			b -> minecraft.gui.setScreen(new WaypointListScreen(this))).bounds(x, y, w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Minimap"),
			b -> minecraft.gui.setScreen(new MinimapSettingsScreen(this))).bounds(x, y + 24, w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("HUD anpassen (verschieben)"),
			b -> minecraft.gui.setScreen(new HudEditorScreen(this))).bounds(x, y + 48, w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Einstellungen & Module"),
			b -> minecraft.gui.setScreen(new SettingsScreen(this))).bounds(x, y + 72, w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Fertig"),
			b -> onClose()).bounds(x, y + 108, w, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, "§6§lArneSurv", width / 2, height / 4 - 20, 0xFFFFFFFF);
		g.centeredText(font, "§7Survival-Essentials für jeden Server", width / 2, height / 4 - 8, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
