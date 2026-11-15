package dev.arnesurv.comfort;

import dev.arnesurv.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Wählt beim Abbauen automatisch das beste Werkzeug aus der Hotbar
 * und wechselt danach (optional) zurück zum vorherigen Slot.
 * Werkzeuge kurz vor dem Kaputtgehen werden geschont.
 */
public final class AutoTool {
	private static final int MIN_DURABILITY = 10;
	private static int previousSlot = -1;

	private AutoTool() {
	}

	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || !ModConfig.get().autoToolEnabled || player.isCreative() || player.isSpectator()) {
			previousSlot = -1;
			return;
		}
		Inventory inv = player.getInventory();
		boolean mining = mc.gui.screen() == null && mc.options.keyAttack.isDown()
			&& mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;

		if (!mining) {
			if (previousSlot >= 0 && ModConfig.get().autoToolSwitchBack) inv.setSelectedSlot(previousSlot);
			previousSlot = -1;
			return;
		}

		BlockState state = mc.level.getBlockState(((BlockHitResult) mc.hitResult).getBlockPos());
		if (state.isAir()) return;
		int best = bestSlot(inv, state);
		if (best >= 0 && best != inv.getSelectedSlot()) {
			if (previousSlot < 0) previousSlot = inv.getSelectedSlot();
			inv.setSelectedSlot(best);
		}
	}

	private static int bestSlot(Inventory inv, BlockState state) {
		int current = inv.getSelectedSlot();
		float bestScore = score(inv.getItem(current), state);
		int best = current;
		for (int i = 0; i < 9; i++) {
			float s = score(inv.getItem(i), state);
			if (s > bestScore + 0.01f) {
				bestScore = s;
				best = i;
			}
		}
		return best;
	}

	/** Bewertung: richtiges Werkzeug (für Drops) zählt am meisten, dann Abbaugeschwindigkeit. */
	private static float score(ItemStack stack, BlockState state) {
		if (stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= MIN_DURABILITY) return -1f;
		float speed = stack.isEmpty() ? 1f : stack.getDestroySpeed(state);
		boolean needsTool = state.requiresCorrectToolForDrops();
		boolean correct = !needsTool || stack.isCorrectToolForDrops(state);
		return (correct ? 1000f : 0f) + speed;
	}
}
