package dev.arnesurv.waypoint;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Waypoints über den Chat teilen. Die Nachricht ist für alle lesbar
 * ("[ArneSurv] Waypoint "Basis" bei 100 64 -20 (minecraft:overworld)");
 * wer ArneSurv hat, bekommt dahinter einen klickbaren [Übernehmen]-Knopf.
 */
public final class WaypointShare {
	private static final Pattern PATTERN = Pattern.compile(
		"\\[ArneSurv] Waypoint \"([^\"]{1,48})\" bei (-?\\d{1,8}) (-?\\d{1,4}) (-?\\d{1,8}) \\(([a-z0-9_.\\-]+:[a-z0-9_./\\-]+)\\)");

	private WaypointShare() {
	}

	public static String message(Waypoint w) {
		String name = w.name.replace("\"", "'");
		return "[ArneSurv] Waypoint \"" + name + "\" bei " + w.x + " " + w.y + " " + w.z + " (" + w.dimension + ")";
	}

	/** Schickt den Waypoint in den Chat (der Spieler klickt dafür selbst auf "Teilen"). */
	public static void share(Waypoint w) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.getConnection() == null) return;
		mc.getConnection().sendChat(message(w));
	}

	/** Klickbarer Zusatz für eine Chatnachricht, oder null, wenn kein geteilter Waypoint drin ist. */
	public static Component clickableFor(String text) {
		Matcher m = PATTERN.matcher(text);
		if (!m.find()) return null;
		String cmd = "/arnesurv waypoint add " + m.group(2) + " " + m.group(3) + " " + m.group(4)
			+ " \"" + m.group(5) + "\" " + m.group(1);
		return Component.literal(" [Übernehmen]").withStyle(Style.EMPTY
			.withColor(0x55FF55)
			.withClickEvent(new ClickEvent.RunCommand(cmd))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Waypoint \"" + m.group(1) + "\" zu deinen Waypoints hinzufügen"))));
	}

	public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("arnesurv")
			.then(ClientCommands.literal("waypoint")
				.then(ClientCommands.literal("add")
					.then(ClientCommands.argument("x", IntegerArgumentType.integer())
						.then(ClientCommands.argument("y", IntegerArgumentType.integer())
							.then(ClientCommands.argument("z", IntegerArgumentType.integer())
								.then(ClientCommands.argument("dimension", StringArgumentType.string())
									.then(ClientCommands.argument("name", StringArgumentType.greedyString())
										.executes(ctx -> {
											String name = StringArgumentType.getString(ctx, "name");
											if (name.length() > 48) name = name.substring(0, 48);
											Waypoint w = new Waypoint(name,
												IntegerArgumentType.getInteger(ctx, "x"),
												IntegerArgumentType.getInteger(ctx, "y"),
												IntegerArgumentType.getInteger(ctx, "z"),
												StringArgumentType.getString(ctx, "dimension"),
												WaypointColors.COLORS[WaypointManager.all().size() % WaypointColors.COLORS.length]);
											WaypointManager.add(w);
											ctx.getSource().sendFeedback(Component.literal("§a[ArneSurv] Waypoint \"" + name + "\" hinzugefügt."));
											return 1;
										})))))))));
	}
}
