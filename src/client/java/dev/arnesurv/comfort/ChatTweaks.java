package dev.arnesurv.comfort;

import dev.arnesurv.config.ModConfig;
import dev.arnesurv.waypoint.WaypointShare;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Zeitstempel, Erwähnungen und klickbare geteilte Waypoints im Chat. */
public final class ChatTweaks {
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	private ChatTweaks() {
	}

	/** Chatnachricht im Format "&lt;Absender&gt; Text" */
	private static final Pattern CHAT = Pattern.compile("<([^<>\\s]{1,40})> (.*)");

	/**
	 * Erwähnung = jemand anderes schreibt deinen Namen in den Chat.
	 * Systemnachrichten (z.B. "X hat den Fortschritt … erzielt") zählen nicht.
	 */
	public static boolean isMention(String text, String name) {
		Matcher m = CHAT.matcher(text);
		if (!m.find()) return false;
		if (m.group(1).equalsIgnoreCase(name)) return false;
		return Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE)
			.matcher(m.group(2)).find();
	}

	public static Component process(Component message) {
		ModConfig cfg = ModConfig.get();
		Minecraft mc = Minecraft.getInstance();
		String text = message.getString();
		MutableComponent result = Component.empty();

		if (cfg.chatTimestamps) {
			result.append(Component.literal("§7[" + LocalTime.now().format(TIME) + "] "));
		}

		boolean mention = cfg.chatMentions && mc.player != null && isMention(text, mc.player.getGameProfile().name());
		if (mention) {
			result.append(Component.literal("§6§l» §r"));
			mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f, 0.6f));
		}

		result.append(message);

		Component share = WaypointShare.clickableFor(text);
		if (share != null) result.append(share);

		return (cfg.chatTimestamps || mention || share != null) ? result : message;
	}
}
