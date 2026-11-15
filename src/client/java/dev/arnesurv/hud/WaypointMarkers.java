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
}
