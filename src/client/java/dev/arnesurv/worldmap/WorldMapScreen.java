package dev.arnesurv.worldmap;

import dev.arnesurv.config.ModConfig;
import dev.arnesurv.gui.WaypointEditScreen;
import dev.arnesurv.overlay.SlimeChunks;
import dev.arnesurv.overlay.Structures;
import dev.arnesurv.waypoint.Waypoint;
import dev.arnesurv.waypoint.WaypointManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashSet;
import java.util.Set;

/**
 * Vollbild-Weltkarte: zeigt alles, was man in dieser Dimension schon erkundet hat.
 * Ziehen = verschieben, Mausrad = zoomen, Rechtsklick = Waypoint setzen/bearbeiten.
 */
public class WorldMapScreen extends Screen {
	private static final float MIN_SCALE = 1f / 16f, MAX_SCALE = 16f;
	/** Zoom bleibt zwischen zwei Öffnungen erhalten. */
	private static float scale = 1f;

	private final Screen parent;
	private final KeyMapping closeKey;
	private double camX, camZ;
	private boolean dragging;
	private double dragDistance;
	private final Set<Long> missing = new HashSet<>();
	private int frames;

	public WorldMapScreen(Screen parent, KeyMapping closeKey) {
		super(Component.literal("Weltkarte"));
		this.parent = parent;
		this.closeKey = closeKey;
	}

	@Override
	protected void init() {
		if (minecraft.player == null) {
			onClose();
			return;
		}
		if (camX == 0 && camZ == 0) centerOnPlayer();
		addRenderableWidget(Button.builder(Component.literal("Zu mir"), b -> centerOnPlayer())
			.bounds(width - 128, 6, 60, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
			.bounds(width - 64, 6, 58, 20).build());
	}

	private void centerOnPlayer() {
		camX = minecraft.player.getX();
		camZ = minecraft.player.getZ();
	}

	private double worldX(double mx) {
		return camX + (mx - width / 2.0) / scale;
	}

	private double worldZ(double my) {
		return camZ + (my - height / 2.0) / scale;
	}

	private float screenX(double wx) {
		return (float) ((wx - camX) * scale + width / 2.0);
	}

	private float screenY(double wz) {
		return (float) ((wz - camZ) * scale + height / 2.0);
	}

	/** Die Karte selbst ist der "Hintergrund" – darüber kommen Fußzeile und Knöpfe. */
	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, width, height, 0xFF0B0B0D);
		if (minecraft.player == null || minecraft.level == null) return;
		if (++frames % 120 == 0) missing.clear(); // ab und zu prüfen, ob neue Regionen da sind
		drawRegions(g);
		drawChunkOverlays(g);
		drawStructures(g, mouseX, mouseY);
		drawWaypoints(g, mouseX, mouseY);
		drawOtherPlayers(g, mouseX, mouseY);
		drawPlayer(g);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		if (minecraft.player != null && minecraft.level != null) drawHud(g, mouseX, mouseY);
		super.extractRenderState(g, mouseX, mouseY, delta);
	}

	private void drawRegions(GuiGraphicsExtractor g) {
		int r = WorldMapStore.REGION;
		int minRX = Math.floorDiv((int) Math.floor(worldX(0)), r), maxRX = Math.floorDiv((int) Math.floor(worldX(width)), r);
		int minRZ = Math.floorDiv((int) Math.floor(worldZ(0)), r), maxRZ = Math.floorDiv((int) Math.floor(worldZ(height)), r);

		g.pose().pushMatrix();
		g.pose().translate(width / 2f, height / 2f);
		g.pose().scale(scale, scale);
		// Ursprung auf die Kamera legen; ganzzahliger Anteil getrennt, damit es auch weit draußen genau bleibt
		int baseX = (int) Math.floor(camX), baseZ = (int) Math.floor(camZ);
		g.pose().translate((float) -(camX - baseX), (float) -(camZ - baseZ));
		for (int rx = minRX; rx <= maxRX; rx++) {
			for (int rz = minRZ; rz <= maxRZ; rz++) {
				long k = ((long) rx << 32) | (rz & 0xFFFFFFFFL);
				if (missing.contains(k)) continue;
				WorldMapStore.Region region = WorldMapStore.region(rx, rz, false);
				if (region == null) {
					missing.add(k);
					continue;
				}
				var tex = region.texture();
				int x0 = rx * r - baseX, z0 = rz * r - baseZ;
				g.blit(tex.getTextureView(), tex.getSampler(), x0, z0, x0 + r, z0 + r, 0f, 1f, 0f, 1f);
			}
		}
		g.pose().popMatrix();
	}

	/** Chunk-Gitter und Slime-Chunks – nur ab einer gewissen Zoomstufe, sonst wird es Matsch. */
	private void drawChunkOverlays(GuiGraphicsExtractor g) {
		ModConfig cfg = ModConfig.get();
		int minCX = (int) Math.floor(worldX(0)) >> 4, maxCX = (int) Math.floor(worldX(width)) >> 4;
		int minCZ = (int) Math.floor(worldZ(0)) >> 4, maxCZ = (int) Math.floor(worldZ(height)) >> 4;

		if (cfg.overlaySlime && scale >= 0.25f && SlimeChunks.available(minecraft)) {
			long seed = SlimeChunks.seed(minecraft);
			for (int cx = minCX; cx <= maxCX; cx++) {
				for (int cz = minCZ; cz <= maxCZ; cz++) {
					if (!SlimeChunks.isSlimeChunk(seed, cx, cz)) continue;
					int x0 = Math.round(screenX(cx << 4)), z0 = Math.round(screenY(cz << 4));
					int x1 = Math.round(screenX((cx + 1) << 4)), z1 = Math.round(screenY((cz + 1) << 4));
					g.fill(x0, z0, x1, z1, 0x5540FF40);
				}
			}
		}
		if (cfg.overlayGridWorldMap && scale >= 1f) {
			int alpha = scale >= 2f ? 0x50000000 : 0x30000000;
			for (int cx = minCX; cx <= maxCX + 1; cx++) {
				int x = Math.round(screenX(cx << 4));
				g.fill(x, 0, x + 1, height, (cx & 31) == 0 ? 0x80000000 : alpha);
			}
			for (int cz = minCZ; cz <= maxCZ + 1; cz++) {
				int y = Math.round(screenY(cz << 4));
				g.fill(0, y, width, y + 1, (cz & 31) == 0 ? 0x80000000 : alpha);
			}
		}
	}

	private Structures.Entry hoveredStructure(double mx, double my) {
		if (!ModConfig.get().overlayStructures) return null;
		for (Structures.Entry e : Structures.inDimension(minecraft.level.dimension().identifier().toString())) {
			float x = screenX(e.x + 0.5), y = screenY(e.z + 0.5);
			if (Math.abs(mx - x) <= 5 && Math.abs(my - y) <= 5) return e;
		}
		return null;
	}

	private void drawStructures(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		if (!ModConfig.get().overlayStructures) return;
		Structures.Entry hovered = hoveredStructure(mouseX, mouseY);
		for (Structures.Entry e : Structures.inDimension(minecraft.level.dimension().identifier().toString())) {
			int x = Math.round(screenX(e.x + 0.5)), y = Math.round(screenY(e.z + 0.5));
			if (x < -20 || y < -20 || x > width + 20 || y > height + 20) continue;
			g.fill(x - 5, y - 6, x + 6, y + 6, 0xD0000000);
			g.outline(x - 5, y - 6, 11, 12, e.type.color);
			g.centeredText(font, e.type.letter, x + 1, y - 4, e.type.color);
			if (e == hovered) {
				String label = e.type.label + " §7(" + e.x + ", " + e.y + ", " + e.z + ")";
				int tw = font.width(label);
				g.fill(x - tw / 2 - 3, y + 8, x + tw / 2 + 3, y + 19, 0xD0000000);
				g.centeredText(font, label, x, y + 10, 0xFFFFFFFF);
			}
		}
	}

	private Waypoint hoveredWaypoint(double mx, double my) {
		String dim = minecraft.level.dimension().identifier().toString();
		for (Waypoint w : WaypointManager.inDimension(dim)) {
			float x = screenX(w.x + 0.5), y = screenY(w.z + 0.5);
			if (Math.abs(mx - x) <= 5 && Math.abs(my - y) <= 5) return w;
		}
		return null;
	}

	private void drawWaypoints(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		String dim = minecraft.level.dimension().identifier().toString();
		Waypoint hovered = hoveredWaypoint(mouseX, mouseY);
		for (Waypoint w : WaypointManager.inDimension(dim)) {
			int x = Math.round(screenX(w.x + 0.5)), y = Math.round(screenY(w.z + 0.5));
			if (x < -50 || y < -20 || x > width + 50 || y > height + 20) continue;
			int alpha = w.visible ? 0xFF000000 : 0x80000000;
			g.fill(x - 4, y - 4, x + 4, y + 4, alpha);
			g.fill(x - 3, y - 3, x + 3, y + 3, (w.color & 0xFFFFFF) | alpha);
			if (w == hovered || scale >= 0.5f) {
				String label = (w.death ? "☠ " : "") + w.name;
				int tw = font.width(label);
				g.fill(x - tw / 2 - 2, y + 6, x + tw / 2 + 2, y + 17, 0xA0000000);
				g.centeredText(font, label, x, y + 8, w == hovered ? 0xFFFFFF55 : 0xFFFFFFFF);
			}
		}
	}

	/** Andere Spieler (nur die, die der Server dem Client gerade schickt – also in Sichtweite). */
	private void drawOtherPlayers(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		if (!ModConfig.get().minimapPlayers) return;
		for (var other : minecraft.level.players()) {
			if (other == minecraft.player) continue;
			int x = Math.round(screenX(other.getX())), y = Math.round(screenY(other.getZ()));
			if (x < -10 || y < -10 || x > width + 10 || y > height + 10) continue;
			g.fill(x - 3, y - 3, x + 3, y + 3, 0xFF000000);
			g.fill(x - 2, y - 2, x + 2, y + 2, 0xFFFFFFFF);
			String name = other.getGameProfile().name();
			int tw = font.width(name);
			g.fill(x - tw / 2 - 2, y - 15, x + tw / 2 + 2, y - 4, 0xA0000000);
			g.centeredText(font, name, x, y - 13, 0xFFFFFFFF);
		}
	}

	private void drawPlayer(GuiGraphicsExtractor g) {
		var p = minecraft.player;
		g.pose().pushMatrix();
		g.pose().translate(screenX(p.getX()), screenY(p.getZ()));
		g.pose().rotate((float) Math.toRadians(p.getYRot() + 180f));
		for (int i = 0; i < 9; i++) {
			int hw = i / 2 + 1;
			g.fill(-hw - 1, -6 + i, hw + 1, -5 + i, 0xFF000000);
		}
		for (int i = 0; i < 7; i++) {
			int hw = i / 2;
			g.fill(-hw, -5 + i, hw + 1, -4 + i, 0xFFFFFFFF);
		}
		g.pose().popMatrix();
	}

	private void drawHud(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		// Titel
		String dimName = minecraft.level.dimension() == Level.OVERWORLD ? "Oberwelt"
			: minecraft.level.dimension() == Level.NETHER ? "Nether"
			: minecraft.level.dimension() == Level.END ? "End"
			: minecraft.level.dimension().identifier().getPath();
		g.fill(4, 6, 12 + font.width("Weltkarte · " + dimName), 24, 0xA0000000);
		g.text(font, "§lWeltkarte §r§7· " + dimName, 8, 11, 0xFFFFFFFF, false);

		// Fußzeile
		int bx = (int) Math.floor(worldX(mouseX)), bz = (int) Math.floor(worldZ(mouseY));
		String coords = "X " + bx + "   Z " + bz;
		if (minecraft.level.dimension() == Level.OVERWORLD) coords += "   §7(Nether " + Math.floorDiv(bx, 8) + ", " + Math.floorDiv(bz, 8) + ")";
		else if (minecraft.level.dimension() == Level.NETHER) coords += "   §7(Oberwelt " + bx * 8 + ", " + bz * 8 + ")";
		String help = "§7Ziehen: verschieben · Mausrad: zoomen · Rechtsklick: Waypoint";
		String zoom = scale >= 1 ? Math.round(scale) + " px/Block" : "1 px = " + Math.round(1 / scale) + " Blöcke";
		boolean fits = font.width(coords) + font.width(help) + 20 < width;
		int barTop = fits ? height - 16 : height - 28;
		g.fill(0, barTop, width, height, 0xA0000000);
		g.text(font, coords, 6, height - 12, 0xFFFFFFFF, false);
		if (fits) {
			g.text(font, help, width - font.width(help) - 6, height - 12, 0xFFFFFFFF, false);
		} else {
			g.text(font, help, 6, height - 24, 0xFFFFFFFF, false);
		}
		g.text(font, "§7" + zoom, 6, barTop - 10, 0xFFFFFFFF, true);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		if (event.button() == 0) {
			dragging = true;
			dragDistance = 0;
			return true;
		}
		if (event.button() == 1) {
			Waypoint w = hoveredWaypoint(event.x(), event.y());
			Structures.Entry s = w == null ? hoveredStructure(event.x(), event.y()) : null;
			if (w != null) {
				minecraft.gui.setScreen(new WaypointEditScreen(this, w));
			} else if (s != null) {
				// Rechtsklick auf Struktur: Waypoint mit passendem Namen vorschlagen
				WaypointEditScreen edit = WaypointEditScreen.forPosition(this, s.x, s.y, s.z);
				edit.setSuggestedName(s.type.label);
				minecraft.gui.setScreen(edit);
			} else {
				int x = (int) Math.floor(worldX(event.x())), z = (int) Math.floor(worldZ(event.y()));
				int y = minecraft.level.hasChunk(x >> 4, z >> 4)
					? minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)
					: minecraft.player.getBlockY();
				minecraft.gui.setScreen(WaypointEditScreen.forPosition(this, x, y, z));
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (!dragging) return super.mouseDragged(event, dx, dy);
		camX -= dx / scale;
		camZ -= dy / scale;
		dragDistance += Math.abs(dx) + Math.abs(dy);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		if (scrollY == 0) return false;
		double wx = worldX(mx), wz = worldZ(my);
		scale = Mth.clamp(scale * (scrollY > 0 ? 1.25f : 0.8f), MIN_SCALE, MAX_SCALE);
		// Punkt unter der Maus bleibt an der gleichen Stelle
		camX = wx - (mx - width / 2.0) / scale;
		camZ = wz - (my - height / 2.0) / scale;
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (closeKey != null && closeKey.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		WorldMapStore.saveDirty();
		minecraft.gui.setScreen(parent);
	}
}
