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
import java.util.Collections;
import java.util.stream.Stream;
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
    private final DefaultListModel<String> historyListModel = new DefaultListModel<>();
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
        tabbedPane.addTab("History", createHistoryTab());
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
                if (currentImage != null && imageLabel.getParent() != null) {
                    int width = imageLabel.getParent().getWidth();
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

    private JPanel createHistoryTab() {
        JPanel historyTab = new JPanel(new BorderLayout());
        JList<String> fileList = new JList<>(historyListModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final Image[] previewImage = new Image[1];
        JLabel previewLabel = new JLabel("Select a screenshot to preview", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                if (previewImage[0] != null) {
                    int width = getWidth();
                    int imgWidth = previewImage[0].getWidth(null);
                    int imgHeight = previewImage[0].getHeight(null);
                    if (imgWidth > 0 && imgHeight > 0) {
                        int scaledHeight = (int) ((double) imgHeight * width / imgWidth);
                        g.drawImage(previewImage[0], 0, 0, width, scaledHeight, null);
                    }
                } else {
                    super.paintComponent(g);
                }
            }
        };

        JScrollPane previewScroll = new JScrollPane(previewLabel);
        previewScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    try {
                        byte[] bytes = Files.readAllBytes(directory.resolve(selectedFile));
                        previewImage[0] = new ImageIcon(bytes).getImage();
                        previewLabel.setText("");

                        int width = previewScroll.getViewport().getWidth();
                        int imgWidth = previewImage[0].getWidth(null);
                        int imgHeight = previewImage[0].getHeight(null);
                        if (width > 0 && imgWidth > 0 && imgHeight > 0) {
                            int scaledHeight = (int) ((double) imgHeight * width / imgWidth);
                            previewLabel.setPreferredSize(new Dimension(width, scaledHeight));
                        }
                        previewLabel.revalidate();
                        previewLabel.repaint();
                    } catch (IOException ex) {
                        System.err.println("Error loading preview: " + ex.getMessage());
                    }
                }
            }
        });

        previewScroll.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (previewImage[0] != null) {
                    int width = previewScroll.getViewport().getWidth();
                    int imgWidth = previewImage[0].getWidth(null);
                    int imgHeight = previewImage[0].getHeight(null);
                    if (width > 0 && imgWidth > 0 && imgHeight > 0) {
                        int scaledHeight = (int) ((double) imgHeight * width / imgWidth);
                        previewLabel.setPreferredSize(new Dimension(width, scaledHeight));
                        previewLabel.revalidate();
                    }
                }
            }
        });

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshHistory());

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JScrollPane(fileList), BorderLayout.CENTER);
        leftPanel.add(refreshButton, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, previewScroll);
        splitPane.setDividerLocation(200);

        historyTab.add(splitPane, BorderLayout.CENTER);

        refreshHistory();
        return historyTab;
    }

    private void refreshHistory() {
        historyListModel.clear();
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".jpg"))
                    .sorted(Collections.reverseOrder())
                    .forEach(historyListModel::addElement);
        } catch (IOException e) {
            System.err.println("Error refreshing history: " + e.getMessage());
        }
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
            String filename = "screenshot-" + timestamp + ".jpg";
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
            int width = imageLabel.getParent() != null ? imageLabel.getParent().getWidth() : imageLabel.getWidth();
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
