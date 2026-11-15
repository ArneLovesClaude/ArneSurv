package dev.arnesurv.hud;

/**
 * Position eines HUD-Elements. x/y sind relativ (0 = links/oben, 1 = rechts/unten),
 * damit das Element bei jeder Fenstergröße an der gleichen Stelle bleibt.
 */
public class HudPos {
	public float x;
	public float y;
	public float scale = 1f;

	public HudPos() {
	}

	public HudPos(float x, float y, float scale) {
		this.x = x;
		this.y = y;
		this.scale = scale;
	}
}
