package dev.arnesurv.hud;

import dev.arnesurv.config.ModConfig;
import dev.arnesurv.waypoint.Waypoint;
import dev.arnesurv.waypoint.WaypointManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * Zeigt Waypoints direkt in der Spielwelt: Rauten-Symbol über dem Punkt,
 * Name und Entfernung, wenn man ungefähr hinschaut oder nah dran ist.
 * (Wird auf dem HUD gezeichnet und per Kamera-Projektion platziert.)
 */
public final class WaypointMarkers {
	private WaypointMarkers() {
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		ModConfig cfg = ModConfig.get();
		if (!cfg.waypointsEnabled || !cfg.waypointMarkers || mc.player == null || mc.level == null
			|| mc.gui.hud.isHidden() || mc.gui.screen() != null) return;

		Camera cam = mc.gameRenderer.mainCamera();
		Vec3 pos = cam.position();
		Vector3fc fwd = cam.forwardVector(), up = cam.upVector(), left = cam.leftVector();
		int w = g.guiWidth(), h = g.guiHeight();
		double focal = (h / 2.0) / Math.tan(Math.toRadians(cam.getFov()) / 2.0);

		String dim = mc.level.dimension().identifier().toString();
		for (Waypoint wp : WaypointManager.inDimension(dim)) {
			if (!wp.visible) continue;
			double dx = wp.x + 0.5 - pos.x, dy = wp.y + 1.2 - pos.y, dz = wp.z + 0.5 - pos.z;
			double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (cfg.waypointBeams && dist <= cfg.waypointBeamMaxDistance && dist > 3) {
				drawBeam(g, wp, pos, fwd, up, left, focal, w, h, dist);
			}
			if (dist > cfg.waypointMarkerMaxDistance || dist < 2) continue;

			double zc = dx * fwd.x() + dy * fwd.y() + dz * fwd.z();
			if (zc < 0.05) continue; // hinter der Kamera
			double xc = dx * left.x() + dy * left.y() + dz * left.z();
			double yc = dx * up.x() + dy * up.y() + dz * up.z();
			int sx = (int) Math.round(w / 2.0 - xc / zc * focal);
			int sy = (int) Math.round(h / 2.0 - yc / zc * focal);
			if (sx < -20 || sy < -20 || sx > w + 20 || sy > h + 20) continue;

			// Raute
			for (int i = 0; i < 5; i++) {
				g.fill(sx - i - 1, sy - 5 + i, sx + i + 2, sy - 4 + i, 0xFF000000);
				g.fill(sx - i - 1, sy + 4 - i, sx + i + 2, sy + 5 - i, 0xFF000000);
			}
			for (int i = 0; i < 4; i++) {
				g.fill(sx - i, sy - 4 + i, sx + i + 1, sy - 3 + i, wp.color);
				g.fill(sx - i, sy + 3 - i, sx + i + 1, sy + 4 - i, wp.color);
			}

			boolean looking = Math.abs(sx - w / 2) < 40 && Math.abs(sy - h / 2) < 40;
			if (looking || dist < 48) {
				String label = wp.name + " §7" + Math.round(dist) + "m";
				int tw = mc.font.width(label);
				g.fill(sx - tw / 2 - 2, sy - 18, sx + tw / 2 + 2, sy - 7, 0x90000000);
				g.centeredText(mc.font, label, sx, sy - 16, wp.color);
			}
		}
	}

	/** Projiziert einen Weltpunkt auf den Bildschirm; null, wenn er hinter der Kamera liegt. */
	private static double[] project(double x, double y, double z, Vec3 cam, Vector3fc fwd, Vector3fc up, Vector3fc left,
									double focal, int w, int h) {
		double dx = x - cam.x, dy = y - cam.y, dz = z - cam.z;
		double zc = dx * fwd.x() + dy * fwd.y() + dz * fwd.z();
		if (zc < 0.05) return null;
		double xc = dx * left.x() + dy * left.y() + dz * left.z();
		double yc = dx * up.x() + dy * up.y() + dz * up.z();
		return new double[]{w / 2.0 - xc / zc * focal, h / 2.0 - yc / zc * focal};
	}

	/**
	 * Lichtsäule: eine durchscheinende, farbige Linie vom Waypoint senkrecht nach oben.
	 * Sie wird stückweise projiziert, damit sie auch beim Hoch-/Runterschauen richtig verläuft.
	 */
	private static void drawBeam(GuiGraphicsExtractor g, Waypoint wp, Vec3 cam, Vector3fc fwd, Vector3fc up, Vector3fc left,
								 double focal, int w, int h, double dist) {
		int color = (wp.color & 0xFFFFFF) | 0x70000000;
		int hw = dist < 32 ? 2 : dist < 128 ? 1 : 0;
		double x = wp.x + 0.5, z = wp.z + 0.5;
		double y0 = wp.y, y1 = wp.y + 256;
		int samples = 32;
		double[] prev = project(x, y0, z, cam, fwd, up, left, focal, w, h);
		for (int i = 1; i <= samples; i++) {
			double y = y0 + (y1 - y0) * i / samples;
			double[] cur = project(x, y, z, cam, fwd, up, left, focal, w, h);
			if (prev != null && cur != null) segment(g, prev, cur, hw, color, w, h);
			prev = cur;
		}
	}

	/** Zeichnet ein Linienstück Zeile für Zeile (die Säule ist auf dem Bildschirm fast senkrecht). */
	private static void segment(GuiGraphicsExtractor g, double[] a, double[] b, int hw, int color, int w, int h) {
		double ya = a[1], yb = b[1];
		if (Math.abs(yb - ya) < 0.5) return;
		int from = (int) Math.max(0, Math.floor(Math.min(ya, yb)));
		int to = (int) Math.min(h, Math.ceil(Math.max(ya, yb)));
		for (int sy = from; sy < to; sy++) {
			double t = (sy + 0.5 - ya) / (yb - ya);
			int sx = (int) Math.round(a[0] + (b[0] - a[0]) * t);
			if (sx < -hw || sx > w + hw) continue;
			g.fill(sx - hw, sy, sx + hw + 1, sy + 1, color);
		}
	}
}
