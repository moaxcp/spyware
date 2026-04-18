package com.github.moaxcp.spyware;

import org.junit.jupiter.api.Test;
import java.awt.*;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ScreenshotServiceTest {

    @Test
    public void testCaptureJpegWithBounds() throws AWTException, IOException {
        ScreenshotService service = new ScreenshotService();
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Headless environment, skipping test");
            return;
        }

        Rectangle bounds = new Rectangle(0, 0, 100, 100);
        byte[] screenshot = service.captureJpeg(bounds);

        assertNotNull(screenshot);
        assertTrue(screenshot.length > 0);
    }

    @Test
    public void testCaptureJpegDefault() throws AWTException, IOException {
        ScreenshotService service = new ScreenshotService();
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Headless environment, skipping test");
            return;
        }

        byte[] screenshot = service.captureJpeg();

        assertNotNull(screenshot);
        assertTrue(screenshot.length > 0);
    }

    @Test
    public void testCaptureJpegInvalidBounds() {
        ScreenshotService service = new ScreenshotService();
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Headless environment, skipping test");
            return;
        }

        Rectangle bounds = new Rectangle(0, 0, -1, -1);
        assertThrows(IllegalArgumentException.class, () -> service.captureJpeg(bounds));
    }
}
