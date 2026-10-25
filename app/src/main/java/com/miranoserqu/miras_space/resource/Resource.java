package com.miranoserqu.miras_space.resource;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Manages game resources such as images and sounds.
 * Resources are loaded from the classpath and cached for efficient access.
 */
public class Resource {

    private static final Resource INSTANCE = new Resource();

    // Cache for images: key is the path string, value is the BufferedImage
    private final Map<String, BufferedImage> imageCache = new HashMap<>();
    // Cache for sounds: key is the path string, value is the Clip
    private final Map<String, Clip> soundCache = new HashMap<>();

    private Resource() {
        // Private constructor to prevent instantiation
    }

    /**
     * Returns the singleton instance of Resource.
     *
     * @return the Resource instance
     */
    public static Resource getInstance() {
        return INSTANCE;
    }

    /**
     * Loads and returns an image from the classpath.
     * The image is cached after the first load.
     *
     * @param path the classpath path to the image (e.g., "/images/spaceship.png")
     * @return the BufferedImage
     * @throws IllegalArgumentException if the image cannot be loaded
     */
    public BufferedImage getImage(String path) {
        return imageCache.computeIfAbsent(path, this::loadImage);
    }

    /**
     * Loads an image from the classpath.
     *
     * @param path the classpath path
     * @return the loaded BufferedImage
     * @throws IllegalArgumentException if the image cannot be loaded
     */
    private BufferedImage loadImage(String path) {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Image not found: " + path);
            }
            return ImageIO.read(stream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load image: " + path, e);
        }
    }

    /**
     * Loads and returns a sound clip from the classpath.
     * The sound is cached after the first load.
     *
     * @param path the classpath path to the sound (e.g., "/sounds/laser.wav")
     * @return the Clip
     * @throws IllegalArgumentException if the sound cannot be loaded
     */
    public Clip getSound(String path) {
        return soundCache.computeIfAbsent(path, this::loadSound);
    }

    /**
     * Loads a sound clip from the classpath.
     *
     * @param path the classpath path
     * @return the loaded Clip
     * @throws IllegalArgumentException if the sound cannot be loaded
     */
    private Clip loadSound(String path) {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Sound not found: " + path);
            }
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(stream);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            throw new IllegalArgumentException("Failed to load sound: " + path, e);
        }
    }

    /**
     * Clears all cached resources. Useful for freeing memory or when reloading resources.
     */
    public void clearCache() {
        imageCache.clear();
        // Close all sound clips to free system resources
        for (Clip clip : soundCache.values()) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.close();
        }
        soundCache.clear();
    }
}