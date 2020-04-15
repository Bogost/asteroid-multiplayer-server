package edu.bo.app.asteroid_multiplayer_server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Chatter {

    private Socket socket;
    private PlayerTcpService pts;

    private DataInputStream dataInput;
    private DataOutputStream dataOutput;

    private volatile Player player = null;

    private SoundMixer soundMixer = null;

    public Chatter(Socket socket, PlayerTcpService pts) throws IOException {
        this.socket = socket;
        this.pts = pts;

        dataInput = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        dataOutput = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public DataInputStream getInputStream() {
        return dataInput;
    }

    public DataOutputStream getOutputStream() {
        return dataOutput;
    }

    public void closeConnection() throws IOException {

        dataInput.close();
        dataOutput.close();
        socket.close();
        if (player != null) {
            player.setChatter(null);
            player.closeConnection();
        }
        if (soundMixer != null) {
            soundMixer.chatters.remove(this);
        }
    }

    private void refuseFurtherConnection() throws IOException {
        dataOutput.writeInt(0);
        dataOutput.flush();
        closeConnection();
    }

    public void setPlayer(Player p) {
        player = p;
    }

    public void start() {
        Chatter thisChatter = this;
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    int command = dataInput.readInt();
                    String name = dataInput.readUTF();
                    String roomName = dataInput.readUTF();
                    if (command != 1) {
                        closeConnection();
                        return;
                    }

                    RoomManager roomManager = pts.gameChatRooms;
                    Room room = roomManager.getRoom(roomName);
                    if (room == null) {
                        refuseFurtherConnection();
                        return;
                    }

                    player = room.getPlayer(name);
                    if (player == null) {
                        refuseFurtherConnection();
                        return;
                    }

                    player.setChatter(thisChatter);

                    // send acceptance
                    dataOutput.writeInt(10);
                    dataOutput.flush();

                    boolean beginMixing = false;
                    if (room.soundMixer == null) {
                        room.soundMixer = new SoundMixer();
                        beginMixing = true;
                    }
                    room.soundMixer.chatters.add(thisChatter);
                    if (beginMixing) {
                        room.soundMixer.start();
                    }
                    soundMixer = room.soundMixer;

                } catch (IOException e) {
                    try {
                        closeConnection();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

}
