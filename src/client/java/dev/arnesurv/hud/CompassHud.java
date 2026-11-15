package dev.arnesurv.hud;

import dev.arnesurv.config.ModConfig;
import dev.arnesurv.waypoint.Waypoint;
import dev.arnesurv.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Kompassleiste: Himmelsrichtungen und Waypoints in Blickrichtung.
 * Der Waypoint, auf den man am genauesten schaut, zeigt Name und Entfernung.
 */
public final class CompassHud extends MovableHud {
	public static final CompassHud INSTANCE = new CompassHud();
	private static final int WIDTH = 200;
	private static final int HALF = WIDTH / 2;
	private static final float FOV = 90f; // Grad links und rechts der Mitte

	private CompassHud() {
	}

	@Override
	public String displayName() {
		return "Kompass";
	}

	@Override
	public HudPos pos() {
		return ModConfig.get().compassPos;
	}

	@Override
	public boolean enabled() {
		return ModConfig.get().compassEnabled;
	}

	@Override
	public int width(Minecraft mc, boolean preview) {
		return WIDTH;
	}

	@Override
	public int height(Minecraft mc, boolean preview) {
		return 27;
	}

	@Override
	protected void draw(GuiGraphicsExtractor g, Minecraft mc, boolean preview) {
		ModConfig cfg = ModConfig.get();
		LocalPlayer player = mc.player;
		float yaw = player != null ? player.getYRot() : 180f;
		int cx = HALF;

		g.fill(0, 1, WIDTH, 13, 0x60000000);
		g.fill(cx, 0, cx + 1, 14, 0xFFFFFFFF);

		// Himmelsrichtungen (Yaw: S=0, W=90, N=180, O=270)
		String[] labels = {"S", "SW", "W", "NW", "N", "NO", "O", "SO"};
		for (int i = 0; i < 8; i++) {
			float rel = Mth.wrapDegrees(i * 45f - yaw);
			if (Math.abs(rel) > FOV) continue;
			int x = cx + Math.round(rel / FOV * HALF);
			int color = labels[i].equals("N") ? 0xFFFF5555 : i % 2 == 0 ? cfg.hudTextColor : 0xFFAAAAAA;
			g.centeredText(mc.font, labels[i], x, 3, color);
		}

		if (!cfg.waypointsEnabled || player == null || mc.level == null) return;
		String dim = mc.level.dimension().identifier().toString();
		Waypoint focused = null;
		float focusedRel = 8f; // nur beschriften, wenn man ungefähr hinschaut
		for (Waypoint w : WaypointManager.inDimension(dim)) {
			if (!w.visible) continue;
			double dx = w.x + 0.5 - player.getX();
			double dz = w.z + 0.5 - player.getZ();
			float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
			float rel = Mth.wrapDegrees(target - yaw);
			if (Math.abs(rel) > FOV) continue;
			int x = cx + Math.round(rel / FOV * HALF);
			g.fill(x - 2, 10, x + 3, 15, 0xFF000000);
			g.fill(x - 1, 11, x + 2, 14, w.color);
			if (Math.abs(rel) < focusedRel) {
				focusedRel = Math.abs(rel);
				focused = w;
			}
		}
		if (focused != null) {
			int dist = (int) Math.round(focused.distanceTo(player.getX(), player.getY(), player.getZ()));
			String label = focused.name + " §7" + dist + "m";
			int w = mc.font.width(label);
			g.fill(cx - w / 2 - 3, 16, cx + w / 2 + 3, 27, 0x80000000);
			g.centeredText(mc.font, label, cx, 18, focused.color);
		}
	}
}
