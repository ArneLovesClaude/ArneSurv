package dev.arnesurv.overlay;

import dev.arnesurv.config.ModConfig;
import dev.arnesurv.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import java.util.Random;

/**
 * Slime-Chunks hängen vom Welt-Seed ab. Im Einzelspieler kennt der Mod ihn automatisch,
 * auf Servern muss man ihn einmal in den Overlay-Einstellungen eintragen.
 */
public final class SlimeChunks {
	private SlimeChunks() {
	}

	/** Seed der aktuellen Welt, oder null, wenn unbekannt. */
	public static Long seed(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server != null) return server.overworld().getSeed();
		String key = WaypointManager.worldKey(mc);
		if (key == null) return null;
		String text = ModConfig.get().serverSeeds.get(key);
		return parse(text);
	}

	/** Wie Minecraft selbst: Zahlen direkt, sonst Text-Hash. */
	public static Long parse(String text) {
		if (text == null || text.isBlank()) return null;
		String t = text.trim();
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return (long) t.hashCode();
		}
	}

	/** Slime-Chunks gibt es nur in der Oberwelt. */
	public static boolean available(Minecraft mc) {
		return mc.level != null && mc.level.dimension() == Level.OVERWORLD && seed(mc) != null;
	}

	/** Die Original-Formel aus Minecraft (inkl. absichtlichem int-Überlauf). */
	public static boolean isSlimeChunk(long seed, int x, int z) {
		Random rnd = new Random(seed
			+ (long) (x * x * 0x4c1906)
			+ (long) (x * 0x5ac0db)
			+ (long) (z * z) * 0x4307a7L
			+ (long) (z * 0x5f24f) ^ 0x3ad8025fL);
		return rnd.nextInt(10) == 0;
	}
}
