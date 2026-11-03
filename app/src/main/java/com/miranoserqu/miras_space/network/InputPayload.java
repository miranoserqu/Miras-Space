package com.miranoserqu.miras_space.network;

import com.miranoserqu.miras_space.game.Player;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class InputPayload implements Payload {

    public boolean invertControls;
    public List<Integer> inputs;
    public double mouseX;
    public double mouseY;
    public UUID playerUUID;
    public double targetAngle;
    public static final int ID = 0;

    public InputPayload(){

    }

    public InputPayload(double mouseX, double mouseY, List<Integer> inputs, double targetAngle, UUID playerUUID, boolean invertControls){
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.inputs = inputs;
        this.targetAngle = targetAngle;
        this.playerUUID = playerUUID;
        this.invertControls = invertControls;
    }

    @Override
    public void serialize(DataOutputStream buf) throws IOException {
        buf.writeInt(ID);
        buf.writeBoolean(this.invertControls);
        buf.writeInt(inputs.size());
        for(Integer i : inputs){
            buf.writeInt(i);
        }
        buf.writeDouble(mouseX);
        buf.writeDouble(mouseY);
        buf.writeLong(playerUUID.getMostSignificantBits());
        buf.writeLong(playerUUID.getLeastSignificantBits());
    }

    @Override
    public void deserialize(DataInputStream buf) throws IOException {
        if(buf.readInt() != ID) throw new RuntimeException("payload type does not match");
        this.invertControls = buf.readBoolean();
        for(int i = 0; i < buf.readInt(); i++){
            this.inputs.add(buf.readInt());
        }
        this.mouseX = buf.readDouble();
        this.mouseY = buf.readDouble();
        long most = buf.readLong();
        long least = buf.readLong();
        this.playerUUID = new UUID(most, least);
    }
}
