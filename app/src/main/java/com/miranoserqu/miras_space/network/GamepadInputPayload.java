package com.miranoserqu.miras_space.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GamepadInputPayload implements Payload {

    public float xAxis;
    public float yAxis;
    public boolean a;
    public boolean b;
    public boolean x;
    public boolean y;
    public boolean lt;
    public boolean lb;
    public boolean rt;
    public boolean rb;
    public boolean start;
    public boolean right;
    public boolean left;
    public boolean up;
    public boolean down;

    public GamepadInputPayload(float xAxis, float yAxis, boolean a, boolean b, boolean x, boolean y, boolean lt, boolean lb, boolean rt, boolean rb, boolean start, boolean right, boolean left, boolean up, boolean down){
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.a = a;
        this.b = b;
        this.x = x;
        this.y = y;
        this.lt = lt;
        this.lb = lb;
        this.rt = rt;
        this.rb = rb;
        this.start = start;
        this.right = right;
        this.left = left;
        this.up = up;
        this.down = down;
    }

    @Override
    public void serialize(DataOutputStream buf) throws IOException {
        buf.writeFloat(xAxis);
        buf.writeFloat(yAxis);
        buf.writeBoolean(a);
        buf.writeBoolean(b);
        buf.writeBoolean(x);
        buf.writeBoolean(y);
        buf.writeBoolean(lt);
        buf.writeBoolean(lb);
        buf.writeBoolean(rt);
        buf.writeBoolean(rb);
        buf.writeBoolean(start);
        buf.writeBoolean(right);
        buf.writeBoolean(left);
        buf.writeBoolean(up);
        buf.writeBoolean(down);
    }

    @Override
    public void deserialize(DataInputStream buf) throws IOException {
        this.xAxis = buf.readFloat();
        this.yAxis = buf.readFloat();
        this.a = buf.readBoolean();
        this.b = buf.readBoolean();
        this.x = buf.readBoolean();
        this.y = buf.readBoolean();
        this.lt = buf.readBoolean();
        this.lb = buf.readBoolean();
        this.rt = buf.readBoolean();
        this.rb = buf.readBoolean();
        this.start = buf.readBoolean();
        this.right = buf.readBoolean();
        this.left = buf.readBoolean();
        this.up = buf.readBoolean();
        this.down = buf.readBoolean();
    }

}
