package dev.arnesurv.overlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.arnesurv.ArneSurv;
import dev.arnesurv.config.ModConfig;
import dev.arnesurv.waypoint.WaypointManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Merkt sich Strukturen, die man gesehen hat. Ein Client kennt Strukturen nicht direkt –
 * deshalb werden sie an typischen Blöcken erkannt (z.B. Glocke = Dorf, Endportalrahmen = Festung).
 */
public final class Structures {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Strukturtyp: Erkennungsblock, Anzeige, Farbe, Zusammenfass-Radius. */
	public enum Type {
		VILLAGE("bell", "Dorf", "D", 0xFFE0A030, 96),
		STRONGHOLD("end_portal_frame", "Festung", "F", 0xFF60D0A0, 128),
		SPAWNER("spawner", "Spawner", "S", 0xFF9090A0, 12),
		TRIAL_CHAMBER("trial_spawner", "Trial Chamber", "T", 0xFFD07030, 64),
		ANCIENT_CITY("reinforced_deepslate", "Antike Stadt", "A", 0xFF30A0B0, 128),
		BASTION("gilded_blackstone", "Bastion", "B", 0xFFC0A040, 96),
		FORTRESS("nether_brick_fence", "Netherfestung", "N", 0xFFB04040, 160),
		END_CITY("purpur_pillar", "Endstadt", "E", 0xFFC080C0, 64),
		MONUMENT("dark_prismarine", "Ozeanmonument", "M", 0xFF40B0A0, 96);

		public final String block;
		public final String label;
		public final String letter;
		public final int color;
		final int mergeRadius;
		Block blockRef;

		Type(String block, String label, String letter, int color, int mergeRadius) {
			this.block = block;
			this.label = label;
			this.letter = letter;
			this.color = color;
			this.mergeRadius = mergeRadius;
		}
	}

	public static class Entry {
		public Type type;
		public int x, y, z;
		public String dimension;

		public Entry() {
		}

		Entry(Type type, int x, int y, int z, String dimension) {
			this.type = type;
			this.x = x;
			this.y = y;
			this.z = z;
			this.dimension = dimension;
		}
	}

	private static String currentKey;
	private static List<Entry> entries = new ArrayList<>();
	private static final Map<String, LongOpenHashSet> SCANNED = new HashMap<>();

	private Structures() {
	}

	public static List<Entry> inDimension(String dimension) {
		List<Entry> list = new ArrayList<>();
		for (Entry e : entries) if (dimension.equals(e.dimension)) list.add(e);
		return list;
	}

	public static int count() {
		return entries.size();
	}

	public static void clearAll() {
		entries.clear();
		SCANNED.clear();
		save();
	}

	public static void tick(Minecraft mc) {
		String key = WaypointManager.worldKey(mc);
		if (key == null) {
			currentKey = null;
			entries = new ArrayList<>();
			SCANNED.clear();
			return;
		}
		if (!key.equals(currentKey)) {
			currentKey = key;
			SCANNED.clear();
			load();
		}
	}

	/** Wird für jeden neu gesehenen Chunk aufgerufen. Prüft per Palette nur, ob ein Erkennungsblock drin ist. */
	public static void scan(ClientLevel level, int cx, int cz) {
		if (currentKey == null || !ModConfig.get().overlayStructures) return;
		String dim = level.dimension().identifier().toString();
		if (!SCANNED.computeIfAbsent(dim, d -> new LongOpenHashSet()).add(ChunkPos.pack(cx, cz))) return;

		LevelChunk chunk = level.getChunk(cx, cz);
		LevelChunkSection[] sections = chunk.getSections();
		boolean added = false;
		for (Type type : Type.values()) {
			if (type.blockRef == null) {
				type.blockRef = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(type.block));
			}
			Block target = type.blockRef;
			for (int i = 0; i < sections.length; i++) {
				LevelChunkSection section = sections[i];
				if (section == null || section.hasOnlyAir() || !section.maybeHas(s -> s.is(target))) continue;
				int sy = level.getSectionYFromSectionIndex(i);
				int[] found = find(section, target);
				if (found == null) continue;
				int x = (cx << 4) + found[0], y = (sy << 4) + found[1], z = (cz << 4) + found[2];
				ArneSurv.LOGGER.debug("Struktur erkannt: {} bei {} {} {}", type, x, y, z);
				if (addIfNew(type, x, y, z, dim)) added = true;
				break;
			}
		}
		if (added) save();
	}

	/** Nur für den Entwickler-Autotest: Chunk erneut scannen. */
	public static void devRescan(ClientLevel level, int cx, int cz) {
		var set = SCANNED.get(level.dimension().identifier().toString());
		if (set != null) set.remove(ChunkPos.pack(cx, cz));
		scan(level, cx, cz);
	}

	private static int[] find(LevelChunkSection section, Block target) {
		for (int y = 0; y < 16; y++)
			for (int z = 0; z < 16; z++)
				for (int x = 0; x < 16; x++) {
					BlockState s = section.getBlockState(x, y, z);
					if (s.is(target)) return new int[]{x, y, z};
				}
		return null;
	}

	private static boolean addIfNew(Type type, int x, int y, int z, String dim) {
		int r = type.mergeRadius;
		for (Entry e : entries) {
			if (e.type == type && dim.equals(e.dimension)) {
				int dx = e.x - x, dz = e.z - z;
				if (dx * dx + dz * dz <= r * r) return false;
			}
		}
		entries.add(new Entry(type, x, y, z, dim));
		return true;
	}

	private static Path file() {
		return ModConfig.dir().resolve("structures").resolve(currentKey + ".json");
	}

	private static void load() {
		entries = new ArrayList<>();
		Path file = file();
		if (!Files.exists(file)) return;
		try {
			List<Entry> loaded = GSON.fromJson(Files.readString(file), new TypeToken<List<Entry>>() {}.getType());
			if (loaded != null) {
				for (Entry e : loaded) if (e.type != null && e.dimension != null) entries.add(e);
			}
		} catch (Exception e) {
			ArneSurv.LOGGER.error("Strukturen konnten nicht geladen werden", e);
		}
	}

	private static void save() {
		if (currentKey == null) return;
		try {
			Path file = file();
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(entries));
		} catch (Exception e) {
			ArneSurv.LOGGER.error("Strukturen konnten nicht gespeichert werden", e);
		}
	}
}
