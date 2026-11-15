package dev.arnesurv.minimap;

import com.mojang.blaze3d.platform.NativeImage;
import dev.arnesurv.ArneSurv;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ermittelt die Kartenfarbe eines Blocks aus dem Durchschnitt seiner echten Textur
 * (funktioniert damit auch mit Resource Packs) und färbt Gras/Laub/Wasser nach Biom.
 */
public final class BlockColorCache {
	private static final Map<Block, Integer> BASE = new HashMap<>();
	private static final String[] SUFFIXES = {
		"_slab", "_stairs", "_wall", "_fence_gate", "_fence", "_pressure_plate", "_button",
		"_carpet", "_pane", "_wall_hanging_sign", "_hanging_sign", "_wall_sign", "_sign", "_door", "_trapdoor"
	};

	private BlockColorCache() {
	}

	public static void clear() {
		BASE.clear();
	}

	/** Endgültige Farbe (ARGB) inkl. Biom-Färbung. */
	public static int color(BlockState state, ClientLevel level, BlockPos pos) {
		int base = BASE.computeIfAbsent(state.getBlock(), b -> compute(state));
		if (state.getBlock() == Blocks.WATER) {
			// Wasser hat keine Block-Tint-Quelle, die Farbe kommt vom Biom
			return multiply(base, BiomeColors.getAverageWaterColor(level, pos));
		}
		BlockTintSource tint = Minecraft.getInstance().getBlockColors().getTintSource(state, 0);
		if (tint != null) {
			try {
				base = multiply(base, tint.colorInWorld(state, level, pos));
			} catch (Exception ignored) {
				// manche Tint-Quellen brauchen Daten, die es hier nicht gibt – dann ungefärbt
			}
		}
		return base;
	}

	private static int compute(BlockState state) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		for (String name : candidates(id.getPath())) {
			Integer c = average(Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/block/" + name + ".png"));
			if (c != null) return c;
		}
		// Fallback: Farbe wie auf Vanilla-Karten
		return 0xFF000000 | state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
	}

	private static List<String> candidates(String path) {
		List<String> list = new ArrayList<>();
		switch (path) {
			case "water" -> list.add("water_still");
			case "lava" -> list.add("lava_still");
			default -> {
			}
		}
		list.add(path + "_top");
		list.add(path);
		for (String suffix : SUFFIXES) {
			if (path.endsWith(suffix)) {
				String base = path.substring(0, path.length() - suffix.length());
				list.add(base + "_planks");
				list.add(base + "s");
				list.add(base + "_top");
				list.add(base);
				list.add(base + "_wool");
				break;
			}
		}
		return list;
	}

	/** Durchschnittsfarbe der (ersten Frame der) Textur, nur nicht-transparente Pixel. */
	private static Integer average(Identifier texture) {
		var rm = Minecraft.getInstance().getResourceManager();
		if (rm.getResource(texture).isEmpty()) return null;
		try (InputStream in = rm.open(texture); NativeImage img = NativeImage.read(in)) {
			int w = img.getWidth();
			int h = Math.min(img.getHeight(), w); // animierte Texturen: nur erstes Frame
			long r = 0, g = 0, b = 0, n = 0;
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int p = img.getPixel(x, y);
					if ((p >>> 24) < 128) continue;
					r += (p >> 16) & 0xFF;
					g += (p >> 8) & 0xFF;
					b += p & 0xFF;
					n++;
				}
			}
			if (n == 0) return null;
			return 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
		} catch (Exception e) {
			ArneSurv.LOGGER.debug("Textur nicht lesbar: {}", texture, e);
			return null;
		}
	}

	static int multiply(int argb, int rgb) {
		int r = ((argb >> 16) & 0xFF) * ((rgb >> 16) & 0xFF) / 255;
		int g = ((argb >> 8) & 0xFF) * ((rgb >> 8) & 0xFF) / 255;
		int b = (argb & 0xFF) * (rgb & 0xFF) / 255;
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	static int shade(int argb, float factor) {
		int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * factor));
		int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * factor));
		int b = Math.min(255, Math.round((argb & 0xFF) * factor));
		return 0xFF000000 | r << 16 | g << 8 | b;
	}
}
