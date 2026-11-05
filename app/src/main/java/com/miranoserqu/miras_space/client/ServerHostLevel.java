package com.miranoserqu.miras_space.client;

import com.miranoserqu.miras_space.game.Game;
import com.miranoserqu.miras_space.event.SubscribeEvent;
import com.miranoserqu.miras_space.game.Level;
import com.miranoserqu.miras_space.network.*;
import com.miranoserqu.miras_space.server.ServerLevel;
import com.miranoserqu.miras_space.server.ServerPlayer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerHostLevel extends Level {

    private final ServerLevel serverLevel;
    private final int port;
    private ServerSocket serverSocket;
    private boolean isRunning;

    // Almacena las conexiones TCP activas de cada cliente
    private final Map<UUID, ClientConnection> activeClients = new ConcurrentHashMap<>();

    public ServerHostLevel(int port, ServerLevel serverLevel) {
        super();
        this.serverLevel = serverLevel;
        this.port = port;
        this.isRunning = true;

        Game.getInstance().getServerBus().register(this);
        Game.getInstance().registerClient(this); // Asumiendo que esta es tu lógica interna
    }

    public void start() {
        new Thread(this::acceptConnections, "Server-Accept-Thread").start();
        System.out.println("Servidor TCP escuchando en el puerto " + port);
    }

    private void acceptConnections() {
        try {
            serverSocket = new ServerSocket(port);
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                ClientConnection connection = new ClientConnection(clientSocket);
                connection.start();
            }
        } catch (IOException e) {
            if (isRunning) System.err.println("Error en el servidor TCP: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
            for (ClientConnection conn : activeClients.values()) {
                conn.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- ENVIAR DATOS A LOS CLIENTES ---

    @SubscribeEvent
    public void onPlayerInputRequest(InputRequestPayload payload) {
        broadcastPayload(payload);
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdatePayload payload) {
        broadcastPayload(payload);
    }

    private void broadcastPayload(Payload payload) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(buffer);
            payload.serialize(dos);
            dos.flush();
            byte[] data = buffer.toByteArray();

            // Enviar a todos los clientes conectados de forma concurrente
            for (ClientConnection client : activeClients.values()) {
                client.sendPacket(data);
            }
        } catch (IOException e) {
            System.err.println("Error serializando payload para broadcast: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- MANEJADOR DE CONEXIÓN POR CLIENTE ---

    private class ClientConnection extends Thread {
        private final Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        private UUID clientId;

        public ClientConnection(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                // 1. Fase de Registro (Primer paquete esperado)
                int regLength = in.readInt();
                byte[] regData = new byte[regLength];
                in.readFully(regData);
                DataInputStream regDis = new DataInputStream(new ByteArrayInputStream(regData));

                this.clientId = new UUID(regDis.readLong(), regDis.readLong());
                String clientName = regDis.readUTF();

                activeClients.put(clientId, this);
                ServerPlayer serverPlayer = new ServerPlayer(clientId, clientName, serverLevel);
                serverLevel.addPlayer(serverPlayer);
                System.out.println("[REGISTRO] Cliente conectado: " + clientName + " (" + clientId + ")");

                // 2. Bucle de escucha continuo
                while (isRunning) {
                    try {
                        int length = in.readInt(); // Leer tamaño del paquete entrante
                        byte[] packetData = new byte[length];
                        in.readFully(packetData, 0, length); // Leer payload exacto

                        // Inspeccionar el ID
                        DataInputStream peekDis = new DataInputStream(new ByteArrayInputStream(packetData));
                        int type = peekDis.readInt();
                        switch(type) {
                            case InputPayload.ID -> {
                                InputPayload payload = new InputPayload();
                                payload.deserialize(peekDis);
                                Game.getInstance().getServerBus().post(payload);
                            }
                            case GamepadInputPayload.ID -> {
                                GamepadInputPayload payload = new GamepadInputPayload();
                                payload.deserialize(peekDis);
                                Game.getInstance().getServerBus().post(payload);
                            }
                        }
                    } catch (Exception e) {
                        // AHORA EL HILO NO MORIRÁ, SOLO DESCARTARÁ EL PAQUETE MALO
                        System.err.println("[ERROR EN SERVIDOR] Fallo al procesar paquete del cliente " + clientId);
                        e.printStackTrace(); // Esto te dirá exactamente qué línea de tu lógica está crasheando
                    }
                }
            } catch (EOFException e) {
                System.out.println("[DESCONEXIÓN] El cliente ha cerrado el juego: " + clientId);
                e.printStackTrace();
            } catch (Throwable fatalCrash) {
                System.err.println("[CRASH FATAL EN SERVIDOR] El hilo del cliente ha muerto por este error:");
                fatalCrash.printStackTrace();
            } finally {
                close();
            }
        }

        public synchronized void sendPacket(byte[] data) {
            try {
                out.writeInt(data.length); // Framing: Enviar longitud primero
                out.write(data);
                out.flush();
            } catch (IOException e) {
                System.err.println("Error enviando paquete al cliente " + clientId);
                e.printStackTrace();
                close();
            }
        }

        public void close() {
            try {
                if (clientId != null) activeClients.remove(clientId);
                if (socket != null) socket.close();
            } catch (IOException ignored) {
                ignored.printStackTrace();
            }
        }
    }
}