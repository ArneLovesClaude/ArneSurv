package dev.arnesurv.hud;

import dev.arnesurv.gui.HudEditorScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/** Basis für HUD-Elemente, die man im HUD-Editor verschieben und skalieren kann. */
public abstract class MovableHud {
	private static final int MARGIN = 2;

	public abstract String displayName();

	public abstract HudPos pos();

	public abstract boolean enabled();

	/** Größe ohne Skalierung. preview = im Editor (Beispieldaten erlaubt). */
	public abstract int width(Minecraft mc, boolean preview);

	public abstract int height(Minecraft mc, boolean preview);

	/** Zeichnet das Element mit der linken oberen Ecke bei (0, 0). */
	protected abstract void draw(GuiGraphicsExtractor g, Minecraft mc, boolean preview);

	/** Wird vom normalen HUD aufgerufen. */
	public void renderHud(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!enabled() || mc.player == null || mc.level == null || mc.gui.hud.isHidden()) return;
		if (mc.gui.screen() instanceof HudEditorScreen) return; // Editor zeichnet selbst
		render(g, mc, false);
	}

	public void render(GuiGraphicsExtractor g, Minecraft mc, boolean preview) {
		int w = width(mc, preview), h = height(mc, preview);
		if (w <= 0 || h <= 0) return;
		int[] p = screenPos(g.guiWidth(), g.guiHeight(), w, h);
		if (!preview) p[1] += avoidStatusEffects(mc, p, g.guiWidth());
		float s = pos().scale;
		g.pose().pushMatrix();
		g.pose().translate(p[0], p[1]);
		g.pose().scale(s, s);
		draw(g, mc, preview);
		g.pose().popMatrix();
	}

	/** Ob dieses Element den Trank-Symbolen oben rechts ausweichen soll. */
	protected boolean avoidsStatusEffects() {
		return false;
	}

	/**
	 * Minecraft zeigt Status-Effekte oben rechts (gute in Reihe 1, schlechte in Reihe 2).
	 * Liegt das Element dort, wird es so weit nach unten geschoben, dass nichts verdeckt wird.
	 */
	private int avoidStatusEffects(Minecraft mc, int[] rect, int screenW) {
		if (!avoidsStatusEffects() || mc.player == null) return 0;
		int good = 0, bad = 0;
		for (var e : mc.player.getActiveEffects()) {
			if (!e.showIcon()) continue;
			if (e.getEffect().value().isBeneficial()) good++;
			else bad++;
		}
		if (good + bad == 0) return 0;
		int iconsW = 25 * Math.max(good, bad);
		int iconsH = bad > 0 ? 52 : 26;
		boolean overlapX = rect[0] + rect[2] > screenW - iconsW;
		boolean overlapY = rect[1] < iconsH;
		return overlapX && overlapY ? iconsH - rect[1] + 2 : 0;
	}

	/** Linke obere Ecke auf dem Bildschirm. */
	public int[] screenPos(int screenW, int screenH, int w, int h) {
		float s = pos().scale;
		int sw = Math.round(w * s), sh = Math.round(h * s);
		int x = MARGIN + Math.round(pos().x * (screenW - sw - 2 * MARGIN));
		int y = MARGIN + Math.round(pos().y * (screenH - sh - 2 * MARGIN));
		return new int[]{x, y, sw, sh};
	}

	/** Setzt die Position so, dass die linke obere Ecke bei (x, y) liegt. */
	public void moveTo(int x, int y, int screenW, int screenH, int w, int h) {
		float s = pos().scale;
		int freeW = Math.max(1, screenW - Math.round(w * s) - 2 * MARGIN);
		int freeH = Math.max(1, screenH - Math.round(h * s) - 2 * MARGIN);
		pos().x = Mth.clamp((x - MARGIN) / (float) freeW, 0f, 1f);
		pos().y = Mth.clamp((y - MARGIN) / (float) freeH, 0f, 1f);
	}
}
