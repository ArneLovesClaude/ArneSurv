package dev.arnesurv.hud;

import dev.arnesurv.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Schlichte Info-Anzeige: Koordinaten, Biom, Richtung und optional mehr. */
public final class InfoHud extends MovableHud {
	public static final InfoHud INSTANCE = new InfoHud();
	private static final int PADDING = 3;

	private InfoHud() {
	}

	@Override
	public String displayName() {
		return "Info-Anzeige";
	}

	@Override
	public HudPos pos() {
		return ModConfig.get().infoPos;
	}

	@Override
	public boolean enabled() {
		return ModConfig.get().hudEnabled;
	}

	private List<Component> lines(Minecraft mc, boolean preview) {
		ModConfig cfg = ModConfig.get();
		List<Component> lines = new ArrayList<>();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) {
			if (preview) lines.add(Component.literal("128, 64, -256"));
			return lines;
		}
		BlockPos pos = player.blockPosition();

		if (cfg.hudCoords) {
			lines.add(Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
		}
		if (cfg.hudPortalCoords) {
			if (mc.level.dimension() == Level.OVERWORLD) {
				lines.add(Component.literal("Nether: " + Math.floorDiv(pos.getX(), 8) + ", " + Math.floorDiv(pos.getZ(), 8)));
			} else if (mc.level.dimension() == Level.NETHER) {
				lines.add(Component.literal("Oberwelt: " + pos.getX() * 8 + ", " + pos.getZ() * 8));
			}
		}
		if (cfg.hudBiome) {
			mc.level.getBiome(pos).unwrapKey().ifPresent(key -> {
				var id = key.identifier();
				lines.add(Component.translatable("biome." + id.getNamespace() + "." + id.getPath()));
			});
		}
		if (cfg.hudDirection) {
			lines.add(Component.literal(directionName(player.getYRot())));
		}
		if (cfg.hudTime || cfg.hudDay) {
			long time = mc.level.getOverworldClockTime();
			List<String> parts = new ArrayList<>();
			if (cfg.hudTime) {
				long t = (time + 6000) % 24000; // 0 Ticks = 06:00 Uhr
				parts.add(String.format("%02d:%02d Uhr", t / 1000, (t % 1000) * 60 / 1000));
			}
			if (cfg.hudDay) parts.add("Tag " + (time / 24000 + 1));
			lines.add(Component.literal(String.join("  ·  ", parts)));
		}
		if (cfg.hudFps || cfg.hudPing) {
			List<String> parts = new ArrayList<>();
			if (cfg.hudFps) parts.add(mc.getFps() + " FPS");
			if (cfg.hudPing && !mc.isLocalServer() && mc.getConnection() != null) {
				PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
				if (info != null) parts.add(info.getLatency() + " ms");
			}
			if (!parts.isEmpty()) lines.add(Component.literal(String.join("  ·  ", parts)));
		}
		if (lines.isEmpty() && preview) lines.add(Component.literal("(alle Zeilen aus)"));
		return lines;
	}

	@Override
	public int width(Minecraft mc, boolean preview) {
		int width = 0;
		for (Component c : lines(mc, preview)) width = Math.max(width, mc.font.width(c));
		return width == 0 ? 0 : width + PADDING * 2;
	}

	@Override
	public int height(Minecraft mc, boolean preview) {
		int n = lines(mc, preview).size();
		return n == 0 ? 0 : n * (mc.font.lineHeight + 1) + PADDING * 2 - 1;
	}

	@Override
	public void renderHud(net.minecraft.client.gui.GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
		if (Minecraft.getInstance().debugEntries.isOverlayVisible()) return; // F3 hat Vorrang
		super.renderHud(g, delta);
	}

	@Override
	protected void draw(GuiGraphicsExtractor g, Minecraft mc, boolean preview) {
		ModConfig cfg = ModConfig.get();
		List<Component> lines = lines(mc, preview);
		if (cfg.hudBackground) {
			g.fill(0, 0, width(mc, preview), height(mc, preview), 0x80000000);
		}
		int y = PADDING;
		for (Component c : lines) {
			g.text(mc.font, c, PADDING, y, cfg.hudTextColor, true);
			y += mc.font.lineHeight + 1;
		}
	}

	/** Minecraft-Yaw: 0 = Süden, 90 = Westen, 180 = Norden, 270 = Osten */
	static String directionName(float yaw) {
		String[] names = {"Süden", "Südwest", "Westen", "Nordwest", "Norden", "Nordost", "Osten", "Südost"};
		float y = ((yaw % 360) + 360) % 360;
		int idx = Math.round(y / 45f) % 8;
		String axis = switch (Math.round(y / 90f) % 4) {
			case 0 -> "+Z";
			case 1 -> "-X";
			case 2 -> "-Z";
			default -> "+X";
		};
		return names[idx] + " (" + axis + ")";
	}
}
