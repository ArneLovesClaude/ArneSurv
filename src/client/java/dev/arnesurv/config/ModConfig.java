package dev.arnesurv.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.arnesurv.ArneSurv;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Alle Einstellungen von ArneSurv. Wird als config/arnesurv/config.json gespeichert.
 * Neue Felder einfach hier ergänzen – fehlende Werte in alten Dateien bekommen den Standardwert.
 */
public class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ModConfig instance = new ModConfig();

	/** Aktuelle Config-Version. Bei Änderungen an Standardwerten hochzählen und in migrate() behandeln. */
	private static final int CURRENT_VERSION = 2;
	/** 0 = Datei aus einer Version vor der Versionierung. */
	public int configVersion = 0;

	// ---- Modul: Info-HUD ----
	public boolean hudEnabled = true;
	public boolean hudCoords = true;
	public boolean hudBiome = true;
	public boolean hudDirection = true;
	public boolean hudTime = false;
	public boolean hudDay = false;
	public boolean hudFps = false;
	public boolean hudPing = false;
	public boolean hudPortalCoords = false;
	public boolean hudBackground = false;
	public int hudTextColor = 0xFFFFFFFF;
	public dev.arnesurv.hud.HudPos infoPos = new dev.arnesurv.hud.HudPos(0f, 0f, 1f);

	// ---- Modul: Kompassleiste ----
	public boolean compassEnabled = true;
	public dev.arnesurv.hud.HudPos compassPos = new dev.arnesurv.hud.HudPos(0.5f, 0f, 1f);

	// ---- Modul: Minimap ----
	public boolean minimapEnabled = true;
	public boolean minimapRound = false;
	public boolean minimapRotate = true;
	public int minimapSize = 120;
	/** Index in MinimapHud.ZOOM_LEVELS */
	public int minimapZoom = 1;
	/** AUTO, ON oder OFF */
	public String minimapCaveMode = "AUTO";
	public boolean minimapPlayers = true;
	public boolean minimapWaypoints = true;
	public dev.arnesurv.hud.HudPos minimapPos = new dev.arnesurv.hud.HudPos(1f, 0f, 1f);

	// ---- Modul: Waypoints ----
	public boolean waypointsEnabled = true;
	public boolean deathWaypoints = true;
	public int maxDeathWaypoints = 5;

	public static ModConfig get() {
		return instance;
	}

	public static Path dir() {
		return FabricLoader.getInstance().getConfigDir().resolve(ArneSurv.MOD_ID);
	}

	private static Path file() {
		return dir().resolve("config.json");
	}

	public static void load() {
		Path file = file();
		if (Files.exists(file)) {
			try {
				ModConfig loaded = GSON.fromJson(Files.readString(file), ModConfig.class);
				if (loaded != null) instance = loaded;
			} catch (Exception e) {
				ArneSurv.LOGGER.error("Config konnte nicht gelesen werden, nutze Standardwerte", e);
			}
		}
		instance.migrate();
		save();
	}

	private void migrate() {
		if (configVersion < 2) {
			// v0.1.0 hatte zu viele HUD-Zeilen an – auf die neuen, schlichteren Standardwerte setzen
			ModConfig d = new ModConfig();
			hudTime = d.hudTime;
			hudFps = d.hudFps;
			hudPing = d.hudPing;
			hudPortalCoords = d.hudPortalCoords;
			hudBackground = d.hudBackground;
		}
		if (infoPos == null) infoPos = new ModConfig().infoPos;
		if (compassPos == null) compassPos = new ModConfig().compassPos;
		if (minimapPos == null) minimapPos = new ModConfig().minimapPos;
		if (minimapCaveMode == null) minimapCaveMode = "AUTO";
		configVersion = CURRENT_VERSION;
	}

	public static void save() {
		try {
			Files.createDirectories(dir());
			Files.writeString(file(), GSON.toJson(instance));
		} catch (IOException e) {
			ArneSurv.LOGGER.error("Config konnte nicht gespeichert werden", e);
		}
	}
}
