package dev.hiddencoords.ui;

import dev.hiddencoords.CoordOverlayMod;
import dev.hiddencoords.CoordinateSnapshot;
import dev.hiddencoords.OverlayConfig;
import dev.hiddencoords.overlay.OverlayProcessMain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public final class CoordWindow {
    private final OverlayConfig config = OverlayConfig.load();
    private volatile boolean available = true;
    private volatile boolean wantedVisible = false;

    private final Object writeLock = new Object();
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter writer;
    private Process overlayProcess;
    private Thread connectionThread;
    private volatile boolean shuttingDown;

    private volatile CoordinateSnapshot lastSnapshot;
    private long lastGeomSave;

    public void toggle() {
        wantedVisible = !wantedVisible;
        if (wantedVisible) show(); else hideFrame();
    }

    public void show() {
        if (!available) return;
        ensureRunning();
        send("SHOW");
    }

    public void hideForNoWorld() {
        send("HIDE");
    }

    public void update(CoordinateSnapshot snapshot) {
        if (!available || !wantedVisible) return;
        lastSnapshot = snapshot;
        if (!ensureRunning()) return;
        send("POS;" + snapshot.x() + ";" + snapshot.y() + ";" + snapshot.z() + ";"
            + sanitize(snapshot.dimension()) + ";" + sanitize(snapshot.biome()));
    }

    public void shutdown() {
        shuttingDown = true;
        send("QUIT");
        closeSocket();
        if (overlayProcess != null && overlayProcess.isAlive()) {
            try {
                overlayProcess.waitFor(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (overlayProcess.isAlive()) {
                overlayProcess.destroyForcibly();
            }
        }
        closeServerSocket();
    }

    private void hideFrame() {
        send("HIDE");
    }

    private synchronized boolean ensureRunning() {
        if (overlayProcess != null && overlayProcess.isAlive()) {
            synchronized (writeLock) {
                if (clientSocket != null && clientSocket.isConnected() && !clientSocket.isClosed()) {
                    return true;
                }
            }
            if (connectionThread != null && connectionThread.isAlive()) {
                return false;
            }
            stopConnection();
        } else if (overlayProcess != null && !overlayProcess.isAlive()) {
            stopConnection();
        }

        closeServerSocket();
        try {
            serverSocket = new ServerSocket(0, 1, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
            int port = serverSocket.getLocalPort();

            String classpath = findOverlayClasspath();
            if (classpath == null) {
                CoordOverlayMod.LOGGER.warn("Could not locate overlay classpath; overlay process unavailable");
                available = false;
                return false;
            }

            String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            ProcessBuilder pb = new ProcessBuilder(
                javaBin, "-cp", classpath, "-Djava.awt.headless=false",
                "dev.hiddencoords.overlay.OverlayProcessMain",
                String.valueOf(port),
                String.valueOf(config.fontSize),
                config.alwaysOnTop ? "1" : "0",
                String.valueOf(config.x),
                String.valueOf(config.y),
                String.valueOf(config.width),
                String.valueOf(config.height)
            );
            pb.redirectErrorStream(true);
            overlayProcess = pb.start();

            connectionThread = new Thread(this::connectionLoop, "overlay-connection");
            connectionThread.setDaemon(true);
            connectionThread.start();

            return true;
        } catch (Exception e) {
            CoordOverlayMod.LOGGER.warn("Failed to start overlay process", e);
            stopConnection();
            closeServerSocket();
            available = false;
            return false;
        }
    }

    private void connectionLoop() {
        try {
            Socket accepted = serverSocket.accept();
            synchronized (writeLock) {
                clientSocket = accepted;
                writer = new PrintWriter(
                    new OutputStreamWriter(accepted.getOutputStream(), StandardCharsets.UTF_8), true);
            }
            CoordOverlayMod.LOGGER.info("Overlay process connected");

            synchronized (writeLock) {
                if (wantedVisible) {
                    writer.println("SHOW");
                } else {
                    writer.println("HIDE");
                }
                if (lastSnapshot != null) {
                    writer.println("POS;" + lastSnapshot.x() + ";" + lastSnapshot.y() + ";"
                        + lastSnapshot.z() + ";" + sanitize(lastSnapshot.dimension()) + ";"
                        + sanitize(lastSnapshot.biome()));
                }
            }

            BufferedReader responseReader = new BufferedReader(
                new InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8));

            String response;
            while (!shuttingDown && (response = responseReader.readLine()) != null) {
                if (response.equals("HIDDEN")) {
                    wantedVisible = false;
                } else if (response.startsWith("GEOM;")) {
                    handleGeom(response);
                }
            }
        } catch (IOException e) {
            if (!shuttingDown) {
                CoordOverlayMod.LOGGER.warn("Overlay connection lost", e);
            }
        } finally {
            if (!shuttingDown) {
                closeSocket();
                overlayProcess = null;
                available = false;
            }
        }
    }

    private void handleGeom(String line) {
        String[] parts = line.split(";", 5);
        if (parts.length < 5) return;
        try {
            int gx = Integer.parseInt(parts[1]);
            int gy = Integer.parseInt(parts[2]);
            int gw = Integer.parseInt(parts[3]);
            int gh = Integer.parseInt(parts[4]);
            long now = System.currentTimeMillis();
            if (now - lastGeomSave < 1000) return;
            lastGeomSave = now;
            config.x = gx;
            config.y = gy;
            config.width = gw;
            config.height = gh;
            config.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void send(String command) {
        synchronized (writeLock) {
            if (writer != null) {
                writer.println(command);
            }
        }
    }

    private void closeSocket() {
        synchronized (writeLock) {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException ignored) {
            }
            clientSocket = null;
            writer = null;
        }
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        serverSocket = null;
    }

    private void stopConnection() {
        closeSocket();
        if (overlayProcess != null && overlayProcess.isAlive()) {
            overlayProcess.destroyForcibly();
        }
        overlayProcess = null;
        connectionThread = null;
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace(';', ',').replace('\n', ' ').replace('\r', ' ');
    }

    private static String findOverlayClasspath() {
        try {
            URL location = OverlayProcessMain.class.getProtectionDomain().getCodeSource().getLocation();
            String url = location.toExternalForm();
            if (url.startsWith("jar:")) {
                url = url.substring(4);
                int separator = url.indexOf("!/");
                if (separator != -1) url = url.substring(0, separator);
            }
            return Path.of(URI.create(url)).toAbsolutePath().toString();
        } catch (Exception e) {
            CoordOverlayMod.LOGGER.error("Failed to locate overlay classpath", e);
            return null;
        }
    }
}
