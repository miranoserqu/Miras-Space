package com.miranoserqu.miras_space.client;

import net.java.games.input.*;
import java.util.*;

public class GamepadInput {
    private Controller controller;
    private float[] axes = new float[4];
    private boolean[] buttons = new boolean[10];
    private boolean found = false;

    public GamepadInput() {
        ControllerEnvironment env = ControllerEnvironment.getDefaultEnvironment();
        Controller[] controllers = env.getControllers();
        for (Controller c : controllers) {
            if (c.getType() == Controller.Type.GAMEPAD || c.getType() == Controller.Type.STICK) {
                controller = c;
                found = true;
                System.out.println("Gamepad conectado: " + c.getName());
                break;
            }
        }
    }

    public void poll() {
        if (controller == null) return;
        controller.poll();
        EventQueue queue = controller.getEventQueue();
        Event event;
        while (queue.getNextEvent(event = new Event())) {
            Component comp = event.getComponent();
            float val = event.getValue();
            String id = comp.getIdentifier().getName();
            if (id.contains("X") || id.contains("Rotation")) axes[0] = val;
            else if (id.contains("Y") || id.contains("Z")) axes[1] = val;
            else if (id.contains("Z") || id.contains("Pz")) axes[2] = val;
            else {
                for (int i = 0; i < buttons.length; i++) {
                    if (id.contains("Button") && id.contains(String.valueOf(i + 1))) {
                        buttons[i] = val > 0.5f;
                    }
                }
            }
        }
    }

    public double getAxisX() { poll(); return axes[0]; }
    public double getAxisY() { poll(); return axes[1]; }
    public boolean isButtonA() { poll(); return buttons[0]; }
    public boolean isButtonB() { poll(); return buttons[1]; }
    public boolean isConnected() { return found && controller != null; }
}
