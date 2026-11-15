package dev.arnesurv.gui;

import dev.arnesurv.config.ModConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Ein Knopf "Name: AN/AUS", der direkt die Config umschaltet und speichert. */
public final class ToggleButton {
	private ToggleButton() {
	}

	public static Button create(String label, Supplier<Boolean> getter, Consumer<Boolean> setter, int x, int y, int width) {
		return Button.builder(text(label, getter.get()), b -> {
			boolean value = !getter.get();
			setter.accept(value);
			ModConfig.save();
			b.setMessage(text(label, value));
		}).bounds(x, y, width, 20).build();
	}

	private static Component text(String label, boolean on) {
		return Component.literal(label + ": " + (on ? "§aAN" : "§cAUS"));
	}
}
