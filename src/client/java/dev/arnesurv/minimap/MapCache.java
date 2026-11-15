package dev.arnesurv.minimap;

import dev.arnesurv.config.ModConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Berechnet pro Chunk die Farbe jeder Spalte (16×16) und hält sie im Speicher.
 * Chunks in der Nähe werden regelmäßig neu berechnet, damit Änderungen sichtbar werden.
 */
public final class MapCache {
	/** Chunk-Radius um den Spieler, der berechnet wird (reicht für den größten Zoom inkl. Drehung). */
	public static final int RADIUS = 9;
	private static final int BUDGET_PER_TICK = 8;
	private static final int REFRESH_TICKS = 100;
	private static final int[][] OFFSETS = buildOffsets();

	private static final Long2ObjectOpenHashMap<Tile> TILES = new Long2ObjectOpenHashMap<>();
	private static ClientLevel lastLevel;
	private static int lastMode = Integer.MIN_VALUE;
	private static long tickCounter;
	private static boolean changed;

	private static final class Tile {
		final int[] colors = new int[256];
		long computedAt;
	}

	private MapCache() {
	}

	private static int[][] buildOffsets() {
		List<int[]> list = new ArrayList<>();
		for (int dx = -RADIUS; dx <= RADIUS; dx++)
			for (int dz = -RADIUS; dz <= RADIUS; dz++) list.add(new int[]{dx, dz});
		list.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
		return list.toArray(new int[0][]);
	}

	/** true, wenn seit dem letzten Aufruf neue Farben berechnet wurden. */
	public static boolean consumeChanged() {
		boolean c = changed;
		changed = false;
		return c;
	}

	/** Aktueller Modus: Integer.MIN_VALUE = Oberfläche, sonst Start-Y für den Höhlenmodus. */
	public static int currentMode(Minecraft mc) {
		ModConfig cfg = ModConfig.get();
		LocalPlayer p = mc.player;
		ClientLevel level = mc.level;
		boolean cave = switch (cfg.minimapCaveMode) {
			case "ON" -> true;
			case "OFF" -> level.dimensionType().hasCeiling();
			default -> level.dimensionType().hasCeiling()
				|| p.getBlockY() < level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getBlockX(), p.getBlockZ()) - 3;
		};
		// Y in 4er-Schritten, damit nicht bei jedem Schritt alles neu berechnet wird
		return cave ? Math.floorDiv(p.getBlockY() + 2, 4) * 4 : Integer.MIN_VALUE;
	}

	public static boolean isCaveMode(Minecraft mc) {
		return lastMode != Integer.MIN_VALUE;
	}

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		if (level == null || player == null) {
			if (lastLevel != null) {
				TILES.clear();
				lastLevel = null;
			}
			return;
		}
		if (!ModConfig.get().minimapEnabled) return;
		tickCounter++;

		if (level != lastLevel) {
			TILES.clear();
			BlockColorCache.clear();
			lastLevel = level;
		}
		int mode = currentMode(mc);
		if (mode != lastMode) {
			TILES.clear();
			lastMode = mode;
			changed = true;
		}

		int pcx = player.getBlockX() >> 4, pcz = player.getBlockZ() >> 4;
		int budget = BUDGET_PER_TICK;
		for (int[] o : OFFSETS) {
			if (budget <= 0) break;
			int cx = pcx + o[0], cz = pcz + o[1];
			long key = ChunkPos.pack(cx, cz);
			Tile tile = TILES.get(key);
			if (tile != null && tickCounter - tile.computedAt < REFRESH_TICKS) continue;
			if (!level.hasChunk(cx, cz)) continue;
			if (tile == null) {
				tile = new Tile();
				TILES.put(key, tile);
			}
			compute(level, cx, cz, mode, tile);
			tile.computedAt = tickCounter;
			changed = true;
			budget--;
		}

		// Weit entfernte Chunks vergessen
		if (tickCounter % 200 == 0) {
			TILES.long2ObjectEntrySet().removeIf(e -> {
				long k = e.getLongKey();
				return Math.abs(ChunkPos.getX(k) - pcx) > RADIUS + 4 || Math.abs(ChunkPos.getZ(k) - pcz) > RADIUS + 4;
			});
		}
	}

	/** Farbe der Spalte (x, z), oder 0 (transparent), wenn noch unbekannt. */
	public static int colorAt(int x, int z) {
		Tile tile = TILES.get(ChunkPos.pack(x >> 4, z >> 4));
		return tile == null ? 0 : tile.colors[(z & 15) << 4 | (x & 15)];
	}

	private static void compute(ClientLevel level, int cx, int cz, int mode, Tile tile) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int lz = 0; lz < 16; lz++) {
			for (int lx = 0; lx < 16; lx++) {
				int x = (cx << 4) + lx, z = (cz << 4) + lz;
				tile.colors[lz << 4 | lx] = mode == Integer.MIN_VALUE
					? surfaceColor(level, x, z, pos)
					: caveColor(level, x, z, mode, pos);
			}
		}
	}

	private static int surfaceColor(ClientLevel level, int x, int z, BlockPos.MutableBlockPos pos) {
		int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
		if (y < level.getMinY()) return 0xFF000000;
		pos.set(x, y, z);
		BlockState state = level.getBlockState(pos);

		if (state.getFluidState().is(FluidTags.WATER)) {
			int depth = 0;
			BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos(x, y - 1, z);
			while (depth < 12 && level.getBlockState(below).getFluidState().is(FluidTags.WATER)) {
				depth++;
				below.move(0, -1, 0);
			}
			int water = BlockColorCache.color(state, level, pos);
			return BlockColorCache.shade(water, 1.05f - depth * 0.035f);
		}

		int color = BlockColorCache.color(state, level, pos);
		int north = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - 1) - 1;
		int diff = Math.max(-4, Math.min(4, y - north));
		return BlockColorCache.shade(color, 1f + diff * 0.07f);
	}

	private static int caveColor(ClientLevel level, int x, int z, int startY, BlockPos.MutableBlockPos pos) {
		int y = startY;
		int minY = level.getMinY();
		pos.set(x, y, z);
		// Wenn man oben in Stein startet: erst bis zum Hohlraum runter
		int steps = 0;
		while (steps < 24 && y > minY && !level.getBlockState(pos).isAir()) {
			y--;
			steps++;
			pos.setY(y);
		}
		if (steps >= 24) return 0xFF101010;
		// Dann bis zum Boden des Hohlraums
		steps = 0;
		while (steps < 48 && y > minY && level.getBlockState(pos).isAir()) {
			y--;
			steps++;
			pos.setY(y);
		}
		if (steps >= 48 || y <= minY) return 0xFF101010;
		BlockState state = level.getBlockState(pos);
		int color = BlockColorCache.color(state, level, pos);
		float depth = Math.min(1f, (startY - y) / 24f);
		return BlockColorCache.shade(color, 1.05f - depth * 0.45f);
	}
}
