package dev.arnesurv.worldmap;

import com.mojang.blaze3d.platform.NativeImage;
import dev.arnesurv.ArneSurv;
import dev.arnesurv.config.ModConfig;
import dev.arnesurv.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Speichert alles, was man schon einmal gesehen hat, dauerhaft auf der Festplatte.
 * Die Welt ist in Regionen à 512×512 Blöcke (32×32 Chunks) aufgeteilt;
 * jede Region ist ein PNG unter config/arnesurv/maps/&lt;welt&gt;/&lt;dimension&gt;/.
 */
public final class WorldMapStore {
	public static final int REGION = 512;
	private static final int MAX_LOADED = 48;
	private static final int SAVE_INTERVAL_TICKS = 20 * 30;

	/** Regionen in LRU-Reihenfolge (zuletzt benutzte am Ende). */
	private static final LinkedHashMap<Long, Region> REGIONS = new LinkedHashMap<>(64, 0.75f, true);
	private static String worldKey;
	private static String dimension;
	private static long ticks;

	public static final class Region {
		public final int rx, rz;
		final DynamicTexture texture;
		boolean dirty;
		boolean textureDirty;

		Region(int rx, int rz, NativeImage image) {
			this.rx = rx;
			this.rz = rz;
			this.texture = new DynamicTexture(() -> "arnesurv_region_" + rx + "_" + rz, image);
		}

		public DynamicTexture texture() {
			if (textureDirty) {
				texture.upload();
				textureDirty = false;
			}
			return texture;
		}
	}

	private WorldMapStore() {
	}

	private static long key(int rx, int rz) {
		return ((long) rx << 32) | (rz & 0xFFFFFFFFL);
	}

	/** Jeden Tick: bei Welt- oder Dimensionswechsel speichern und leeren, regelmäßig speichern. */
	public static void tick(Minecraft mc) {
		String w = WaypointManager.worldKey(mc);
		String d = mc.level == null ? null : mc.level.dimension().identifier().toString();
		if (w == null || !w.equals(worldKey) || !d.equals(dimension)) {
			saveAndUnloadAll();
			worldKey = w;
			dimension = d;
		}
		if (w != null && ++ticks % SAVE_INTERVAL_TICKS == 0) saveDirty();
	}

	/** Übernimmt die Farben eines Chunks (16×16, Index z*16+x) in die Karte. */
	public static void putChunk(int cx, int cz, int[] colors) {
		if (worldKey == null) return;
		Region r = region(Math.floorDiv(cx, 32), Math.floorDiv(cz, 32), true);
		if (r == null) return;
		NativeImage img = r.texture.getPixels();
		int ox = Math.floorMod(cx, 32) * 16, oz = Math.floorMod(cz, 32) * 16;
		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				int c = colors[z << 4 | x];
				if (c != 0) img.setPixel(ox + x, oz + z, c);
			}
		}
		r.dirty = true;
		r.textureDirty = true;
	}

	/** Region holen; lädt sie bei Bedarf von der Festplatte. create = neu anlegen, wenn es keine gibt. */
	public static Region region(int rx, int rz, boolean create) {
		if (worldKey == null) return null;
		long k = key(rx, rz);
		Region r = REGIONS.get(k);
		if (r != null) return r;

		Path file = file(rx, rz);
		NativeImage img = null;
		if (Files.exists(file)) {
			try (var in = Files.newInputStream(file)) {
				img = NativeImage.read(in);
			} catch (Exception e) {
				ArneSurv.LOGGER.warn("Kartenregion defekt, wird neu angelegt: {}", file, e);
			}
		}
		if (img == null) {
			if (!create) return null;
			img = new NativeImage(REGION, REGION, true);
			img.fillRect(0, 0, REGION, REGION, 0);
		}
		r = new Region(rx, rz, img);
		REGIONS.put(k, r);
		evict();
		return r;
	}

	/** Gibt es für diese Region schon Daten (geladen oder als Datei)? */
	public static boolean exists(int rx, int rz) {
		return worldKey != null && (REGIONS.containsKey(key(rx, rz)) || Files.exists(file(rx, rz)));
	}

	private static void evict() {
		var it = REGIONS.entrySet().iterator();
		while (REGIONS.size() > MAX_LOADED && it.hasNext()) {
			Region r = it.next().getValue();
			save(r);
			r.texture.close();
			it.remove();
		}
	}

	private static Path dir() {
		return ModConfig.dir().resolve("maps").resolve(worldKey).resolve(WaypointManager.sanitize(dimension));
	}

	private static Path file(int rx, int rz) {
		return dir().resolve("r." + rx + "." + rz + ".png");
	}

	private static void save(Region r) {
		if (!r.dirty) return;
		try {
			Files.createDirectories(dir());
			r.texture.getPixels().writeToFile(file(r.rx, r.rz));
			r.dirty = false;
		} catch (Exception e) {
			ArneSurv.LOGGER.error("Kartenregion konnte nicht gespeichert werden", e);
		}
	}

	public static void saveDirty() {
		if (worldKey == null) return;
		for (Region r : REGIONS.values()) save(r);
	}

	public static void saveAndUnloadAll() {
		if (worldKey != null) {
			for (Map.Entry<Long, Region> e : REGIONS.entrySet()) {
				save(e.getValue());
				e.getValue().texture.close();
			}
		}
		REGIONS.clear();
	}
}
