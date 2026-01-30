package com.github.moaxcp.spyware;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

public class SpywareGui extends JFrame {
    private final WebServer server;
    private final JButton startButton;
    private final JButton stopButton;
    private final JButton restartButton;
    private final JLabel statusLabel;
    private int port = 8080;

    public SpywareGui(WebServer server) {
        this.server = server;
        setTitle("Spyware Web Server Control");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(new FlowLayout());

        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        restartButton = new JButton("Restart");
        statusLabel = new JLabel("Status: Stopped");

        startButton.addActionListener(this::handleStart);
        stopButton.addActionListener(this::handleStop);
        restartButton.addActionListener(this::handleRestart);

        add(startButton);
        add(stopButton);
        add(restartButton);
        add(statusLabel);

        setupSystemTray();
        updateStatus();
    }

    private void updateStatus() {
        if (server.isRunning()) {
            statusLabel.setText("Status: Running on port " + port);
            updateButtons(true);
        } else {
            statusLabel.setText("Status: Stopped");
            updateButtons(false);
        }
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray is not supported");
            return;
        }

        PopupMenu popup = new PopupMenu();
        MenuItem openItem = new MenuItem("Open");
        MenuItem exitItem = new MenuItem("Exit");

        openItem.addActionListener(e -> {
            if (checkPassword()) {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
            }
        });

        exitItem.addActionListener(e -> {
            if (checkPassword()) {
                server.stop();
                System.exit(0);
            }
        });

        popup.add(openItem);
        popup.addSeparator();
        popup.add(exitItem);

        Image image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D) image.getGraphics();
        g2.setColor(Color.RED);
        g2.fillRect(0, 0, 16, 16);
        g2.dispose();

        TrayIcon trayIcon = new TrayIcon(image, "Spyware Control", popup);
        trayIcon.setImageAutoSize(true);

        trayIcon.addActionListener(e -> {
            if (isVisible()) {
                setVisible(false);
            } else {
                if (checkPassword()) {
                    setVisible(true);
                    setExtendedState(JFrame.NORMAL);
                }
            }
        });

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }
    }

    public void setPort(int port) {
        this.port = port;
        updateStatus();
    }

    private boolean checkPassword() {
        JPasswordField pf = new JPasswordField();
        int okCxl = JOptionPane.showConfirmDialog(null, pf, "Enter Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (okCxl == JOptionPane.OK_OPTION) {
            String password = new String(pf.getPassword());
            if ("password1234".equals(password)) {
                return true;
            } else {
                JOptionPane.showMessageDialog(null, "Incorrect password", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    private void handleStart(ActionEvent e) {
        new Thread(() -> {
            try {
                server.start(port);
                int actualPort = server.getPort();
                SwingUtilities.invokeLater(() -> {
                    setPort(actualPort);
                    updateStatus();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Error starting server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void handleStop(ActionEvent e) {
        server.stop();
        updateStatus();
    }

    private void handleRestart(ActionEvent e) {
        new Thread(() -> {
            server.stop();
            try {
                server.start(port);
                int actualPort = server.getPort();
                SwingUtilities.invokeLater(() -> {
                    setPort(actualPort);
                    updateStatus();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    updateStatus();
                    JOptionPane.showMessageDialog(this, "Error restarting server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void updateButtons(boolean running) {
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
        restartButton.setEnabled(running);
    }
}
