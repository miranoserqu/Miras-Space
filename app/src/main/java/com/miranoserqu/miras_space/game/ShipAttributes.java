package com.miranoserqu.miras_space.game;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Configuration attributes for the player's spaceship.
 * Stored in JSON format using standard Java libraries (java.util.Properties for simplicity,
 * with manual JSON-like formatting for the required tag format).
 */
public class ShipAttributes {
    private double baseAcceleration = 0.8;
    private double maxSpeedX = 8.0;
    private double maxSpeedY = 8.0;
    private double rotationSpeed = 3.0;
    private double decelerationFactor = 0.95;
    private double forwardAcceleration = 1.0;
    private double lateralAcceleration = 0.7;

    private static final String ATTRIBUTES_FILE = "ship_attributes.json";

    /**
     * Creates default ship attributes.
     */
    public ShipAttributes() {}

    /**
     * Creates ship attributes with specific values.
     */
    public ShipAttributes(double baseAcceleration, double maxSpeedX, double maxSpeedY,
                          double rotationSpeed, double decelerationFactor,
                          double forwardAcceleration, double lateralAcceleration) {
        this.baseAcceleration = baseAcceleration;
        this.maxSpeedX = maxSpeedX;
        this.maxSpeedY = maxSpeedY;
        this.rotationSpeed = rotationSpeed;
        this.decelerationFactor = decelerationFactor;
        this.forwardAcceleration = forwardAcceleration;
        this.lateralAcceleration = lateralAcceleration;
    }

    /**
     * Loads ship attributes from JSON file using manual parsing.
     *
     * @return loaded ShipAttributes
     */
    public static ShipAttributes loadFromFile() {
        try {
            String content = Files.readString(Path.of(ATTRIBUTES_FILE));
            return parseFromJson(content);
        } catch (IOException e) {
            System.out.println("Could not load ship attributes from " + ATTRIBUTES_FILE + ". Using defaults.");
            return new ShipAttributes();
        }
    }

    /**
     * Parses JSON string manually.
     */
    private static ShipAttributes parseFromJson(String content) {
        ShipAttributes attr = new ShipAttributes();
        // Simple manual parsing for key-value pairs
        String[] lines = content.split("[{},]");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains(":")) {
                String[] parts = trimmed.split(":");
                if (parts.length == 2) {
                    String key = parts[0].replaceAll("\"", "").trim();
                    String value = parts[1].replaceAll("\"", "").trim();
                    try {
                        switch (key) {
                            case "baseAcceleration":
                                attr.baseAcceleration = Double.parseDouble(value);
                                break;
                            case "maxSpeedX":
                                attr.maxSpeedX = Double.parseDouble(value);
                                break;
                            case "maxSpeedY":
                                attr.maxSpeedY = Double.parseDouble(value);
                                break;
                            case "rotationSpeed":
                                attr.rotationSpeed = Double.parseDouble(value);
                                break;
                            case "decelerationFactor":
                                attr.decelerationFactor = Double.parseDouble(value);
                                break;
                            case "forwardAcceleration":
                                attr.forwardAcceleration = Double.parseDouble(value);
                                break;
                            case "lateralAcceleration":
                                attr.lateralAcceleration = Double.parseDouble(value);
                                break;
                        }
                    } catch (NumberFormatException ex) {
                        // Skip invalid values
                    }
                }
            }
        }
        return attr;
    }

    /**
     * Saves ship attributes to JSON file.
     */
    public void saveToFile() {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("    \"baseAcceleration\": ").append(baseAcceleration).append(",\n");
            json.append("    \"maxSpeedX\": ").append(maxSpeedX).append(",\n");
            json.append("    \"maxSpeedY\": ").append(maxSpeedY).append(",\n");
            json.append("    \"rotationSpeed\": ").append(rotationSpeed).append(",\n");
            json.append("    \"decelerationFactor\": ").append(decelerationFactor).append(",\n");
            json.append("    \"forwardAcceleration\": ").append(forwardAcceleration).append(",\n");
            json.append("    \"lateralAcceleration\": ").append(lateralAcceleration).append("\n");
            json.append("}\n");

            Files.writeString(Path.of(ATTRIBUTES_FILE), json.toString());
        } catch (IOException e) {
            System.out.println("Could not save ship attributes to " + ATTRIBUTES_FILE + ": " + e.getMessage());
        }
    }

    // Getters and Setters
    public double getBaseAcceleration() { return baseAcceleration; }
    public void setBaseAcceleration(double v) { this.baseAcceleration = v; }

    public double getMaxSpeedX() { return maxSpeedX; }
    public void setMaxSpeedX(double v) { this.maxSpeedX = v; }

    public double getMaxSpeedY() { return maxSpeedY; }
    public void setMaxSpeedY(double v) { this.maxSpeedY = v; }

    public double getRotationSpeed() { return rotationSpeed; }
    public void setRotationSpeed(double v) { this.rotationSpeed = v; }

    public double getDecelerationFactor() { return decelerationFactor; }
    public void setDecelerationFactor(double v) { this.decelerationFactor = v; }

    public double getForwardAcceleration() { return forwardAcceleration; }
    public void setForwardAcceleration(double v) { this.forwardAcceleration = v; }

    public double getLateralAcceleration() { return lateralAcceleration; }
    public void setLateralAcceleration(double v) { this.lateralAcceleration = v; }

    @Override
    public String toString() {
        return "ShipAttributes{" +
                "baseAcceleration=" + baseAcceleration +
                ", maxSpeedX=" + maxSpeedX +
                ", maxSpeedY=" + maxSpeedY +
                ", rotationSpeed=" + rotationSpeed +
                ", decelerationFactor=" + decelerationFactor +
                ", forwardAcceleration=" + forwardAcceleration +
                ", lateralAcceleration=" + lateralAcceleration +
                '}';
    }
}