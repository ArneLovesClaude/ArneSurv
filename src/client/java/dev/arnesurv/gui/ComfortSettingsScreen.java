package dev.arnesurv.gui;

import dev.arnesurv.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Komfort- und Chat-Funktionen an/aus. */
public class ComfortSettingsScreen extends Screen {
	private final Screen parent;

	public ComfortSettingsScreen(Screen parent) {
		super(Component.literal("Komfort & Chat"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ModConfig c = ModConfig.get();
		var o = minecraft.options;
		int colW = 150, gap = 8;
		int left = width / 2 - colW - gap / 2, right = width / 2 + gap / 2;
		int y = 40;

		// Links: Steuerung
		addRenderableWidget(ToggleButton.create("Zoom (Taste C)", () -> c.zoomEnabled, v -> c.zoomEnabled = v, left, y, colW));
		addRenderableWidget(ToggleButton.create("Auto-Tool", () -> c.autoToolEnabled, v -> c.autoToolEnabled = v, left, y + 24, colW));
		addRenderableWidget(ToggleButton.create("Danach zurück", () -> c.autoToolSwitchBack, v -> c.autoToolSwitchBack = v, left, y + 48, colW));
		addRenderableWidget(ToggleButton.create("Sprint umschalten", () -> o.toggleSprint().get(), v -> {
			o.toggleSprint().set(v);
			o.save();
		}, left, y + 72, colW));
		addRenderableWidget(ToggleButton.create("Sneak umschalten", () -> o.toggleCrouch().get(), v -> {
			o.toggleCrouch().set(v);
			o.save();
		}, left, y + 96, colW));
		addRenderableWidget(ToggleButton.create("Sneak im Inventar", () -> c.sneakInInventory, v -> c.sneakInInventory = v, left, y + 120, colW));

		// Rechts: Inventar, Chat, Sonstiges
		addRenderableWidget(ToggleButton.create("Inventar-Knöpfe", () -> c.inventoryButtons, v -> c.inventoryButtons = v, right, y, colW));
		addRenderableWidget(ToggleButton.create("Chat-Uhrzeit", () -> c.chatTimestamps, v -> c.chatTimestamps = v, right, y + 24, colW));
		addRenderableWidget(ToggleButton.create("Erwähnungen", () -> c.chatMentions, v -> c.chatMentions = v, right, y + 48, colW));
		addRenderableWidget(ToggleButton.create("Waypoints im Spiel", () -> c.waypointMarkers, v -> c.waypointMarkers = v, right, y + 72, colW));
		addRenderableWidget(ToggleButton.create("Lichtsäulen", () -> c.waypointBeams, v -> c.waypointBeams = v, right, y + 96, colW));
		addRenderableWidget(ToggleButton.create("Statistik mitschreiben", () -> c.statsEnabled, v -> c.statsEnabled = v, right, y + 120, colW));
		addRenderableWidget(ToggleButton.create("Update-Hinweis", () -> c.updateCheck, v -> c.updateCheck = v, left, y + 144, colW));

		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
			.bounds(width / 2 - 100, height - 28, 200, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, "§lKomfort & Chat", width / 2, 14, 0xFFFFFFFF);
		g.centeredText(font, "§7Steuerung", width / 2 - 79, 30, 0xFFFFFFFF);
		g.centeredText(font, "§7Inventar, Chat & mehr", width / 2 + 79, 30, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
