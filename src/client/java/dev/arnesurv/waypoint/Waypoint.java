package dev.arnesurv.waypoint;

public class Waypoint {
	public String name;
	public int x;
	public int y;
	public int z;
	/** z.B. "minecraft:overworld" */
	public String dimension;
	public int color;
	public boolean visible = true;
	public boolean death = false;
	public long created = System.currentTimeMillis();

	public Waypoint() {
	}

	public Waypoint(String name, int x, int y, int z, String dimension, int color) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		this.color = color;
	}

	public double distanceTo(double px, double py, double pz) {
		double dx = x + 0.5 - px, dy = y - py, dz = z + 0.5 - pz;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
