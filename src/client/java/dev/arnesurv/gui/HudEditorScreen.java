package dev.arnesurv.gui;

import dev.arnesurv.config.ModConfig;
import dev.arnesurv.hud.CompassHud;
import dev.arnesurv.hud.HudPos;
import dev.arnesurv.hud.InfoHud;
import dev.arnesurv.hud.MovableHud;
import dev.arnesurv.minimap.MinimapHud;
import dev.arnesurv.waypoint.WaypointColors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * HUD-Editor: Elemente mit der Maus verschieben, mit dem Mausrad skalieren.
 */
public class HudEditorScreen extends Screen {
	private final Screen parent;
	private final List<MovableHud> elements = List.of(MinimapHud.INSTANCE, InfoHud.INSTANCE, CompassHud.INSTANCE);

	private MovableHud dragging;
	private double grabX, grabY;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("HUD anpassen"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ModConfig c = ModConfig.get();
		// Bei schmalen Fenstern zwei Reihen à zwei Knöpfe
		boolean twoRows = width < 4 * 120 + 12 + 8;
		int w = 120, perRow = twoRows ? 2 : 4;
		int total = w * perRow + 4 * (perRow - 1), x0 = width / 2 - total / 2;
		int y0 = height - (twoRows ? 50 : 28);
		int[][] slots = new int[4][];
		for (int i = 0; i < 4; i++) {
			slots[i] = new int[]{x0 + (i % perRow) * (w + 4), y0 + (i / perRow) * 24};
		}

		addRenderableWidget(Button.builder(colorLabel(), b -> {
			c.hudTextColor = WaypointColors.next(c.hudTextColor);
			ModConfig.save();
			b.setMessage(colorLabel());
		}).bounds(slots[0][0], slots[0][1], w, 20).build());
		addRenderableWidget(ToggleButton.create("Hintergrund", () -> c.hudBackground, v -> c.hudBackground = v, slots[1][0], slots[1][1], w));
		addRenderableWidget(Button.builder(Component.literal("Zurücksetzen"), b -> {
			ModConfig d = new ModConfig();
			c.infoPos = d.infoPos;
			c.compassPos = d.compassPos;
			c.minimapPos = d.minimapPos;
			ModConfig.save();
		}).bounds(slots[2][0], slots[2][1], w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
			.bounds(slots[3][0], slots[3][1], w, 20).build());
	}

	private Component colorLabel() {
		int color = ModConfig.get().hudTextColor;
		return Component.literal("Textfarbe: ").append(
			Component.literal("■ " + WaypointColors.name(color)).withColor(color & 0xFFFFFF));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// Kein Hintergrund – man soll die Spielwelt sehen
		g.fill(0, 0, width, height, 0x30000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		for (MovableHud e : elements) {
			if (!e.enabled()) continue;
			e.render(g, minecraft, true);
			int[] r = rect(e);
			boolean hover = e == dragging || inside(r, mouseX, mouseY);
			g.outline(r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2, hover ? 0xFFFFFF55 : 0x80FFFFFF);
			if (hover) {
				String info = e.displayName() + " · " + Math.round(e.pos().scale * 100) + "%";
				int ly = r[1] + r[3] + 4 > height - 40 ? r[1] - 12 : r[1] + r[3] + 4;
				g.text(font, info, r[0], ly, 0xFFFFFF55, true);
			}
		}
		g.centeredText(font, "§lHUD anpassen", width / 2, height / 2 - 20, 0xFFFFFFFF);
		g.centeredText(font, "§7Ziehen zum Verschieben · Mausrad zum Vergrößern/Verkleinern", width / 2, height / 2 - 6, 0xFFFFFFFF);
	}

	private int[] rect(MovableHud e) {
		return e.screenPos(width, height, e.width(minecraft, true), e.height(minecraft, true));
	}

	private static boolean inside(int[] r, double mx, double my) {
		return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
	}

	private MovableHud elementAt(double mx, double my) {
		for (int i = elements.size() - 1; i >= 0; i--) {
			MovableHud e = elements.get(i);
			if (e.enabled() && inside(rect(e), mx, my)) return e;
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		if (event.button() != 0) return false;
		MovableHud e = elementAt(event.x(), event.y());
		if (e == null) return false;
		int[] r = rect(e);
		dragging = e;
		grabX = event.x() - r[0];
		grabY = event.y() - r[1];
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) return super.mouseDragged(event, dx, dy);
		dragging.moveTo((int) Math.round(event.x() - grabX), (int) Math.round(event.y() - grabY),
			width, height, dragging.width(minecraft, true), dragging.height(minecraft, true));
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			ModConfig.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		MovableHud e = elementAt(mx, my);
		if (e == null || scrollY == 0) return super.mouseScrolled(mx, my, scrollX, scrollY);
		HudPos p = e.pos();
		p.scale = Mth.clamp(Math.round((p.scale + (scrollY > 0 ? 0.1f : -0.1f)) * 10f) / 10f, 0.5f, 2f);
		ModConfig.save();
		return true;
	}

	@Override
	public void onClose() {
		ModConfig.save();
		minecraft.gui.setScreen(parent);
	}
}
