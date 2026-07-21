package dev.hiddencoords.ui;

import dev.hiddencoords.CoordOverlayMod;
import dev.hiddencoords.CoordinateSnapshot;
import dev.hiddencoords.OverlayConfig;

import net.minecraft.text.Text;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class CoordWindow {
    private static String TITLE;
    private static String COORDS_FMT;
    private static String COORDS_PH;
    private static String DIM_FMT;
    private static String DIM_PH;
    private static String BIO_FMT;
    private static String BIO_PH;

    private final OverlayConfig config = OverlayConfig.load();
    private JFrame frame;
    private JLabel coordinates;
    private JLabel dimension;
    private JLabel biome;
    private volatile boolean available = true;
    private volatile boolean wantedVisible = true;

    public void toggle() {
        wantedVisible = !wantedVisible;
        if (wantedVisible) show(); else hideFrame();
    }

    public void show() {
        if (!available) return;
        SwingUtilities.invokeLater(() -> {
            if (!createIfNeeded()) return;
            if (wantedVisible) frame.setVisible(true);
        });
    }

    public void hideForNoWorld() {
        SwingUtilities.invokeLater(() -> {
            if (frame != null) frame.setVisible(false);
        });
    }

    public void update(CoordinateSnapshot snapshot) {
        if (!available || !wantedVisible) return;
        SwingUtilities.invokeLater(() -> {
            if (!createIfNeeded()) return;
            loadTranslations();
            coordinates.setText(COORDS_FMT.formatted(snapshot.x(), snapshot.y(), snapshot.z()));
            dimension.setText(DIM_FMT.formatted(snapshot.dimension()));
            biome.setText(BIO_FMT.formatted(snapshot.biome()));
            if (wantedVisible) frame.setVisible(true);
        });
    }

    public void shutdown() {
        SwingUtilities.invokeLater(() -> {
            if (frame != null) {
                persistBounds();
                frame.dispose();
                frame = null;
            }
        });
    }

    private void hideFrame() {
        SwingUtilities.invokeLater(() -> {
            if (frame != null) frame.setVisible(false);
        });
    }

    private boolean createIfNeeded() {
        if (frame != null) return true;
        if (GraphicsEnvironment.isHeadless()) {
            disable("Java is running headless; coordinate window is unavailable", null);
            return false;
        }
        try {
            loadTranslations();
            frame = new JFrame(TITLE);
            frame.setAutoRequestFocus(false);
            frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            frame.setAlwaysOnTop(config.alwaysOnTop);
            frame.setMinimumSize(new Dimension(260, 120));
            frame.setSize(config.width, config.height);
            if (config.x != Integer.MIN_VALUE && config.y != Integer.MIN_VALUE) frame.setLocation(config.x, config.y);
            else frame.setLocationByPlatform(true);

            JPanel content = new JPanel(new BorderLayout());
            content.setBackground(new Color(24, 24, 28));
            content.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            JPanel rows = new JPanel(new GridLayout(3, 1, 0, 7));
            rows.setOpaque(false);
            coordinates = label(COORDS_PH, Font.BOLD);
            dimension = label(DIM_PH, Font.PLAIN);
            biome = label(BIO_PH, Font.PLAIN);
            rows.add(coordinates); rows.add(dimension); rows.add(biome);
            content.add(rows, BorderLayout.CENTER);
            frame.setContentPane(content);
            frame.addComponentListener(new ComponentAdapter() {
                @Override public void componentMoved(ComponentEvent event) { persistBounds(); }
                @Override public void componentResized(ComponentEvent event) { persistBounds(); }
            });
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) {

                    wantedVisible = false;
                    persistBounds();
                }
            });
            return true;
        } catch (Throwable exception) {

            disable("Could not create coordinate window; check DISPLAY/Wayland environment", exception);
            return false;
        }
    }

    private static void loadTranslations() {
        if (TITLE != null) return;
        TITLE = Text.translatable("hidden_coords.window.title").getString();
        COORDS_FMT = Text.translatable("hidden_coords.coordinates.format").getString();
        COORDS_PH = Text.translatable("hidden_coords.coordinates.placeholder").getString();
        DIM_FMT = Text.translatable("hidden_coords.dimension.format").getString();
        DIM_PH = Text.translatable("hidden_coords.dimension.placeholder").getString();
        BIO_FMT = Text.translatable("hidden_coords.biome.format").getString();
        BIO_PH = Text.translatable("hidden_coords.biome.placeholder").getString();
    }

    private JLabel label(String text, int style) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setForeground(new Color(238, 238, 242));
        label.setFont(new Font(Font.SANS_SERIF, style, config.fontSize));
        return label;
    }

    private void persistBounds() {
        if (frame == null) return;
        config.x = frame.getX(); config.y = frame.getY();
        config.width = frame.getWidth(); config.height = frame.getHeight();
        config.save();
    }

    private void disable(String message, Throwable exception) {
        available = false;
        if (exception == null) CoordOverlayMod.LOGGER.warn(message);
        else CoordOverlayMod.LOGGER.warn(message, exception);
    }
}
