package dev.arnesurv.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.arnesurv.ArneSurv;
import dev.arnesurv.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hält die Waypoints der aktuellen Welt bzw. des aktuellen Servers.
 * Pro Server/Welt gibt es eine eigene Datei unter config/arnesurv/waypoints/.
 */
public final class WaypointManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static String currentKey = null;
	private static List<Waypoint> waypoints = new ArrayList<>();
	private static boolean wasDead = false;

	private WaypointManager() {
	}

	public static List<Waypoint> all() {
		return waypoints;
	}

	public static List<Waypoint> inDimension(String dimension) {
		List<Waypoint> list = new ArrayList<>();
		for (Waypoint w : waypoints) if (dimension.equals(w.dimension)) list.add(w);
		return list;
	}

	public static void add(Waypoint w) {
		waypoints.add(w);
		save();
	}

	public static void remove(Waypoint w) {
		waypoints.remove(w);
		save();
	}

	/** Jeden Tick aufrufen: lädt bei Welt-/Serverwechsel die passende Datei und erkennt Tode. */
	public static void tick(Minecraft mc) {
		String key = worldKey(mc);
		if (key == null) {
			currentKey = null;
			waypoints = new ArrayList<>();
			wasDead = false;
			return;
		}
		if (!key.equals(currentKey)) {
			currentKey = key;
			load();
		}

		LocalPlayer player = mc.player;
		if (player == null) return;
		boolean dead = player.isDeadOrDying();
		if (dead && !wasDead && ModConfig.get().waypointsEnabled && ModConfig.get().deathWaypoints) {
			addDeathWaypoint(player, mc.level.dimension().identifier().toString());
		}
		wasDead = dead;
	}

	private static void addDeathWaypoint(LocalPlayer player, String dimension) {
		String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
		Waypoint w = new Waypoint("Tod " + time, player.getBlockX(), player.getBlockY(), player.getBlockZ(), dimension, 0xFFFF5555);
		w.death = true;
		waypoints.add(w);

		// Nur die letzten X Todes-Waypoints behalten
		List<Waypoint> deaths = new ArrayList<>(waypoints.stream().filter(p -> p.death).toList());
		deaths.sort(Comparator.comparingLong(p -> p.created));
		int max = Math.max(1, ModConfig.get().maxDeathWaypoints);
		while (deaths.size() > max) waypoints.remove(deaths.remove(0));
		save();
	}

	/** Eindeutiger Name für den aktuellen Server bzw. die Einzelspielerwelt, oder null. */
	public static String worldKey(Minecraft mc) {
		if (mc.level == null || mc.player == null) return null;
		ServerData server = mc.getCurrentServer();
		if (server != null && !mc.isLocalServer()) return "server_" + sanitize(server.ip);
		if (mc.getSingleplayerServer() != null) {
			return "welt_" + sanitize(mc.getSingleplayerServer().getWorldData().getLevelName());
		}
		return "unbekannt";
	}

	public static String sanitize(String s) {
		return s.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
	}

	private static Path file() {
		return ModConfig.dir().resolve("waypoints").resolve(currentKey + ".json");
	}

	private static void load() {
		waypoints = new ArrayList<>();
		Path file = file();
		if (!Files.exists(file)) return;
		try {
			List<Waypoint> loaded = GSON.fromJson(Files.readString(file), new TypeToken<List<Waypoint>>() {}.getType());
			if (loaded != null) waypoints = new ArrayList<>(loaded);
		} catch (Exception e) {
			ArneSurv.LOGGER.error("Waypoints konnten nicht geladen werden: {}", file, e);
		}
	}

	public static void save() {
		if (currentKey == null) return;
		try {
			Path file = file();
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(waypoints));
		} catch (Exception e) {
			ArneSurv.LOGGER.error("Waypoints konnten nicht gespeichert werden", e);
		}
	}
}
