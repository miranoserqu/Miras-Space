package com.miranoserqu.miras_space.client;

import com.miranoserqu.miras_space.Game;
import com.miranoserqu.miras_space.event.SubscribeEvent;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.game.Player;
import com.miranoserqu.miras_space.network.PlayerUpdatePayload;

import java.util.UUID;
import org.lwjgl.opengl.GL11;

public class ClientPlayer extends Player {

    private ClientLevel level;

    public ClientPlayer(double x, double y, double orientation, double velocityX, double velocityY, UUID uuid, String name, ClientLevel level) {
        super(x, y, orientation, velocityX, velocityY, uuid, name);
        this.level = level;
    }

    public ClientPlayer(UUID uuid, String name, ClientLevel level){
        super(uuid, name);
        this.level = level;
    }

    @SubscribeEvent
    public void onPositionChange(PlayerUpdatePayload payload){
        this.x = payload.x;
        this.y = payload.y;
        this.velocityX = payload.vX;
        this.velocityY = payload.vY;
        this.orientation = payload.angle;
        this.accelerationX = payload.aX;
        this.accelerationY = payload.aY;
    }

    /**
     * Renders the player using LWJGL OpenGL.
     */
    public void render(org.lwjgl.opengl.GL11 gl, double screenX, double screenY) {
        float size = 25.0f;

        gl.glPushMatrix();
        gl.glTranslated(screenX, screenY, 0);
        gl.glRotated(Math.toDegrees(orientation), 0, 0, 1);

        gl.glColor3f(0.0f, 1.0f, 0.0f); // Green
        gl.glBegin(GL11.GL_TRIANGLES);
        gl.glVertex2f(-size, size);
        gl.glVertex2f(size, size);
        gl.glVertex2f(0, -size);
        gl.glEnd();

        gl.glPopMatrix();
    }

    /**
     * Calculates the screen position of the player based on world coordinates
     * and camera offsets.
     */
    public double[] getScreenPosition(double cameraX, double cameraY, int screenWidth, int screenHeight) {
        double screenX = (x - cameraX + screenWidth / 2.0);
        double screenY = (y - cameraY + screenHeight / 2.0);
        return new double[]{screenX, screenY};
    }

    /**
     * Calculates the orientation angle for screen rendering.
     * In LWJGL, angles need to be adjusted based on coordinate system.
     */
    public double getScreenAngle() {
        return orientation;
        // In LWJGL with y-up coordinate system, we might need to adjust
        // For now, keep as is (renderer will handle coordinate system)
    }

    /**
     * Gets the velocity magnitude for rendering effects.
     */
    public double getVelocityMagnitude() {
        return Math.hypot(velocityX, velocityY);
    }
}