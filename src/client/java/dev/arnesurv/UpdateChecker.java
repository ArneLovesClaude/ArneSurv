package dev.arnesurv;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.arnesurv.config.ModConfig;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fragt beim Start einmal bei GitHub nach der neuesten Version.
 * Gibt es eine neuere, erscheint im Hauptmenü ein Knopf, der zur Download-Seite führt.
 * Es wird nichts automatisch heruntergeladen oder installiert.
 */
public final class UpdateChecker {
	private static final String API = "https://api.github.com/repos/ArneLovesClaude/ArneSurv/releases/latest";

	private static volatile String latestVersion;
	private static volatile String latestUrl;

	private UpdateChecker() {
	}

	public static String currentVersion() {
		return FabricLoader.getInstance().getModContainer(ArneSurv.MOD_ID)
			.map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("0");
	}

	public static void checkAsync() {
		if (!ModConfig.get().updateCheck) return;
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
		HttpRequest req = HttpRequest.newBuilder(URI.create(API))
			.header("Accept", "application/vnd.github+json")
			.header("User-Agent", "ArneSurv-UpdateCheck")
			.timeout(Duration.ofSeconds(10))
			.GET().build();
		client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(res -> {
			if (res.statusCode() != 200) return;
			try {
				JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
				String tag = json.get("tag_name").getAsString();
				String version = tag.startsWith("v") ? tag.substring(1) : tag;
				if (isNewer(version, currentVersion())) {
					latestVersion = version;
					latestUrl = json.get("html_url").getAsString();
					ArneSurv.LOGGER.info("ArneSurv-Update verfügbar: {} (installiert: {})", version, currentVersion());
				}
			} catch (Exception e) {
				ArneSurv.LOGGER.debug("Update-Prüfung fehlgeschlagen", e);
			}
		}).exceptionally(e -> null);
	}

	/** Vergleicht Versionen wie 1.2.10 > 1.2.9. */
	static boolean isNewer(String a, String b) {
		String[] pa = a.split("[.\\-+]"), pb = b.split("[.\\-+]");
		for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
			int x = i < pa.length ? num(pa[i]) : 0, y = i < pb.length ? num(pb[i]) : 0;
			if (x != y) return x > y;
		}
		return false;
	}

	private static int num(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** Knopf im Hauptmenü, wenn ein Update da ist. */
	public static void onScreenInit(Minecraft mc, Screen screen) {
		if (!(screen instanceof TitleScreen) || latestVersion == null || !ModConfig.get().updateCheck) return;
		String url = latestUrl;
		Button b = Button.builder(Component.literal("§eArneSurv " + latestVersion + " verfügbar"),
				btn -> ConfirmLinkScreen.confirmLinkNow(screen, URI.create(url)))
			.bounds(4, 4, 150, 20)
			.tooltip(Tooltip.create(Component.literal("Öffnet die Download-Seite auf GitHub. Installiert ist " + currentVersion() + ".")))
			.build();
		Screens.getWidgets(screen).add(b);
	}
}
