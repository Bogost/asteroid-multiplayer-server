package edu.bo.app.asteroid_multiplayer_server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import edu.bo.app.asteroid_multiplayer_server.exception.AlreadyExistException;
import edu.bo.app.asteroid_multiplayer_server.exception.NoSpaceInRoomException;
import edu.bo.app.asteroid_multiplayer_server.game.PlayerShip;

public class Player {

    public int points = 0;
    public volatile PlayerShip ship;

    private Socket socket;
    private PlayerTcpService pts;

    private DataInputStream dataInput;
    private DataOutputStream dataOutput;

    private Room room;
    private int roomPosition = -1;
    private String name;
    private boolean ready = false;

    private volatile Chatter chatter = null;

    volatile public long lastMessageTime;

    public Player(Socket socket, PlayerTcpService pts) throws IOException {
        this.socket = socket;
        this.pts = pts;

        dataInput = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        dataOutput = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

    }

    public void closeConnection() throws IOException {
        if (roomPosition != -1) {
            room.removePlayer(roomPosition);
        }
        if (chatter != null) {
            chatter.setPlayer(null);
            chatter.closeConnection();
        }
        dataInput.close();
        dataOutput.close();
        socket.close();
    }

    public void refuseFurtherConnection() throws IOException {
        dataOutput.writeInt(0);
        dataOutput.flush();
        closeConnection();
    }

    public DataOutputStream getDataOutput() {
        return dataOutput;
    }

    public int getRoomPosition() {
        return roomPosition;
    }

    public void setRoomPosition(int position) {
        roomPosition = position;
    }

    public String getName() {
        return name;
    }

    public boolean isReady() {
        return ready;
    }

    public void start() {
        Player thisPlayer = this;
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    // get start command
                    int command = dataInput.readInt();
                    name = dataInput.readUTF();
                    String roomName = dataInput.readUTF();
                    int chat = dataInput.readInt();
                    lastMessageTime = System.currentTimeMillis();

                    System.out.println("get client: " + name + " data");
                    // get proper room manager based on chat availability
                    RoomManager roomManager;
                    if (chat == 0) {
                        roomManager = pts.gameRooms;
                    } else if (chat == 1) {
                        roomManager = pts.gameChatRooms;
                    } else {
                        System.err.println("wrong chat command");
                        closeConnection();
                        return;
                    }
                    System.out.println("get room manager");

                    // getting room
                    if (command == 1) {
                        room = new Room(roomName);
                        try {
                            roomManager.addRoom(room);
                        } catch (AlreadyExistException e) {
                            refuseFurtherConnection();
                            return;
                        }
                    } else if (command == 2) {
                        room = roomManager.getRoom(roomName);
                        if (room == null) {
                            refuseFurtherConnection();
                            return;
                        }
                    } else {
                        closeConnection();
                        return;
                    }
                    System.out.println("get room");

                    // adding player
                    try {
                        room.addPlayer(thisPlayer);
                    } catch (NoSpaceInRoomException | AlreadyExistException e) {
                        refuseFurtherConnection();
                        return;
                    }
                    System.out.println("player added");

                    // start hearing loop
                    int position;
                    while (true) {
                        command = dataInput.readInt();
                        lastMessageTime = System.currentTimeMillis();
                        System.out.println("command: " + command);
                        if (room.isInGame()) {
                            // accelerate
                            if (command == -1) {
                                ship.accelerate();
                            } // decelerate
                            else if (command == -2) {
                                ship.decelerate();
                            } // rotate right
                            else if (command == -3) {
                                ship.rotateLeft();
                            } // rotate left
                            else if (command == -4) {
                                ship.rotateRight();
                            } // fire bullet
                            else if (command == -5) {
                                ship.fireBullet();
                            }
                        }

                        // player ready
                        if (command == 3) {
                            if (ready) {
                                closeConnection();
                            }
                            ready = true;
                            room.propagatePlayerAsReady(roomPosition);
                        }// kick out the player
                        else if (command == 4) {
                            if (roomPosition != room.getHostPosition()) {
                                closeConnection();
                            }
                            position = dataInput.readInt();
                            room.removePlayer(position);
                        }// start game
                        else if (command == 5) {
                            if (roomPosition != room.getHostPosition()) {
                                closeConnection();
                            }
                            room.startGame();
                        }

                    }

                } catch (IOException e) {
                    try {
                        closeConnection();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    public void setChatter(Chatter thisChatter) {
        chatter = thisChatter;
    }

    public Chatter getChatter() {
        return chatter;
    }
}
