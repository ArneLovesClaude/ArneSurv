package dev.arnesurv.gui;

import dev.arnesurv.waypoint.Waypoint;
import dev.arnesurv.waypoint.WaypointColors;
import dev.arnesurv.waypoint.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Waypoint anlegen oder bearbeiten: Name, Koordinaten, Farbe. */
public class WaypointEditScreen extends Screen {
	private final Screen parent;
	private final Waypoint target; // null = neuer Waypoint
	private final Waypoint draft;

	private EditBox nameBox, xBox, yBox, zBox;
	private Button saveButton;

	public WaypointEditScreen(Screen parent, Waypoint existing) {
		super(Component.literal(existing == null ? "Neuer Waypoint" : "Waypoint bearbeiten"));
		this.parent = parent;
		this.target = existing;
		this.draft = existing == null ? new Waypoint() : copy(existing);
	}

	/** Neuer Waypoint an der aktuellen Position des Spielers. */
	public static WaypointEditScreen forCurrentPosition(Screen parent) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return forPosition(parent, 0, 64, 0);
		return forPosition(parent, mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
	}

	/** Neuer Waypoint an einer beliebigen Stelle (z.B. Rechtsklick auf der Weltkarte). */
	public static WaypointEditScreen forPosition(Screen parent, int x, int y, int z) {
		Minecraft mc = Minecraft.getInstance();
		WaypointEditScreen s = new WaypointEditScreen(parent, null);
		s.draft.x = x;
		s.draft.y = y;
		s.draft.z = z;
		if (mc.level != null) {
			s.draft.dimension = mc.level.dimension().identifier().toString();
		}
		s.draft.name = "Waypoint " + (WaypointManager.all().size() + 1);
		s.draft.color = WaypointColors.COLORS[WaypointManager.all().size() % WaypointColors.COLORS.length];
		return s;
	}

	private static Waypoint copy(Waypoint w) {
		Waypoint c = new Waypoint(w.name, w.x, w.y, w.z, w.dimension, w.color);
		c.visible = w.visible;
		c.death = w.death;
		c.created = w.created;
		return c;
	}

	@Override
	protected void init() {
		int w = 200, x = width / 2 - w / 2, y = height / 2 - 60;

		nameBox = new EditBox(font, x, y, w, 20, Component.literal("Name"));
		nameBox.setMaxLength(48);
		nameBox.setValue(draft.name == null ? "" : draft.name);
		nameBox.setResponder(v -> validate());
		addRenderableWidget(nameBox);

		int cw = (w - 8) / 3;
		xBox = coordBox(x, y + 36, cw, draft.x);
		yBox = coordBox(x + cw + 4, y + 36, cw, draft.y);
		zBox = coordBox(x + 2 * (cw + 4), y + 36, cw, draft.z);

		addRenderableWidget(Button.builder(colorLabel(), b -> {
			draft.color = WaypointColors.next(draft.color);
			b.setMessage(colorLabel());
		}).bounds(x, y + 64, w, 20).build());

		saveButton = addRenderableWidget(Button.builder(Component.literal("§aSpeichern"), b -> save())
			.bounds(x, y + 100, w / 2 - 2, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Abbrechen"), b -> onClose())
			.bounds(x + w / 2 + 2, y + 100, w / 2 - 2, 20).build());

		setInitialFocus(nameBox);
		validate();
	}

	private EditBox coordBox(int x, int y, int w, int value) {
		EditBox box = new EditBox(font, x, y, w, 20, Component.literal("Koordinate"));
		box.setMaxLength(9);
		box.setValue(Integer.toString(value));
		box.setResponder(v -> validate());
		return addRenderableWidget(box);
	}

	private Component colorLabel() {
		return Component.literal("Farbe: ").append(Component.literal("■ " + WaypointColors.name(draft.color))
			.withColor(draft.color & 0xFFFFFF));
	}

	private void validate() {
		if (saveButton == null) return;
		saveButton.active = !nameBox.getValue().isBlank()
			&& parse(xBox) != null && parse(yBox) != null && parse(zBox) != null;
	}

	private static Integer parse(EditBox box) {
		if (box == null) return null;
		try {
			return Integer.parseInt(box.getValue().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void save() {
		draft.name = nameBox.getValue().trim();
		draft.x = parse(xBox);
		draft.y = parse(yBox);
		draft.z = parse(zBox);
		if (target == null) {
			WaypointManager.add(draft);
		} else {
			target.name = draft.name;
			target.x = draft.x;
			target.y = draft.y;
			target.z = draft.z;
			target.color = draft.color;
			WaypointManager.save();
		}
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		int x = width / 2 - 100, y = height / 2 - 60;
		g.centeredText(font, "§l" + title.getString(), width / 2, y - 28, 0xFFFFFFFF);
		g.text(font, "§7Name", x, y - 11, 0xFFFFFFFF, false);
		g.text(font, "§7X              Y              Z", x, y + 25, 0xFFFFFFFF, false);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
