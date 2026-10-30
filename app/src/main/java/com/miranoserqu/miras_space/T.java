package com.miranoserqu.miras_space;

import de.gurkenlabs.input4j.InputDevices;

import java.util.Arrays;

import static de.gurkenlabs.input4j.InputDevices.init;

public class T {

    static float a0;
    static float a1;
    static float a2;
    static float a3;

    public static void main(String[] args){
        init().onDeviceConnected(device -> {
            device.onAxisChanged(0, f -> a0 = f);
            device.onAxisChanged(1, f -> a1 = f);
            device.onAxisChanged(2, f -> a2 = f);
            device.onAxisChanged(3, f -> a3 = f);
        });
        while(true){
            System.out.println("axis 0: " + a0);
            for (String s : Arrays.asList("axis 1: " + a1, "axis 2: " + a2, "axis 3: " + a3)) {
                System.out.println(s);
            }
        }
    }

}
