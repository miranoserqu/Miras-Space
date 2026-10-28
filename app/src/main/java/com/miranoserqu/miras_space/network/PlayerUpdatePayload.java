package com.miranoserqu.miras_space.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class PlayerUpdatePayload implements Payload {

    public UUID playerUUID;
    public double x;
    public double y;
    public double angle;
    public double vX;
    public double vY;
    public double aX;
    public double aY;

    public PlayerUpdatePayload(UUID playerUUID, double x, double y, double angle, double vX, double vY, double aX, double aY){
        this.playerUUID = playerUUID;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.vX = vX;
        this.vY = vY;
        this.aX = aX;
        this.aY = aY;
    }

    @Override
    public void serialize(DataOutputStream buf) throws IOException {
        buf.writeLong(playerUUID.getMostSignificantBits());
        buf.writeLong(playerUUID.getLeastSignificantBits());
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(angle);
        buf.writeDouble(vX);
        buf.writeDouble(vY);
        buf.writeDouble(aX);
        buf.writeDouble(aY);
    }

    @Override
    public void deserialize(DataInputStream buf) throws IOException {
        long most = buf.readLong();
        long least = buf.readLong();
        this.playerUUID = new UUID(most, least);
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.angle = buf.readDouble();
        this.vX = buf.readDouble();
        this.vY = buf.readDouble();
        this.aX = buf.readDouble();
        this.aY = buf.readDouble();
    }
}
