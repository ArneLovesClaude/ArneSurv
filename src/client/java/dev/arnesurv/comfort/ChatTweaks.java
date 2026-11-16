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

	/**
	 * Erkannte Chat-Formate (Gruppe 1 = Absender, Gruppe 2 = Text):
	 * "&lt;Name&gt; Text" (Vanilla/Paper), "Name: Text", "Name » Text", "Name > Text", "Name | Text" –
	 * jeweils auch mit vorangestellten Rängen wie "[Admin] Name: Text".
	 */
	private static final Pattern[] CHAT_FORMATS = {
		Pattern.compile("<([^<>\\s]{1,40})> (.*)"),
		Pattern.compile("^(?:\\[[^\\]]{0,24}\\]\\s*)*(?:[^\\sA-Za-z0-9_]\\s*)?([A-Za-z0-9_]{2,16})\\s*(?::|»|>>|>|->|\\|)\\s+(.*)$")
	};

	/**
	 * Erwähnung = jemand anderes schreibt deinen Namen in den Chat.
	 * Systemnachrichten (z.B. "X hat den Fortschritt … erzielt") zählen nicht.
	 */
	public static boolean isMention(String text, String name) {
		for (Pattern format : CHAT_FORMATS) {
			Matcher m = format.matcher(text);
			if (!m.find()) continue;
			if (m.group(1).equalsIgnoreCase(name)) return false;
			return Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE)
				.matcher(m.group(2)).find();
		}
		return false;
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
