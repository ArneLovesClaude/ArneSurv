package dev.arnesurv.stats;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Statistik-Seite: Vanilla-Stats nach Zeitraum und Kategorie, mit Löschen der Aufzeichnung. */
public class StatsScreen extends Screen {
	private static final int ROW = 12;

	enum Range {
		SESSION("Diese Session"), TODAY("Heute"), WEEK("Letzte 7 Tage"), MONTH("Letzte 30 Tage"),
		RECORDED("Alles Aufgezeichnete"), SERVER("Gesamt laut Server");
		final String label;

		Range(String label) {
			this.label = label;
		}

		long since() {
			long now = System.currentTimeMillis();
			return switch (this) {
				case SESSION -> StatsRecorder.sessionStart();
				case TODAY -> LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
				case WEEK -> now - 7L * 24 * 3600 * 1000;
				case MONTH -> now - 30L * 24 * 3600 * 1000;
				default -> 0L;
			};
		}
	}

	enum Category {
		GENERAL("minecraft.custom:", "Allgemein"), MINED("minecraft.mined:", "Abgebaut"),
		KILLED("minecraft.killed:", "Getötet"), KILLED_BY("minecraft.killed_by:", "Getötet von"),
		USED("minecraft.used:", "Benutzt"), CRAFTED("minecraft.crafted:", "Hergestellt"),
		PICKED_UP("minecraft.picked_up:", "Aufgehoben"), DROPPED("minecraft.dropped:", "Fallengelassen"),
		BROKEN("minecraft.broken:", "Verbraucht");
		final String prefix, label;

		Category(String prefix, String label) {
			this.prefix = prefix;
			this.label = label;
		}
	}

	private record Row(Component label, String value, int raw) {
	}

	private static Range range = Range.SESSION;
	private static Category category = Category.GENERAL;

	private final Screen parent;
	private List<Row> rows = List.of();
	private int page;

	public StatsScreen(Screen parent) {
		super(Component.literal("Statistiken"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		refreshRows();
		int w = 150;
		addRenderableWidget(Button.builder(Component.literal("Zeitraum: " + range.label), b -> {
			range = Range.values()[(range.ordinal() + 1) % Range.values().length];
			page = 0;
			rebuildWidgets();
		}).bounds(width / 2 - w - 4, 28, w, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Kategorie: " + category.label), b -> {
			category = Category.values()[(category.ordinal() + 1) % Category.values().length];
			page = 0;
			rebuildWidgets();
		}).bounds(width / 2 + 4, 28, w, 20).build());

		int pages = pages();
		int by = height - 28;
		addRenderableWidget(Button.builder(Component.literal("<"), b -> {
			page--;
			rebuildWidgets();
		}).bounds(width / 2 - 204, by, 20, 20).build()).active = page > 0;
		addRenderableWidget(Button.builder(Component.literal("Aktualisieren"), b -> {
			if (minecraft.getConnection() != null) {
				minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
			}
			rebuildWidgets();
		}).bounds(width / 2 - 180, by, 86, 20).build());
		addRenderableWidget(Button.builder(Component.literal("§cAufzeichnung löschen…"), b ->
			minecraft.gui.setScreen(new DeleteScreen(this))).bounds(width / 2 - 90, by, 128, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
			.bounds(width / 2 + 42, by, 138, 20).build());
		addRenderableWidget(Button.builder(Component.literal(">"), b -> {
			page++;
			rebuildWidgets();
		}).bounds(width / 2 + 184, by, 20, 20).build()).active = page < pages - 1;
	}

	private int rowsPerPage() {
		return Math.max(1, (height - 100) / ROW);
	}

	private int pages() {
		return Math.max(1, (rows.size() + rowsPerPage() - 1) / rowsPerPage());
	}

	private void refreshRows() {
		Map<String, Integer> values = range == Range.SERVER
			? new HashMap<>(StatsRecorder.data().lastKnown)
			: StatsRecorder.sumSince(range.since());
		List<Row> list = new ArrayList<>();
		for (Map.Entry<String, Integer> e : values.entrySet()) {
			if (!e.getKey().startsWith(category.prefix) || e.getValue() == null || e.getValue() <= 0) continue;
			var crit = ObjectiveCriteria.byName(e.getKey());
			if (crit.isEmpty() || !(crit.get() instanceof Stat<?> stat)) continue;
			list.add(new Row(label(stat), stat.format(e.getValue()), e.getValue()));
		}
		list.sort(Comparator.comparingInt(Row::raw).reversed());
		rows = list;
		page = Math.min(page, pages() - 1);
	}

	private static Component label(Stat<?> stat) {
		Object v = stat.getValue();
		if (v instanceof Identifier id) return Component.translatable("stat." + id.toString().replace(':', '.'));
		if (v instanceof Block b) return b.getName();
		if (v instanceof Item i) return new ItemStack(i).getHoverName();
		if (v instanceof EntityType<?> t) return t.getDescription();
		return Component.literal(stat.getName());
	}

	@Override
	public void tick() {
		// Neue Daten vom Server sofort sichtbar machen
		if (minecraft.level != null && minecraft.level.getGameTime() % 20 == 0) refreshRows();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, "§lStatistiken", width / 2, 12, 0xFFFFFFFF);
		int listW = Math.min(400, width - 20), left = width / 2 - listW / 2;
		int y = 56;
		if (rows.isEmpty()) {
			String hint = range == Range.SESSION
				? "§7Noch nichts aufgezeichnet. Spiel ein bisschen – neue Werte kommen alle 30 Sekunden."
				: "§7Für diesen Zeitraum und diese Kategorie gibt es keine Werte.";
			int hw = Math.min(300, width - 40);
			g.textWithWordWrap(font, Component.literal(hint), width / 2 - hw / 2, height / 2 - 14, hw, 0xFFFFFFFF);
			return;
		}
		int per = rowsPerPage();
		for (int i = page * per; i < Math.min(rows.size(), (page + 1) * per); i++) {
			Row r = rows.get(i);
			if ((i & 1) == 0) g.fill(left - 2, y - 2, left + listW + 2, y + ROW - 2, 0x30FFFFFF);
			g.text(font, r.label(), left, y, 0xFFFFFFFF, false);
			g.text(font, r.value(), left + listW - font.width(r.value()), y, 0xFFFFFF55, false);
			y += ROW;
		}
		g.centeredText(font, "§7Seite " + (page + 1) + " / " + pages(), width / 2, height - 40, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	/** Auswahl, welcher Zeitraum der Aufzeichnung gelöscht werden soll – danach Bestätigung. */
	static class DeleteScreen extends Screen {
		private final Screen parent;

		DeleteScreen(Screen parent) {
			super(Component.literal("Aufzeichnung löschen"));
			this.parent = parent;
		}

		@Override
		protected void init() {
			long now = System.currentTimeMillis();
			int x = width / 2 - 100, y = height / 2 - 50;
			option("Letzte Stunde", now - 3600_000L, x, y);
			option("Heute", Range.TODAY.since(), x, y + 24);
			option("Letzte 7 Tage", Range.WEEK.since(), x, y + 48);
			option("§cAlles", 0L, x, y + 72);
			addRenderableWidget(Button.builder(Component.literal("Abbrechen"), b -> onClose())
				.bounds(x, y + 108, 200, 20).build());
		}

		private void option(String label, long since, int x, int y) {
			addRenderableWidget(Button.builder(Component.literal(label), b ->
				minecraft.gui.setScreen(new ConfirmScreen(yes -> {
					if (yes) StatsRecorder.deleteSince(since);
					minecraft.gui.setScreen(parent);
				}, Component.literal("Aufzeichnung löschen: " + label.replace("§c", "") + "?"),
					Component.literal("Das löscht nur die Aufzeichnung von ArneSurv. Die echten Statistiken auf dem Server bleiben unverändert. Das kann nicht rückgängig gemacht werden."))))
				.bounds(x, y, 200, 20).build());
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			super.extractRenderState(g, mouseX, mouseY, delta);
			g.centeredText(font, "§lWelchen Zeitraum löschen?", width / 2, height / 2 - 72, 0xFFFFFFFF);
		}

		@Override
		public void onClose() {
			minecraft.gui.setScreen(parent);
		}
	}
}
