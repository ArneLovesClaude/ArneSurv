package dev.arnesurv.gui;

import dev.arnesurv.waypoint.Waypoint;
import dev.arnesurv.waypoint.WaypointManager;
import dev.arnesurv.waypoint.WaypointShare;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

/** Liste der Waypoints in der aktuellen Dimension, sortiert nach Entfernung. */
public class WaypointListScreen extends Screen {
	private static final int ROW_HEIGHT = 24;
	private final Screen parent;
	private int page = 0;
	private List<Waypoint> shown = List.of();

	public WaypointListScreen(Screen parent) {
		super(Component.literal("Waypoints"));
		this.parent = parent;
	}

	private int rowsPerPage() {
		return Math.max(1, (height - 100) / ROW_HEIGHT);
	}

	@Override
	protected void init() {
		if (minecraft.player == null || minecraft.level == null) {
			onClose();
			return;
		}
		var p = minecraft.player;
		shown = WaypointManager.inDimension(minecraft.level.dimension().identifier().toString()).stream()
			.sorted(Comparator.comparingDouble(w -> w.distanceTo(p.getX(), p.getY(), p.getZ())))
			.toList();

		int perPage = rowsPerPage();
		int pages = Math.max(1, (shown.size() + perPage - 1) / perPage);
		page = Math.min(page, pages - 1);

		int listW = Math.min(360, width - 20);
		int left = width / 2 - listW / 2;
		int y = 40;
		for (int i = page * perPage; i < Math.min(shown.size(), (page + 1) * perPage); i++) {
			Waypoint w = shown.get(i);
			int bx = left + listW;
			addRenderableWidget(Button.builder(Component.literal("§cX"), b -> confirmDelete(w))
				.bounds(bx - 20, y, 20, 20).build());
			addRenderableWidget(Button.builder(Component.literal("Bearbeiten"),
				b -> minecraft.gui.setScreen(new WaypointEditScreen(this, w))).bounds(bx - 92, y, 70, 20).build());
			addRenderableWidget(Button.builder(Component.literal(w.visible ? "§aSichtbar" : "§7Versteckt"), b -> {
				w.visible = !w.visible;
				WaypointManager.save();
				rebuildWidgets();
			}).bounds(bx - 154, y, 60, 20).build());
			if (minecraft.getSingleplayerServer() == null) {
				addRenderableWidget(Button.builder(Component.literal("Teilen"), b -> {
					WaypointShare.share(w);
					minecraft.gui.setScreen(null);
				}).bounds(bx - 202, y, 46, 20)
					.tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
						"Schickt den Waypoint in den Chat. Wer ArneSurv hat, kann ihn mit einem Klick übernehmen.")))
					.build());
			}
			y += ROW_HEIGHT;
		}

		int by = height - 28;
		addRenderableWidget(Button.builder(Component.literal("<"), b -> {
			page--;
			rebuildWidgets();
		}).bounds(width / 2 - 154, by, 20, 20).build()).active = page > 0;
		addRenderableWidget(Button.builder(Component.literal("+ Neuer Waypoint"),
			b -> minecraft.gui.setScreen(WaypointEditScreen.forCurrentPosition(this))).bounds(width / 2 - 130, by, 128, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
			.bounds(width / 2 + 2, by, 128, 20).build());
		addRenderableWidget(Button.builder(Component.literal(">"), b -> {
			page++;
			rebuildWidgets();
		}).bounds(width / 2 + 134, by, 20, 20).build()).active = page < pages - 1;
	}

	private void confirmDelete(Waypoint w) {
		minecraft.gui.setScreen(new ConfirmScreen(yes -> {
			if (yes) WaypointManager.remove(w);
			minecraft.gui.setScreen(this);
		}, Component.literal("Waypoint löschen?"), Component.literal("\"" + w.name + "\" wird endgültig gelöscht.")));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, "§lWaypoints §7(diese Dimension)", width / 2, 14, 0xFFFFFFFF);
		if (shown.isEmpty()) {
			g.centeredText(font, "§7Noch keine Waypoints. Klick auf \"+ Neuer Waypoint\"", width / 2, height / 2 - 10, 0xFFFFFFFF);
			return;
		}
		var p = minecraft.player;
		if (p == null) return;
		int perPage = rowsPerPage();
		int listW = Math.min(360, width - 20);
		int left = width / 2 - listW / 2;
		int y = 40;
		for (int i = page * perPage; i < Math.min(shown.size(), (page + 1) * perPage); i++) {
			Waypoint w = shown.get(i);
			g.fill(left, y + 4, left + 10, y + 14, w.color);
			int dist = (int) Math.round(w.distanceTo(p.getX(), p.getY(), p.getZ()));
			g.text(font, (w.death ? "§c☠ §f" : "§f") + w.name, left + 14, y + 1, 0xFFFFFFFF, true);
			g.text(font, "§7" + w.x + " " + w.y + " " + w.z + "  ·  " + dist + "m", left + 14, y + 11, 0xFFFFFFFF, true);
			y += ROW_HEIGHT;
		}
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
