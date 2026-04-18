package com.github.moaxcp.spyware;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service for capturing screenshots as JPEG bytes.
 */
public class ScreenshotService {

    /**
     * Captures a screenshot of all available screens and returns the image bytes in JPEG format.
     *
     * @return JPEG-encoded bytes of the captured screenshot
     * @throws IllegalStateException if the environment is headless and cannot capture a screen
     * @throws AWTException          if the screenshot capture fails due to AWT/Robot issues
     * @throws IOException           if encoding the image to JPEG fails
     */
    public byte[] captureJpeg() throws AWTException, IOException {
        return captureJpeg(getVirtualScreenBounds());
    }

    /**
     * Captures a screenshot of the specified bounds and returns the image bytes in JPEG format.
     *
     * @param bounds the area to capture
     * @return JPEG-encoded bytes of the captured screenshot
     * @throws IllegalStateException if the environment is headless and cannot capture a screen
     * @throws AWTException          if the screenshot capture fails due to AWT/Robot issues
     * @throws IOException           if encoding the image to JPEG fails
     */
    public byte[] captureJpeg(Rectangle bounds) throws AWTException, IOException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Cannot capture screenshot in headless environment");
        }

        Robot robot = new Robot();
        BufferedImage image = robot.createScreenCapture(bounds);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean ok = ImageIO.write(image, "jpeg", baos);
            if (!ok) {
                throw new IOException("No JPEG writer available");
            }
            return baos.toByteArray();
        }
    }

    /**
     * Calculates the union of all screen device bounds so the screenshot covers all displays.
     */
    private Rectangle getVirtualScreenBounds() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();
        Rectangle all = new Rectangle();
        for (GraphicsDevice device : devices) {
            GraphicsConfiguration gc = device.getDefaultConfiguration();
            Rectangle b = gc.getBounds();
            all = all.union(b);
        }
        if (all.isEmpty()) {
            // Fallback to default toolkit screen size
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            all = new Rectangle(0, 0, size.width, size.height);
        }
        return all;
    }
}
