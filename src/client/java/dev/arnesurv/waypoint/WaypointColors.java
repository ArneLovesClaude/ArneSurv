package dev.arnesurv.waypoint;

/** Feste Farbpalette, damit die Auswahl simpel bleibt. */
public final class WaypointColors {
	public static final int[] COLORS = {
		0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55,
		0xFF55FFFF, 0xFF5555FF, 0xFFFF55FF, 0xFFFFFFFF
	};
	public static final String[] NAMES = {
		"Rot", "Orange", "Gelb", "Grün", "Türkis", "Blau", "Pink", "Weiß"
	};

	private WaypointColors() {
	}

	public static int indexOf(int color) {
		for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == color) return i;
		return 0;
	}

	public static int next(int color) {
		return COLORS[(indexOf(color) + 1) % COLORS.length];
	}

	public static String name(int color) {
		return NAMES[indexOf(color)];
	}
}
