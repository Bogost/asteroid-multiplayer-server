package edu.bo.app.asteroid_multiplayer_server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.bo.app.asteroid_multiplayer_server.exception.AlreadyExistException;

public class RoomManager {

    private HashMap<String, Room> roomSet = new HashMap<>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public final long timeout;

    public RoomManager(long timeout) {
        this.timeout = timeout;
        startTimeOutDaemon();
    }

    private void startTimeOutDaemon() {
        Thread th = new Thread("session timeout check") {

            @Override
            public void run() {
                ArrayList<Player> players;
                while (true) {
                    try {
                        sleep(timeout);
                        lock.readLock()
                            .lock();
                        for (Room room : roomSet.values()) {
                            players = room.getPlayersWithExceededTime();
                            for (Player player : players) {
                                room.removePlayer(player.getRoomPosition());
                            }
                        }
                        lock.readLock()
                            .unlock();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        th.setDaemon(true);
        th.start();
    }

    public Room getRoom(String roomName) {
        Room m;
        lock.readLock()
            .lock();
        m = roomSet.get(roomName);
        lock.readLock()
            .unlock();
        return m;
    }

    public void addRoom(Room room) throws AlreadyExistException {
        lock.writeLock()
            .lock();
        boolean ck = roomSet.containsKey(room.name);

        if (ck) {
            lock.writeLock()
                .unlock();
            throw new AlreadyExistException();
        }

        roomSet.put(room.name, room);
        lock.writeLock()
            .unlock();

        room.setRoomManager(this);
    }

    public void removeRoom(String roomName) {
        lock.writeLock()
            .lock();
        roomSet.remove(roomName);
        lock.writeLock()
            .unlock();
    }
}
