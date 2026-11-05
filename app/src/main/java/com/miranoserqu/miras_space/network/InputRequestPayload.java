package com.miranoserqu.miras_space.network;

import com.miranoserqu.miras_space.game.Player;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class InputRequestPayload extends Payload {

    public static final int ID = 3;

    public InputRequestPayload(){

    }

    public InputRequestPayload(UUID playerUUID){
        this.playerUUID = playerUUID;
    }

    @Override
    public void serialize(DataOutputStream buf) throws IOException {
        buf.writeInt(ID);
        buf.writeLong(playerUUID.getMostSignificantBits());
        buf.writeLong(playerUUID.getLeastSignificantBits());
    }

    @Override
    public void deserialize(DataInputStream buf) throws IOException {
        long most = buf.readLong();
        long least = buf.readLong();
        this.playerUUID = new UUID(most, least);
    }

}
