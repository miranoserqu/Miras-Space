package com.miranoserqu.miras_space.entity;

import org.lwjgl.opengl.GL11;

import java.util.UUID;

public class RemotePlayer extends Entity {

    private String name;

    public RemotePlayer(double x, double y, double orientation, double velocityX, double velocityY, UUID uuid, String name){
        super(x, y, orientation, velocityX, velocityY, uuid);
        this.name = name;
    }

    @Override
    public void render(GL11 gl, double screenX, double screenY) {
        float size = 25.0f;

        gl.glPushMatrix();
        gl.glTranslated(this.x - screenX, this.y - screenY, 0);
        gl.glRotated(Math.toDegrees(orientation), 0, 0, 1);

        gl.glColor3f(0.0f, 1.0f, 0.0f); // Green
        gl.glBegin(GL11.GL_TRIANGLES);
        gl.glVertex2f(-size, size);
        gl.glVertex2f(size, size);
        gl.glVertex2f(0, -size);
        gl.glEnd();

        gl.glPopMatrix();
    }
}
