package com.miranoserqu.miras_space.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class InputRequestPayload implements Payload {

    public static final int ID = 3;

    @Override
    public void serialize(DataOutputStream buf) throws IOException {
        buf.writeInt(ID);
    }

    @Override
    public void deserialize(DataInputStream buf) throws IOException {
        if(buf.readInt() != ID) throw new RuntimeException("payload type does not match");
    }
}
