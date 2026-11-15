package dev.arnesurv.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.arnesurv.ArneSurv;
import dev.arnesurv.config.ModConfig;
import dev.arnesurv.waypoint.WaypointManager;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.stats.Stat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Schreibt mit, wie sich die Vanilla-Statistiken verändern.
 * Gespeichert werden nur Änderungen (pro Minute gebündelt), damit man
 * Zeiträume wie "diese Session", "heute" oder "letzte 7 Tage" auswerten kann.
 */
public final class StatsRecorder {
	private static final Gson GSON = new GsonBuilder().create();
	private static final int REQUEST_INTERVAL_TICKS = 20 * 30;
	private static final long BUCKET_MILLIS = 60_000L;

	/** Gespeicherte Daten pro Welt/Server. */
	public static class Data {
		/** Letzter bekannter Gesamtwert jeder Statistik (Name → Wert). */
		public Map<String, Integer> lastKnown = new HashMap<>();
		/** Änderungen, je Minute gebündelt. */
		public List<Bucket> buckets = new ArrayList<>();
	}

	public static class Bucket {
		public long t;
		public Map<String, Integer> d = new HashMap<>();
	}

	private static String currentKey;
	private static Data data = new Data();
	private static Bucket open;
	private static long sessionStart;
	private static long ticks;
	private static boolean dirty;

	private StatsRecorder() {
	}

	public static Data data() {
		return data;
	}

	public static long sessionStart() {
		return sessionStart;
	}

	public static void tick(Minecraft mc) {
		String key = WaypointManager.worldKey(mc);
		if (key == null || !key.equals(currentKey)) {
			flush();
			save();
			currentKey = key;
			open = null;
			if (key != null) {
				load();
				sessionStart = System.currentTimeMillis();
				ticks = REQUEST_INTERVAL_TICKS - 40; // kurz nach dem Betreten einmal abfragen
			}
		}
		if (key == null || !ModConfig.get().statsEnabled) return;

		if (++ticks >= REQUEST_INTERVAL_TICKS && mc.getConnection() != null && mc.gui.screen() == null) {
			ticks = 0;
			mc.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
		}
		if (open != null && System.currentTimeMillis() - open.t >= BUCKET_MILLIS) {
			flush();
		}
		if (dirty && ticks % (20 * 60) == 0) save();
	}

	/** Vom Mixin aufgerufen, wenn der Server Statistiken schickt. */
	public static void onStats(Object2IntMap<Stat<?>> stats) {
		if (currentKey == null || !ModConfig.get().statsEnabled) return;
		long now = System.currentTimeMillis();
		for (Object2IntMap.Entry<Stat<?>> e : stats.object2IntEntrySet()) {
			String name = e.getKey().getName();
			int value = e.getIntValue();
			Integer old = data.lastKnown.put(name, value);
			// Erster Wert (noch nie gesehen): kein Zuwachs bekannt
			if (old == null || value <= old) continue;
			if (open == null) {
				open = new Bucket();
				open.t = now;
			}
			open.d.merge(name, value - old, Integer::sum);
		}
		dirty = true;
	}

	private static void flush() {
		if (open != null && !open.d.isEmpty()) {
			data.buckets.add(open);
			dirty = true;
		}
		open = null;
	}

	/** Summe aller Änderungen ab einem Zeitpunkt (inkl. der noch offenen Minute). */
	public static Map<String, Integer> sumSince(long since) {
		Map<String, Integer> sum = new HashMap<>();
		for (Bucket b : data.buckets) {
			if (b.t >= since) b.d.forEach((k, v) -> sum.merge(k, v, Integer::sum));
		}
		if (open != null && open.t >= since) open.d.forEach((k, v) -> sum.merge(k, v, Integer::sum));
		return sum;
	}

	/** Aufzeichnungen ab einem Zeitpunkt löschen (since = 0 → alles). */
	public static int deleteSince(long since) {
		flush();
		int before = data.buckets.size();
		data.buckets.removeIf(b -> b.t >= since);
		dirty = true;
		save();
		return before - data.buckets.size();
	}

	private static Path file() {
		return ModConfig.dir().resolve("stats").resolve(currentKey + ".json");
	}

	private static void load() {
		data = new Data();
		Path file = file();
		if (!Files.exists(file)) return;
		try {
			Data d = GSON.fromJson(Files.readString(file), Data.class);
			if (d != null) {
				if (d.lastKnown == null) d.lastKnown = new HashMap<>();
				if (d.buckets == null) d.buckets = new ArrayList<>();
				data = d;
			}
		} catch (Exception e) {
			ArneSurv.LOGGER.error("Statistik-Aufzeichnung konnte nicht geladen werden", e);
		}
	}

	public static void save() {
		if (currentKey == null || !dirty) return;
		try {
			Path file = file();
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(data));
			dirty = false;
		} catch (Exception e) {
			ArneSurv.LOGGER.error("Statistik-Aufzeichnung konnte nicht gespeichert werden", e);
		}
	}

	/** Beim Beenden des Spiels. */
	public static void shutdown() {
		flush();
		save();
	}
}
