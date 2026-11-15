package dev.arnesurv.comfort;

import dev.arnesurv.config.ModConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Zoom wie bei OptiFine: Taste halten, Mausrad ändert die Stärke. */
public final class Zoom {
	private static KeyMapping key;
	private static float strength = 4f; // 4× Vergrößerung
	private static float current = 1f;  // für weiches Ein-/Auszoomen
	/** Nur für den Entwickler-Autotest. */
	public static boolean devForce;

	private Zoom() {
	}

	public static void init(KeyMapping zoomKey) {
		key = zoomKey;
	}

	public static boolean active() {
		Minecraft mc = Minecraft.getInstance();
		return devForce || (key != null && ModConfig.get().zoomEnabled && mc.gui.screen() == null && key.isDown());
	}

	/** Jeden Tick: Zoom weich nachführen; beim Loslassen Stärke zurücksetzen. */
	public static void tick() {
		float target = active() ? strength : 1f;
		current += (target - current) * 0.5f;
		if (Math.abs(current - target) < 0.01f) current = target;
		if (!active()) strength = ModConfig.get().zoomDefault;
	}

	public static float applyFov(float fov) {
		return current <= 1.001f ? fov : fov / current;
	}

	public static double sensitivityFactor() {
		return current <= 1.001f ? 1.0 : 1.0 / Math.sqrt(current);
	}

	/** true = Scroll verbraucht (nur während des Zoomens). */
	public static boolean onScroll(double amount) {
		if (!active() || amount == 0) return false;
		strength = Mth.clamp(strength * (amount > 0 ? 1.25f : 0.8f), 1.5f, 50f);
		return true;
	}
}
