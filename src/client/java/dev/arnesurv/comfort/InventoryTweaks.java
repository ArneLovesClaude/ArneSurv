package dev.arnesurv.comfort;

import dev.arnesurv.config.ModConfig;
import dev.arnesurv.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Inventar sortieren und Quick-Stack ("Einlagern").
 * Alles passiert über ganz normale Inventar-Klicks – dieselben, die man auch von Hand machen könnte –
 * nur automatisch und mit kleiner Verzögerung, damit Server nicht meckern.
 */
public final class InventoryTweaks {
	/** Geplante Klicks: {containerId, slotIndex, button, inputOrdinal}. */
	private static final Deque<int[]> QUEUE = new ArrayDeque<>();

	private InventoryTweaks() {
	}

	// ------------------------------------------------------------ Knöpfe

	public static void onScreenInit(Minecraft mc, Screen screen) {
		if (!ModConfig.get().inventoryButtons) return;
		if (!(screen instanceof AbstractContainerScreen<?> acs) || screen instanceof CreativeModeInventoryScreen) return;
		AbstractContainerMenu menu = acs.getMenu();
		var acc = (AbstractContainerScreenAccessor) acs;
		int bx = acc.arnesurv$getLeftPos() + acc.arnesurv$getImageWidth() + 2;
		int top = acc.arnesurv$getTopPos();

		List<Slot> playerMain = playerMainSlots(menu);
		List<Slot> container = containerSlots(menu);
		List<net.minecraft.client.gui.components.AbstractWidget> widgets = Screens.getWidgets(screen);

		if (!container.isEmpty() && container.size() >= 5) {
			int y = top + container.get(0).y - 1;
			widgets.add(small("S", "Kiste sortieren", bx, y, () -> sort(mc, menu, containerSlots(menu))));
			widgets.add(small("↓", "Einlagern: Items, die schon in der Kiste liegen, hineinlegen", bx, y + 16,
				() -> quickStack(mc, menu)));
		}
		if (!playerMain.isEmpty()) {
			int y = top + playerMain.get(0).y - 1;
			widgets.add(small("S", "Inventar sortieren (ohne Hotbar)", bx, y, () -> sort(mc, menu, playerMainSlots(menu))));
		}
	}

	private static Button small(String label, String tooltip, int x, int y, Runnable action) {
		return Button.builder(Component.literal(label), b -> action.run())
			.bounds(x, y, 14, 14)
			.tooltip(Tooltip.create(Component.literal(tooltip)))
			.build();
	}

	/** Hauptinventar des Spielers (Slots 9–35), ohne Hotbar und Rüstung. */
	private static List<Slot> playerMainSlots(AbstractContainerMenu menu) {
		List<Slot> list = new ArrayList<>();
		for (Slot s : menu.slots) {
			if (s.container instanceof Inventory && s.getContainerSlot() >= 9 && s.getContainerSlot() < 36) list.add(s);
		}
		return list;
	}

	/** Slots des geöffneten Behälters (Kiste, Fass, Shulkerkiste …). */
	private static List<Slot> containerSlots(AbstractContainerMenu menu) {
		List<Slot> list = new ArrayList<>();
		if (!(menu instanceof net.minecraft.world.inventory.ChestMenu
			|| menu instanceof net.minecraft.world.inventory.ShulkerBoxMenu
			|| menu instanceof net.minecraft.world.inventory.DispenserMenu
			|| menu instanceof net.minecraft.world.inventory.HopperMenu)) return list;
		for (Slot s : menu.slots) if (!(s.container instanceof Inventory)) list.add(s);
		return list;
	}

	// ------------------------------------------------------------ Quick-Stack

	private static void quickStack(Minecraft mc, AbstractContainerMenu menu) {
		if (!QUEUE.isEmpty() || !menu.getCarried().isEmpty()) return;
		List<Slot> container = containerSlots(menu);
		for (Slot s : playerMainSlots(menu)) {
			ItemStack stack = s.getItem();
			if (stack.isEmpty()) continue;
			boolean inContainer = container.stream().anyMatch(c -> ItemStack.isSameItemSameComponents(c.getItem(), stack));
			if (inContainer) QUEUE.add(new int[]{menu.containerId, s.index, 0, ContainerInput.QUICK_MOVE.ordinal()});
		}
	}

	// ------------------------------------------------------------ Sortieren

	/**
	 * Plant die Klicks zum Sortieren. Dafür wird das Ergebnis vorher lokal simuliert –
	 * mit denselben Regeln, die der Server bei Linksklicks anwendet.
	 */
	private static void sort(Minecraft mc, AbstractContainerMenu menu, List<Slot> slots) {
		if (!QUEUE.isEmpty() || !menu.getCarried().isEmpty() || slots.isEmpty()) return;
		Sim sim = new Sim(menu.containerId, slots);

		// 1) Gleiche Stapel zusammenführen
		for (int i = 0; i < sim.size(); i++) {
			for (int j = i + 1; j < sim.size(); j++) {
				ItemStack a = sim.get(i), b = sim.get(j);
				if (a.isEmpty() || b.isEmpty() || a.getCount() >= a.getMaxStackSize()) continue;
				if (!ItemStack.isSameItemSameComponents(a, b)) continue;
				sim.click(j);
				sim.click(i);
				if (!sim.cursor.isEmpty()) sim.click(j);
			}
		}

		// 2) Reihenfolge herstellen (Auswahl-Sortierung mit Tauschen)
		List<ItemStack> desired = new ArrayList<>();
		for (int i = 0; i < sim.size(); i++) if (!sim.get(i).isEmpty()) desired.add(sim.get(i).copy());
		desired.sort(ORDER);
		for (int p = 0; p < desired.size(); p++) {
			ItemStack want = desired.get(p);
			if (same(sim.get(p), want)) continue;
			int q = -1;
			for (int k = p + 1; k < sim.size(); k++) {
				if (same(sim.get(k), want)) {
					q = k;
					break;
				}
			}
			if (q < 0) continue;
			sim.click(q);
			sim.click(p);
			if (!sim.cursor.isEmpty()) sim.click(q);
		}
		if (!sim.cursor.isEmpty()) return; // Sicherheitsnetz: sollte nie passieren – dann lieber gar nichts tun
		QUEUE.addAll(sim.clicks);
	}

	private static final Comparator<ItemStack> ORDER = Comparator
		.<ItemStack>comparingInt(s -> BuiltInRegistries.ITEM.getId(s.getItem()))
		.thenComparing(s -> s.getHoverName().getString())
		.thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());

	private static boolean same(ItemStack a, ItemStack b) {
		return a.getCount() == b.getCount() && ItemStack.isSameItemSameComponents(a, b);
	}

	/** Lokale Simulation von Linksklicks (PICKUP) auf eine Liste von Slots. */
	private static final class Sim {
		final int containerId;
		final List<Slot> slots;
		final List<ItemStack> items = new ArrayList<>();
		ItemStack cursor = ItemStack.EMPTY;
		final List<int[]> clicks = new ArrayList<>();

		Sim(int containerId, List<Slot> slots) {
			this.containerId = containerId;
			this.slots = slots;
			for (Slot s : slots) items.add(s.getItem().copy());
		}

		int size() {
			return items.size();
		}

		ItemStack get(int i) {
			return items.get(i);
		}

		void click(int i) {
			clicks.add(new int[]{containerId, slots.get(i).index, 0, ContainerInput.PICKUP.ordinal()});
			ItemStack slot = items.get(i);
			if (cursor.isEmpty()) {
				cursor = slot;
				items.set(i, ItemStack.EMPTY);
			} else if (slot.isEmpty()) {
				items.set(i, cursor);
				cursor = ItemStack.EMPTY;
			} else if (ItemStack.isSameItemSameComponents(slot, cursor)) {
				int room = slot.getMaxStackSize() - slot.getCount();
				int moved = Math.min(room, cursor.getCount());
				if (moved > 0) {
					ItemStack ns = slot.copy();
					ns.grow(moved);
					items.set(i, ns);
					cursor = cursor.getCount() - moved <= 0 ? ItemStack.EMPTY : cursor.copyWithCount(cursor.getCount() - moved);
				}
			} else {
				items.set(i, cursor);
				cursor = slot;
			}
		}
	}

	/** Nur für den Entwickler-Autotest: Einlagern im offenen Menü auslösen. */
	public static void devQuickStack(Minecraft mc) {
		if (mc.gui.screen() instanceof AbstractContainerScreen<?> acs) quickStack(mc, acs.getMenu());
	}

	/** Nur für den Entwickler-Autotest: Spielerinventar im offenen Menü sortieren. */
	public static void devSortPlayer(Minecraft mc) {
		if (mc.gui.screen() instanceof AbstractContainerScreen<?> acs) sort(mc, acs.getMenu(), playerMainSlots(acs.getMenu()));
	}

	// ------------------------------------------------------------ Ausführen

	public static void tick(Minecraft mc) {
		if (QUEUE.isEmpty()) return;
		if (mc.player == null || mc.gameMode == null || !(mc.gui.screen() instanceof AbstractContainerScreen<?> acs)) {
			QUEUE.clear();
			return;
		}
		int perTick = Math.max(1, ModConfig.get().inventoryClicksPerTick);
		for (int n = 0; n < perTick && !QUEUE.isEmpty(); n++) {
			int[] c = QUEUE.poll();
			if (acs.getMenu().containerId != c[0]) {
				QUEUE.clear();
				return;
			}
			mc.gameMode.handleContainerInput(c[0], c[1], c[2], ContainerInput.values()[c[3]], mc.player);
		}
	}
}
