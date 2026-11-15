package dev.arnesurv.comfort;

import com.mojang.blaze3d.platform.InputConstants;
import dev.arnesurv.config.ModConfig;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;

/**
 * "Im Inventar weiter sneaken": Wer beim Öffnen eines Menüs geduckt war,
 * bleibt geduckt, bis das Menü wieder zu ist (z.B. um nicht von der Kante zu fallen).
 */
public final class SneakKeeper {
	private static boolean screenWasOpen;
	private static boolean keepSneaking;
	private static boolean sneakingBefore;

	private SneakKeeper() {
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null) {
			screenWasOpen = false;
			keepSneaking = false;
			return;
		}
		boolean screenOpen = mc.gui.screen() != null;
		if (screenOpen && !screenWasOpen) {
			keepSneaking = ModConfig.get().sneakInInventory && sneakingBefore;
		}
		if (screenOpen && keepSneaking) {
			mc.options.keyShift.setDown(true);
		}
		if (!screenOpen && screenWasOpen && keepSneaking) {
			keepSneaking = false;
			// Bei "Halten" den echten Tastenzustand übernehmen, bei "Umschalten" macht Minecraft das selbst
			if (!mc.options.toggleCrouch().get()) {
				var key = KeyMappingHelper.getBoundKeyOf(mc.options.keyShift);
				boolean physical = key.getType() == InputConstants.Type.KEYBOARD
					&& InputConstants.isKeyDown(key.getValue());
				mc.options.keyShift.setDown(physical);
			}
		}
		if (!screenOpen) sneakingBefore = mc.player.isShiftKeyDown();
		screenWasOpen = screenOpen;
	}
}
