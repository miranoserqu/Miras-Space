package miras.space;

import com.miranoserqu.miras_space.resource.Resource;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test for the App.
 */
class AppTest {

    @Test
    void testResourceSingleton() {
        Resource resource1 = Resource.getInstance();
        Resource resource2 = Resource.getInstance();
        assertSame(resource1, resource2, "Resource should be a singleton");
    }

    @Test
    void testResourceClearCache() {
        Resource resource = Resource.getInstance();
        // This should not throw an exception
        assertDoesNotThrow(() -> resource.clearCache());
    }

    // Note: Testing actual resource loading would require having test resources
    // For now, we just verify the class structure and singleton pattern
}