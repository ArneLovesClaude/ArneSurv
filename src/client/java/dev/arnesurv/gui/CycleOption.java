package dev.arnesurv.gui;

import dev.arnesurv.config.ModConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/** Knopf, der bei jedem Klick zur nächsten Option weiterschaltet, z.B. "Größe: 120". */
public final class CycleOption {
	private CycleOption() {
	}

	public static Button create(Supplier<String> label, Runnable next, int x, int y, int width) {
		return Button.builder(Component.literal(label.get()), b -> {
			next.run();
			ModConfig.save();
			b.setMessage(Component.literal(label.get()));
		}).bounds(x, y, width, 20).build();
	}
}
