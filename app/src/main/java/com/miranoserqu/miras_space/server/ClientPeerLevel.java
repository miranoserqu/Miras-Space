package com.miranoserqu.miras_space.server;

import com.miranoserqu.miras_space.game.Game;
import com.miranoserqu.miras_space.client.ClientPlayer;
import com.miranoserqu.miras_space.event.SubscribeEvent;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.network.*;

import java.io.*;
import java.net.Socket;

public class ClientPeerLevel extends Level {

    private final ClientPlayer player;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isRunning;

    public ClientPeerLevel(ClientPlayer player, String serverHost, int serverPort) throws IOException {
        super();
        this.player = player;
        this.isRunning = true;

        Game.getInstance().getClientBus().register(this);
        Game.getInstance().registerServer(this); // Asumiendo que esta es tu lógica interna

        connectToServer(serverHost, serverPort);
    }

    private void connectToServer(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        // 1. Enviar paquete de registro
        ByteArrayOutputStream regBuffer = new ByteArrayOutputStream();
        DataOutputStream regDos = new DataOutputStream(regBuffer);
        regDos.writeLong(player.getUuid().getMostSignificantBits());
        regDos.writeLong(player.getUuid().getLeastSignificantBits());
        regDos.writeUTF(player.getName());
        regDos.flush();

        byte[] regData = regBuffer.toByteArray();
        out.writeInt(regData.length);
        out.write(regData);
        out.flush();

        // 2. Iniciar hilo de escucha para recibir updates del servidor
        new Thread(this::listenToServer, "Client-Listen-Thread").start();
        System.out.println("[CLIENTE] Conectado al servidor TCP exitosamente.");
    }

    private void listenToServer() {
        try {
            while (isRunning) {
                int length = in.readInt();
                byte[] packetData = new byte[length];
                in.readFully(packetData, 0, length);

                DataInputStream peekDis = new DataInputStream(new ByteArrayInputStream(packetData));
                int type = peekDis.readInt();
                switch (type) {
                    case PlayerUpdatePayload.ID -> {
                        PlayerUpdatePayload payload = new PlayerUpdatePayload();
                        payload.deserialize(peekDis);
                        Game.getInstance().getClientBus().post(payload);
                    }
                }
            }
        } catch (IOException e) {
            if (isRunning) System.err.println("Conexión con el servidor perdida: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- ENVIAR INPUTS AL SERVIDOR ---
    // Usamos synchronized porque múltiples eventos podrían intentar escribir en el socket a la vez

    @SubscribeEvent
    public synchronized void onPlayerInput(InputPayload payload) {
        sendPayload(payload);
    }

    @SubscribeEvent
    public synchronized void onPlayerGamepadInput(GamepadInputPayload payload) {
        sendPayload(payload);
    }

    private synchronized void sendPayload(Payload payload) {
        if (!isRunning || socket.isClosed()) return;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(buffer);
            payload.serialize(dos);
            dos.flush();

            byte[] data = buffer.toByteArray();
            out.writeInt(data.length); // Framing
            out.write(data);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error al enviar input al servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}