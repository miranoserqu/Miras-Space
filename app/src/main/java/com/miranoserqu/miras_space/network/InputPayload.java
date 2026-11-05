package com.miranoserqu.miras_space.network;

import com.miranoserqu.miras_space.game.Player;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InputPayload extends Payload {

    public boolean invertControls;
    public List<Integer> inputs;
    public double mouseX;
    public double mouseY;
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
        buf.writeDouble(targetAngle);
        buf.writeLong(playerUUID.getMostSignificantBits());
        buf.writeLong(playerUUID.getLeastSignificantBits());
    }

    @Override
    public void deserialize(DataInputStream buf) throws IOException {
        this.invertControls = buf.readBoolean();
        this.inputs = new ArrayList<>();
        int length = buf.readInt();
        for(int i = 0; i < length; i++){
            this.inputs.add(buf.readInt());
        }
        this.mouseX = buf.readDouble();
        this.mouseY = buf.readDouble();
        this.targetAngle = buf.readDouble();
        long most = buf.readLong();
        long least = buf.readLong();
        this.playerUUID = new UUID(most, least);
    }

}
