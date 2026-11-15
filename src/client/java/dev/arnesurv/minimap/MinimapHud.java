package dev.arnesurv.minimap;

import com.mojang.blaze3d.platform.NativeImage;
import dev.arnesurv.config.ModConfig;
import dev.arnesurv.hud.HudPos;
import dev.arnesurv.hud.MovableHud;
import dev.arnesurv.overlay.SlimeChunks;
import dev.arnesurv.overlay.Structures;
import dev.arnesurv.waypoint.Waypoint;
import dev.arnesurv.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;

/**
 * Die Minimap: rund oder eckig, optional mit dem Spieler mitdrehend.
 * Die Farben kommen aus dem {@link MapCache}; daraus wird eine 256×256-Textur
 * rund um den Spieler gebaut (1 Pixel = 1 Block).
 */
public final class MinimapHud extends MovableHud {
	public static final MinimapHud INSTANCE = new MinimapHud();

	public static final int[] ZOOM_LEVELS = {32, 64, 96, 128, 176};
	public static final int[] SIZES = {80, 100, 120, 150, 180};
	private static final int TEX = 256;

	private DynamicTexture texture;
	private int centerX, centerZ;
	private boolean needsRebuild = true;

	private MinimapHud() {
	}

	@Override
	public String displayName() {
		return "Minimap";
	}

	@Override
	public HudPos pos() {
		return ModConfig.get().minimapPos;
	}

	@Override
	public boolean enabled() {
		return ModConfig.get().minimapEnabled;
	}

	private int size() {
		int s = ModConfig.get().minimapSize;
		return s - (s % 2);
	}

	@Override
	public int width(Minecraft mc, boolean preview) {
		return size();
	}

	@Override
	public int height(Minecraft mc, boolean preview) {
		return size();
	}

	public static int zoomBlocks() {
		int i = Math.max(0, Math.min(ZOOM_LEVELS.length - 1, ModConfig.get().minimapZoom));
		return ZOOM_LEVELS[i];
	}

	/** Jeden Tick: Textur neu bauen, wenn sich etwas geändert hat oder man sich bewegt hat. */
	public void tick(Minecraft mc) {
		if (mc.player == null) return;
		if (MapCache.consumeChanged()
			|| Math.abs(mc.player.getBlockX() - centerX) > 8 || Math.abs(mc.player.getBlockZ() - centerZ) > 8) {
			needsRebuild = true;
		}
	}

	private void rebuild(Minecraft mc) {
		if (texture == null) {
			texture = new DynamicTexture(() -> "arnesurv_minimap", TEX, TEX, true);
		}
		centerX = mc.player.getBlockX();
		centerZ = mc.player.getBlockZ();
		NativeImage img = texture.getPixels();
		int x0 = centerX - TEX / 2, z0 = centerZ - TEX / 2;
		ModConfig cfg = ModConfig.get();
		boolean grid = cfg.overlayGridMinimap;

		// Slime-Chunks einmal pro Chunk statt pro Pixel berechnen
		int cx0 = x0 >> 4, cz0 = z0 >> 4, chunks = TEX / 16 + 2;
		boolean[] slime = null;
		if (cfg.overlaySlime && SlimeChunks.available(mc)) {
			long seed = SlimeChunks.seed(mc);
			slime = new boolean[chunks * chunks];
			for (int a = 0; a < chunks; a++)
				for (int b = 0; b < chunks; b++) slime[b * chunks + a] = SlimeChunks.isSlimeChunk(seed, cx0 + a, cz0 + b);
		}

		for (int j = 0; j < TEX; j++) {
			for (int i = 0; i < TEX; i++) {
				int x = x0 + i, z = z0 + j;
				int c = MapCache.colorAt(x, z);
				if (c != 0) {
					if (slime != null && slime[((z >> 4) - cz0) * chunks + ((x >> 4) - cx0)]) c = blend(c, 0xFF40FF40, 0.35f);
					if (grid && ((x & 15) == 0 || (z & 15) == 0)) c = BlockColorCache.shade(c, 0.7f);
				}
				img.setPixel(i, j, c);
			}
		}
		texture.upload();
		needsRebuild = false;
	}

	/** Erzwingt einen Neuaufbau, z.B. nach Änderung der Overlay-Einstellungen. */
	public void invalidate() {
		needsRebuild = true;
	}

	static int blend(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return 0xFF000000 | r << 16 | g << 8 | bl;
	}

	@Override
	protected void draw(GuiGraphicsExtractor g, Minecraft mc, boolean preview) {
		ModConfig cfg = ModConfig.get();
		LocalPlayer player = mc.player;
		int d = size();
		float r = d / 2f;
		boolean round = cfg.minimapRound;

		if (player == null) {
			// Vorschau im Hauptmenü-Editor ohne Welt
			fillShape(g, d, round, 0xFF2A3A2A);
			drawBorder(g, d, round);
			return;
		}
		if (needsRebuild || texture == null) rebuild(mc);

		float blocks = zoomBlocks();
		float f = d / blocks; // Pixel pro Block
		float offX = (float) (player.getX() - centerX);
		float offZ = (float) (player.getZ() - centerZ);
		float angle = cfg.minimapRotate ? (float) Math.toRadians(180f - player.getYRot()) : 0f;

		fillShape(g, d, round, 0xFF101010);
		if (!round) g.enableScissor(0, 0, d, d);

		g.pose().pushMatrix();
		g.pose().translate(r, r);
		g.pose().rotate(angle);
		var view = texture.getTextureView();
		var sampler = texture.getSampler();
		if (round) {
			// Zeilenweise zeichnen ergibt einen Kreis – auch wenn die Karte gedreht ist
			for (int row = 0; row < d; row++) {
				float yy = row - r + 0.5f;
				int hw = (int) Math.round(Math.sqrt(Math.max(0, r * r - yy * yy)));
				if (hw <= 0) continue;
				int y0 = row - (int) r;
				blitRegion(g, view, sampler, -hw, y0, hw, y0 + 1, f, offX, offZ);
			}
		} else {
			int half = cfg.minimapRotate ? (int) Math.ceil(r * 1.42f) + 1 : (int) r;
			blitRegion(g, view, sampler, -half, -half, half, half, f, offX, offZ);
		}
		g.pose().popMatrix();
		if (!round) g.disableScissor();

		drawBorder(g, d, round);
		drawNorth(g, mc, d, angle);
		if (cfg.waypointsEnabled && cfg.minimapWaypoints) drawWaypoints(g, mc, player, d, f, angle);
		if (cfg.overlayStructures) drawStructures(g, mc, player, d, f, angle);
		if (cfg.minimapPlayers) drawPlayers(g, mc, player, d, f, angle);
		drawArrow(g, d, cfg.minimapRotate ? 0f : (float) Math.toRadians(player.getYRot() + 180f));

		if (MapCache.isCaveMode(mc)) {
			g.text(mc.font, "Höhle", 3, d - 10, 0xAAFFFFFF, true);
		}
	}

	private static void blitRegion(GuiGraphicsExtractor g, com.mojang.renderpearl.api.textures.GpuTextureView view,
								   com.mojang.renderpearl.api.textures.GpuSampler sampler,
								   int x0, int y0, int x1, int y1, float f, float offX, float offZ) {
		float u0 = (TEX / 2f + offX + x0 / f) / TEX, u1 = (TEX / 2f + offX + x1 / f) / TEX;
		float v0 = (TEX / 2f + offZ + y0 / f) / TEX, v1 = (TEX / 2f + offZ + y1 / f) / TEX;
		g.blit(view, sampler, x0, y0, x1, y1, u0, u1, v0, v1);
	}

	private static void fillShape(GuiGraphicsExtractor g, int d, boolean round, int color) {
		if (!round) {
			g.fill(0, 0, d, d, color);
			return;
		}
		float r = d / 2f;
		for (int row = 0; row < d; row++) {
			float yy = row - r + 0.5f;
			int hw = (int) Math.round(Math.sqrt(Math.max(0, r * r - yy * yy)));
			g.fill((int) r - hw, row, (int) r + hw, row + 1, color);
		}
	}

	private static void drawBorder(GuiGraphicsExtractor g, int d, boolean round) {
		int c = 0xC0000000;
		if (!round) {
			g.outline(-1, -1, d + 2, d + 2, c);
			return;
		}
		float r = d / 2f + 1;
		int steps = d * 4;
		for (int i = 0; i < steps; i++) {
			double a = i * Math.PI * 2 / steps;
			int x = (int) Math.round(d / 2f + Math.cos(a) * r - 0.5);
			int y = (int) Math.round(d / 2f + Math.sin(a) * r - 0.5);
			g.fill(x, y, x + 1, y + 1, c);
		}
	}

	/** Punkt (dx, dz) in Blöcken → Bildschirm, gedreht; wird an den Rand geklemmt. */
	private static float[] toScreen(double dx, double dz, int d, float f, float angle, boolean round, float margin) {
		float x = (float) (dx * f), y = (float) (dz * f);
		float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle);
		float sx = x * cos - y * sin, sy = x * sin + y * cos;
		float r = d / 2f - margin;
		if (round) {
			float len = (float) Math.sqrt(sx * sx + sy * sy);
			if (len > r) {
				sx = sx / len * r;
				sy = sy / len * r;
			}
		} else {
			sx = Math.max(-r, Math.min(r, sx));
			sy = Math.max(-r, Math.min(r, sy));
		}
		return new float[]{d / 2f + sx, d / 2f + sy};
	}

	private void drawNorth(GuiGraphicsExtractor g, Minecraft mc, int d, float angle) {
		float[] p = toScreen(0, -10000, d, 1f, angle, ModConfig.get().minimapRound, 5f);
		int x = Math.round(p[0]), y = Math.round(p[1]);
		g.fill(x - 4, y - 5, x + 5, y + 5, 0xA0000000);
		g.centeredText(mc.font, "N", x + 1, y - 4, 0xFFFF5555);
	}

	private void drawWaypoints(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer player, int d, float f, float angle) {
		String dim = mc.level.dimension().identifier().toString();
		boolean round = ModConfig.get().minimapRound;
		for (Waypoint w : WaypointManager.inDimension(dim)) {
			if (!w.visible) continue;
			float[] p = toScreen(w.x + 0.5 - player.getX(), w.z + 0.5 - player.getZ(), d, f, angle, round, 3f);
			int x = Math.round(p[0]), y = Math.round(p[1]);
			g.fill(x - 3, y - 3, x + 3, y + 3, 0xFF000000);
			g.fill(x - 2, y - 2, x + 2, y + 2, w.color);
		}
	}

	/** Strukturen als kleine Buchstaben-Symbole – nur, wenn sie auf der Karte liegen (nicht am Rand). */
	private void drawStructures(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer player, int d, float f, float angle) {
		boolean round = ModConfig.get().minimapRound;
		float maxDist = d / 2f / f;
		for (Structures.Entry e : Structures.inDimension(mc.level.dimension().identifier().toString())) {
			double dx = e.x + 0.5 - player.getX(), dz = e.z + 0.5 - player.getZ();
			if (Math.abs(dx) > maxDist * 1.42 || Math.abs(dz) > maxDist * 1.42) continue;
			float[] p = toScreen(dx, dz, d, f, angle, round, 0f);
			float cx = p[0] - d / 2f, cy = p[1] - d / 2f;
			// am Rand geklemmt = eigentlich außerhalb → weglassen
			double len = Math.sqrt(dx * dx + dz * dz) * f;
			if (len > d / 2f - 4) continue;
			int x = Math.round(p[0]), y = Math.round(p[1]);
			g.fill(x - 4, y - 5, x + 5, y + 5, 0xC0000000);
			g.centeredText(mc.font, e.type.letter, x + 1, y - 4, e.type.color);
		}
	}

	private void drawPlayers(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer self, int d, float f, float angle) {
		boolean round = ModConfig.get().minimapRound;
		float maxDist = d / 2f / f;
		for (AbstractClientPlayer other : mc.level.players()) {
			if (other == self) continue;
			double dx = other.getX() - self.getX(), dz = other.getZ() - self.getZ();
			if (dx * dx + dz * dz > maxDist * maxDist * 2) continue;
			float[] p = toScreen(dx, dz, d, f, angle, round, 2f);
			int x = Math.round(p[0]), y = Math.round(p[1]);
			g.fill(x - 2, y - 2, x + 2, y + 2, 0xFF000000);
			g.fill(x - 1, y - 1, x + 1, y + 1, 0xFFFFFFFF);
		}
	}

	/** Pfeil in der Mitte; angle = Drehung (0 = zeigt nach oben). */
	private static void drawArrow(GuiGraphicsExtractor g, int d, float angle) {
		g.pose().pushMatrix();
		g.pose().translate(d / 2f, d / 2f);
		g.pose().rotate(angle);
		// Umriss
		for (int i = 0; i < 8; i++) {
			int hw = i / 2 + 1;
			g.fill(-hw - 1, -5 + i, hw + 1, -4 + i, 0xFF000000);
		}
		// Füllung
		for (int i = 0; i < 6; i++) {
			int hw = i / 2;
			g.fill(-hw, -4 + i, hw + 1, -3 + i, 0xFFFFFFFF);
		}
		g.pose().popMatrix();
	}
}
