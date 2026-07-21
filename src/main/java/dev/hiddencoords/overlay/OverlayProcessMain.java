package dev.hiddencoords.overlay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public final class OverlayProcessMain {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        int fontSize = Integer.parseInt(args[1]);
        boolean alwaysOnTop = "1".equals(args[2]);
        int wx = Integer.parseInt(args[3]);
        int wy = Integer.parseInt(args[4]);
        int ww = Integer.parseInt(args[5]);
        int wh = Integer.parseInt(args[6]);

        Socket socket = new Socket("127.0.0.1", port);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        JFrame frame = new JFrame("Hidden Coords");
        frame.setAutoRequestFocus(false);
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        frame.setAlwaysOnTop(alwaysOnTop);
        frame.setMinimumSize(new Dimension(260, 120));
        if (wx != Integer.MIN_VALUE && wy != Integer.MIN_VALUE) {
            frame.setLocation(wx, wy);
        } else {
            frame.setLocationByPlatform(true);
        }
        frame.setSize(ww, wh);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(new Color(24, 24, 28));
        content.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JPanel rows = new JPanel(new GridLayout(3, 1, 0, 7));
        rows.setOpaque(false);

        JLabel coordsLabel = new JLabel("X: 0  Y: 0  Z: 0", SwingConstants.LEFT);
        coordsLabel.setForeground(new Color(238, 238, 242));
        coordsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));

        JLabel dimLabel = new JLabel("Overworld", SwingConstants.LEFT);
        dimLabel.setForeground(new Color(238, 238, 242));
        dimLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));

        JLabel biomeLabel = new JLabel("Plains", SwingConstants.LEFT);
        biomeLabel.setForeground(new Color(238, 238, 242));
        biomeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));

        rows.add(coordsLabel);
        rows.add(dimLabel);
        rows.add(biomeLabel);
        content.add(rows, BorderLayout.CENTER);
        frame.setContentPane(content);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                writer.println("HIDDEN");
                writer.println("GEOM;" + frame.getX() + ";" + frame.getY() + ";"
                    + frame.getWidth() + ";" + frame.getHeight());
            }
        });

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent event) {
                writer.println("GEOM;" + frame.getX() + ";" + frame.getY() + ";"
                    + frame.getWidth() + ";" + frame.getHeight());
            }
            @Override
            public void componentResized(ComponentEvent event) {
                writer.println("GEOM;" + frame.getX() + ";" + frame.getY() + ";"
                    + frame.getWidth() + ";" + frame.getHeight());
            }
        });

        frame.setVisible(true);

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals("QUIT")) {
                break;
            } else if (line.equals("SHOW")) {
                SwingUtilities.invokeLater(() -> frame.setVisible(true));
            } else if (line.equals("HIDE")) {
                SwingUtilities.invokeLater(() -> frame.setVisible(false));
            } else if (line.startsWith("POS;")) {
                String[] parts = line.split(";", 6);
                if (parts.length >= 6) {
                    String px = parts[1];
                    String py = parts[2];
                    String pz = parts[3];
                    String dimName = parts[4];
                    String biomeName = parts[5];
                    SwingUtilities.invokeLater(() -> {
                        coordsLabel.setText("X: " + px + "  Y: " + py + "  Z: " + pz);
                        dimLabel.setText(dimName);
                        biomeLabel.setText(biomeName);
                        frame.setVisible(true);
                    });
                }
            }
        }

        frame.dispose();
        socket.close();
    }
}
