package dev.arnesurv.gui;

import dev.arnesurv.config.ModConfig;
import dev.arnesurv.overlay.Structures;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Welche Strukturtypen auf Minimap und Weltkarte angezeigt werden. */
public class StructureTypesScreen extends Screen {
	private final Screen parent;

	public StructureTypesScreen(Screen parent) {
		super(Component.literal("Strukturtypen"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		var hidden = ModConfig.get().hiddenStructures;
		int colW = 150, gap = 8;
		int left = width / 2 - colW - gap / 2, right = width / 2 + gap / 2;
		Structures.Type[] types = Structures.Type.values();
		for (int i = 0; i < types.length; i++) {
			Structures.Type t = types[i];
			int x = i % 2 == 0 ? left : right, y = 36 + (i / 2) * 24;
			addRenderableWidget(ToggleButton.create(t.letter + " " + t.label,
				() -> !hidden.contains(t.name()),
				v -> {
					if (v) hidden.remove(t.name());
					else hidden.add(t.name());
				}, x, y, colW));
		}
		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
			.bounds(width / 2 - 100, height - 28, 200, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, "§lStrukturtypen anzeigen", width / 2, 14, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
