package dev.arnesurv.comfort;

import com.mojang.blaze3d.platform.NativeImage;
import dev.arnesurv.ArneSurv;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/** Screenshot-Galerie: Vorschaubilder, Großansicht, öffnen, Ordner öffnen, löschen. */
public class ScreenshotScreen extends Screen {
	private static final int THUMB_W = 128;

	private final Screen parent;
	private final List<Path> files = new ArrayList<>();
	private final Map<Path, Thumb> thumbs = new HashMap<>();
	private int page;
	private Path selected;

	private record Thumb(DynamicTexture texture, int w, int h) {
	}

	public ScreenshotScreen(Screen parent) {
		super(Component.literal("Screenshots"));
		this.parent = parent;
	}

	private Path dir() {
		return minecraft.gameDirectory.toPath().resolve("screenshots");
	}

	private int cols() {
		return Math.max(1, (width - 20) / (THUMB_W + 8));
	}

	private int rows() {
		return Math.max(1, (height - 80) / (THUMB_W * 9 / 16 + 18));
	}

	private int perPage() {
		return cols() * rows();
	}

	@Override
	protected void init() {
		files.clear();
		if (Files.isDirectory(dir())) {
			try (Stream<Path> s = Files.list(dir())) {
				s.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
					.sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed())
					.forEach(files::add);
			} catch (IOException e) {
				ArneSurv.LOGGER.warn("Screenshot-Ordner nicht lesbar", e);
			}
		}
		int pages = Math.max(1, (files.size() + perPage() - 1) / perPage());
		page = Math.min(page, pages - 1);
		int by = height - 28;

		if (selected != null) {
			addRenderableWidget(Button.builder(Component.literal("Öffnen"), b -> open(selected))
				.bounds(width / 2 - 206, by, 100, 20).build());
			addRenderableWidget(Button.builder(Component.literal("§cLöschen"), b -> confirmDelete(selected))
				.bounds(width / 2 - 102, by, 100, 20).build());
			addRenderableWidget(Button.builder(Component.literal("Zurück"), b -> {
				selected = null;
				rebuildWidgets();
			}).bounds(width / 2 + 2, by, 204, 20).build());
			return;
		}
		addRenderableWidget(Button.builder(Component.literal("<"), b -> {
			page--;
			rebuildWidgets();
		}).bounds(width / 2 - 160, by, 20, 20).build()).active = page > 0;
		addRenderableWidget(Button.builder(Component.literal("Ordner öffnen"), b -> open(dir()))
			.bounds(width / 2 - 136, by, 130, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> onClose())
			.bounds(width / 2 + 6, by, 130, 20).build());
		addRenderableWidget(Button.builder(Component.literal(">"), b -> {
			page++;
			rebuildWidgets();
		}).bounds(width / 2 + 140, by, 20, 20).build()).active = page < pages - 1;
	}

	private Thumb thumb(Path file) {
		return thumbs.computeIfAbsent(file, f -> {
			try (var in = Files.newInputStream(f); NativeImage full = NativeImage.read(in)) {
				int w = THUMB_W * 2, h = Math.max(1, full.getHeight() * w / full.getWidth());
				NativeImage small = new NativeImage(w, h, false);
				full.resizeSubRectTo(0, 0, full.getWidth(), full.getHeight(), small);
				return new Thumb(new DynamicTexture(() -> "arnesurv_shot", small), w, h);
			} catch (Exception e) {
				return null;
			}
		});
	}

	/** Für die Großansicht in voller Auflösung laden (ersetzt kurzzeitig die Vorschau). */
	private final Map<Path, Thumb> fullImages = new HashMap<>();

	private Thumb full(Path file) {
		return fullImages.computeIfAbsent(file, f -> {
			try (var in = Files.newInputStream(f)) {
				NativeImage img = NativeImage.read(in);
				return new Thumb(new DynamicTexture(() -> "arnesurv_shot_full", img), img.getWidth(), img.getHeight());
			} catch (Exception e) {
				return null;
			}
		});
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		if (selected != null) {
			Thumb t = full(selected);
			g.centeredText(font, "§l" + selected.getFileName(), width / 2, 10, 0xFFFFFFFF);
			if (t != null) {
				int maxW = width - 40, maxH = height - 70;
				float s = Math.min(maxW / (float) t.w(), maxH / (float) t.h());
				int w = Math.round(t.w() * s), h = Math.round(t.h() * s);
				int x = width / 2 - w / 2, y = 26;
				g.blit(t.texture().getTextureView(), t.texture().getSampler(), x, y, x + w, y + h, 0, 1, 0, 1);
			}
			return;
		}
		g.centeredText(font, "§lScreenshots §7(" + files.size() + ")", width / 2, 10, 0xFFFFFFFF);
		if (files.isEmpty()) {
			g.centeredText(font, "§7Noch keine Screenshots. Drück F2 im Spiel.", width / 2, height / 2 - 10, 0xFFFFFFFF);
			return;
		}
		int i = 0;
		for (Path f : pageFiles()) {
			int[] r = cell(i++);
			g.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1,
				inside(r, mouseX, mouseY) ? 0xFFFFFF55 : 0xFF000000);
			Thumb t = thumb(f);
			if (t != null) {
				g.blit(t.texture().getTextureView(), t.texture().getSampler(), r[0], r[1], r[0] + r[2], r[1] + r[3], 0, 1, 0, 1);
			}
			String name = f.getFileName().toString().replace(".png", "");
			if (font.width(name) > r[2]) name = font.plainSubstrByWidth(name, r[2] - 6) + "…";
			g.text(font, "§7" + name, r[0], r[1] + r[3] + 3, 0xFFFFFFFF, false);
		}
	}

	private List<Path> pageFiles() {
		int from = page * perPage();
		return files.subList(Math.min(from, files.size()), Math.min(files.size(), from + perPage()));
	}

	private int[] cell(int i) {
		int cols = cols(), h = THUMB_W * 9 / 16;
		int total = cols * (THUMB_W + 8) - 8;
		int x0 = width / 2 - total / 2;
		return new int[]{x0 + (i % cols) * (THUMB_W + 8), 28 + (i / cols) * (h + 18), THUMB_W, h};
	}

	private static boolean inside(int[] r, double x, double y) {
		return x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3];
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		if (selected == null && event.button() == 0) {
			int i = 0;
			for (Path f : pageFiles()) {
				if (inside(cell(i++), event.x(), event.y())) {
					selected = f;
					rebuildWidgets();
					return true;
				}
			}
		}
		return false;
	}

	private void confirmDelete(Path file) {
		minecraft.gui.setScreen(new ConfirmScreen(yes -> {
			if (yes) {
				try {
					Files.deleteIfExists(file);
				} catch (IOException e) {
					ArneSurv.LOGGER.warn("Screenshot konnte nicht gelöscht werden", e);
				}
				release(file);
				selected = null;
			}
			minecraft.gui.setScreen(this);
		}, Component.literal("Screenshot löschen?"), Component.literal("\"" + file.getFileName() + "\" wird endgültig gelöscht.")));
	}

	private void release(Path file) {
		Thumb t = thumbs.remove(file);
		if (t != null) t.texture().close();
		Thumb f = fullImages.remove(file);
		if (f != null) f.texture().close();
	}

	/** Datei oder Ordner mit dem Standardprogramm des Systems öffnen. */
	static void open(Path path) {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		try {
			if (os.contains("win")) new ProcessBuilder("explorer.exe", path.toAbsolutePath().toString()).start();
			else if (os.contains("mac")) new ProcessBuilder("open", path.toAbsolutePath().toString()).start();
			else new ProcessBuilder("xdg-open", path.toAbsolutePath().toString()).start();
		} catch (IOException e) {
			ArneSurv.LOGGER.warn("Konnte {} nicht öffnen", path, e);
		}
	}

	@Override
	public void removed() {
		thumbs.values().forEach(t -> {
			if (t != null) t.texture().close();
		});
		fullImages.values().forEach(t -> {
			if (t != null) t.texture().close();
		});
		thumbs.clear();
		fullImages.clear();
	}

	@Override
	public void onClose() {
		if (selected != null) {
			selected = null;
			rebuildWidgets();
			return;
		}
		minecraft.gui.setScreen(parent);
	}
}
