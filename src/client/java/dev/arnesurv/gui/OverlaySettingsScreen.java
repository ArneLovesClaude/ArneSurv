package dev.arnesurv.gui;

import dev.arnesurv.config.ModConfig;
import dev.arnesurv.minimap.MinimapHud;
import dev.arnesurv.overlay.SlimeChunks;
import dev.arnesurv.overlay.Structures;
import dev.arnesurv.waypoint.WaypointManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Chunk-Gitter, Slime-Chunks, Strukturen – plus Seed-Eingabe für Server. */
public class OverlaySettingsScreen extends Screen {
	private final Screen parent;
	private EditBox seedBox;
	private String seedStatus = "";

	public OverlaySettingsScreen(Screen parent) {
		super(Component.literal("Overlays"));
		this.parent = parent;
	}

	private boolean onServer() {
		return minecraft.getSingleplayerServer() == null && WaypointManager.worldKey(minecraft) != null;
	}

	@Override
	protected void init() {
		ModConfig c = ModConfig.get();
		int colW = 150, gap = 8;
		int left = width / 2 - colW - gap / 2, right = width / 2 + gap / 2;
		int y = 40;

		addRenderableWidget(ToggleButton.create("Gitter Minimap", () -> c.overlayGridMinimap, v -> c.overlayGridMinimap = v, left, y, colW));
		addRenderableWidget(ToggleButton.create("Gitter Weltkarte", () -> c.overlayGridWorldMap, v -> c.overlayGridWorldMap = v, right, y, colW));
		addRenderableWidget(ToggleButton.create("Slime-Chunks", () -> c.overlaySlime, v -> c.overlaySlime = v, left, y + 24, colW));
		addRenderableWidget(ToggleButton.create("Strukturen", () -> c.overlayStructures, v -> c.overlayStructures = v, right, y + 24, colW));

		int sy = y + 64;
		if (onServer()) {
			String key = WaypointManager.worldKey(minecraft);
			seedBox = new EditBox(font, left, sy, colW * 2 + gap - 64, 20, Component.literal("Seed"));
			seedBox.setMaxLength(64);
			seedBox.setHint(Component.literal("§7Seed dieses Servers (für Slime-Chunks)"));
			String current = c.serverSeeds.get(key);
			if (current != null) seedBox.setValue(current);
			addRenderableWidget(seedBox);
			addRenderableWidget(Button.builder(Component.literal("Speichern"), b -> {
				String v = seedBox.getValue().trim();
				if (v.isEmpty()) c.serverSeeds.remove(key);
				else c.serverSeeds.put(key, v);
				ModConfig.save();
				seedStatus = v.isEmpty() ? "§7Seed entfernt" : "§aSeed gespeichert";
			}).bounds(left + colW * 2 + gap - 60, sy, 60, 20).build());
		}

		addRenderableWidget(Button.builder(Component.literal("Strukturtypen auswählen…"), b ->
			minecraft.gui.setScreen(new StructureTypesScreen(this))).bounds(width / 2 - 100, sy + 40, 200, 20).build());
		addRenderableWidget(Button.builder(Component.literal("§cErkannte Strukturen löschen"), b ->
			minecraft.gui.setScreen(new ConfirmScreen(yes -> {
				if (yes) Structures.clearAll();
				minecraft.gui.setScreen(this);
			}, Component.literal("Alle erkannten Strukturen löschen?"),
				Component.literal("Betrifft nur diese Welt bzw. diesen Server. Sie werden beim Erkunden neu erkannt."))))
			.bounds(width / 2 - 100, sy + 64, 200, 20).build());

		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
			.bounds(width / 2 - 100, height - 28, 200, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, "§lOverlays", width / 2, 14, 0xFFFFFFFF);
		int sy = 40 + 64;
		if (onServer()) {
			g.text(font, "§7Seed (auf Servern nötig für Slime-Chunks, z.B. per /seed erfragen)", width / 2 - 154, sy - 11, 0xFFFFFFFF, false);
			g.centeredText(font, seedStatus, width / 2, sy + 24, 0xFFFFFFFF);
		} else if (minecraft.getSingleplayerServer() != null) {
			g.centeredText(font, "§7Einzelspieler: Seed wird automatisch erkannt.", width / 2, sy + 6, 0xFFFFFFFF);
		}
		if (ModConfig.get().overlaySlime && minecraft.level != null && !SlimeChunks.available(minecraft)) {
			g.centeredText(font, "§eSlime-Chunks: nur in der Oberwelt und nur mit bekanntem Seed.", width / 2, sy + 104, 0xFFFFFFFF);
		}
		g.centeredText(font, "§7Erkannte Strukturen: " + Structures.count(), width / 2, sy + 90, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		MinimapHud.INSTANCE.invalidate();
		minecraft.gui.setScreen(parent);
	}
}
