package edu.bo.app.asteroid_multiplayer_server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import edu.bo.app.asteroid_multiplayer_server.exception.AlreadyExistException;
import edu.bo.app.asteroid_multiplayer_server.exception.NoSpaceInRoomException;
import edu.bo.app.asteroid_multiplayer_server.game.GameArena;
import edu.bo.app.asteroid_multiplayer_server.game.GameObject;

public class Room {

    public final static int SIZE = 4;
    public final String name;

    private RoomManager roomManager;
    private HashMap<Integer, Player> players = new HashMap<>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private int hostPosition = 0;

    private volatile boolean isInGame = false;

    public SoundMixer soundMixer = null;

    private interface BroadcastedMessage {

        void execute(Player p) throws IOException;
    }

    public interface ExecuteForEveryPlayer {

        void execute(Player p);
    }

    public Room(String name) {
        this.name = name;
    }

    public boolean isInGame() {
        return isInGame;
    }

    public void setRoomManager(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    public int getHostPosition() {
        return hostPosition;
    }

    private int getFreePosition() {
        for (int i = 0; i < SIZE; i++) {
            if (!players.containsKey(i)) {
                return i;
            }
        }
        return -1;
    }

    public void forEveryPlayer(ExecuteForEveryPlayer e) {
        lock.readLock()
            .lock();
        for (Player p : players.values()) {
            e.execute(p);
        }
        lock.readLock()
            .unlock();
    }

    private void handleIOException(IOException e, Player player) {
        e.printStackTrace();
        // close player with connection error in other thread;
        Thread th = new Thread("player " + player.getName() + " close") {

            @Override
            public void run() {
                try {
                    player.closeConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
    }

    private void broadcastMessage(BroadcastedMessage bm) {
        lock.readLock()
            .lock();
        for (Player p : players.values()) {
            try {
                bm.execute(p);
            } catch (IOException e) {
                handleIOException(e, p);
            }
        }
        lock.readLock()
            .unlock();
    }

    private void addedPlayerMessage(DataOutputStream dos, int roomPosition, String playerName) throws IOException {
        dos.writeInt(1);
        dos.writeInt(roomPosition);
        dos.writeUTF(playerName);
        dos.flush();
    }

    private void acceptedMessage(DataOutputStream dos) throws IOException {
        dos.writeInt(10);
        dos.flush();
    }

    private void playerPositionMessage(DataOutputStream dos, int roomPosition) throws IOException {
        dos.writeInt(6);
        dos.writeInt(roomPosition);
        dos.flush();
    }

    private void sendCurrentStateToPlayer(Player player, int position) throws IOException {
        lock.readLock()
            .lock();
        for (Player p : players.values()) {
            addedPlayerMessage(player.getDataOutput(), p.getRoomPosition(), p.getName());
            if (p.isReady()) {
                readyMessage(player.getDataOutput(), p.getRoomPosition());
            }
        }
        lock.readLock()
            .unlock();
        playerPositionMessage(player.getDataOutput(), position);
        newHostMessage(player.getDataOutput(), hostPosition);
    }

    // take care of locking
    private boolean isPlayerOfGivenNameExist(String playerName) {
        boolean b = false;
        for (Player p : players.values()) {
            b = p.getName()
                 .equals(playerName);
            if (b) {
                break;
            }
        }
        return b;
    }

    // TO DO raguje gdy gracz o zadanym imieniu już istnieje
    public void addPlayer(Player player) throws NoSpaceInRoomException, IOException, AlreadyExistException {
        lock.writeLock()
            .lock();
        if (players.size() >= 4) {
            lock.writeLock()
                .unlock();
            throw new NoSpaceInRoomException();
        }
        if (players.size() >= 1 && isPlayerOfGivenNameExist(player.getName())) {
            lock.writeLock()
                .unlock();
            throw new AlreadyExistException();
        }
        int position = getFreePosition();

        players.put(position, player);
        lock.writeLock()
            .unlock();

        player.setRoomPosition(position);

        acceptedMessage(player.getDataOutput());

        sendCurrentStateToPlayer(player, position);

        propagateAddedPlayer(player);
    }

    private void propagateAddedPlayer(Player player) {
        broadcastMessage((p) -> {
            if (p.getRoomPosition() == player.getRoomPosition()) {
                return;
            }
            addedPlayerMessage(p.getDataOutput(), player.getRoomPosition(), player.getName());
        });
    }

    private void removedPlayerMessage(DataOutputStream dos, int roomPosition) throws IOException {
        dos.writeInt(2);
        dos.writeInt(roomPosition);
        dos.flush();
    }

    private void propagateRemovedPlayer(int removedPosition, Player player) {
        broadcastMessage((p) -> {
            removedPlayerMessage(p.getDataOutput(), removedPosition);
        });
        try {
            removedPlayerMessage(player.getDataOutput(), removedPosition);
        } catch (IOException e) {
            handleIOException(e, player);
        }
    }

    private void calcuateNextHostPosition() {
        for (int i = 0; i < SIZE; i++) {
            if (players.containsKey(i)) {
                hostPosition = i;
            }
        }
    }

    private void deleteThisRoom() {
        roomManager.removeRoom(name);
        if (soundMixer != null) {
            soundMixer.running = false;
        }
    }

    public void removePlayer(int position) {
        boolean isNewHost = false;
        lock.writeLock()
            .lock();
        Player player = players.get(position);
        // remove player
        players.remove(position);
        // if player is host and there are other players
        if (position == hostPosition && players.size() > 0) {
            calcuateNextHostPosition();
            isNewHost = true;
        }
        if (players.size() <= 0) {
            deleteThisRoom();
        }
        lock.writeLock()
            .unlock();

        propagateRemovedPlayer(position, player);
        if (isNewHost) {
            propagateNewHost();
        }
    }

    private void newHostMessage(DataOutputStream dos, int newHostPosition) throws IOException {
        dos.writeInt(5);
        dos.writeInt(newHostPosition);
        dos.flush();
    }

    private void propagateNewHost() {
        broadcastMessage((p) -> {
            newHostMessage(p.getDataOutput(), hostPosition);
        });
    }

    public ArrayList<Player> getPlayersWithExceededTime() {
        ArrayList<Player> playerList = new ArrayList<>();
        lock.readLock()
            .lock();
        for (Player p : players.values()) {
            if (p.lastMessageTime - System.currentTimeMillis() > roomManager.timeout) {
                playerList.add(p);
            }
        }
        lock.readLock()
            .unlock();
        return playerList;
    }

    private void readyMessage(DataOutputStream dos, int roomPosition) throws IOException {
        dos.writeInt(3);
        dos.writeInt(roomPosition);
        dos.flush();
    }

    public void propagatePlayerAsReady(int roomPosition) {
        broadcastMessage((p) -> {
            readyMessage(p.getDataOutput(), roomPosition);
        });
    }

    public Player getPlayer(String platerName) {
        Player player = null;
        lock.readLock()
            .lock();
        for (Player p : players.values()) {
            if (p.getName()
                 .equals(platerName)) {
                player = p;
                break;
            }
        }
        lock.readLock()
            .unlock();
        return player;
    }

    private void startGameMessage(DataOutputStream dos) throws IOException {
        dos.writeInt(4);
        dos.flush();
    }

    private void propagateStartGame() {
        broadcastMessage((p) -> {
            startGameMessage(p.getDataOutput());
        });
    }

    public void startGame() {
        ArrayList<Integer> notReadyPlayers = new ArrayList<>();
        // populate list of not ready players
        lock.readLock()
            .lock();
        for (Player p : players.values()) {
            if (!p.isReady()) {
                notReadyPlayers.add(p.getRoomPosition());
            }
        }
        lock.readLock()
            .unlock();
        // remove not ready players
        for (int i : notReadyPlayers) {
            removePlayer(i);
        }
        // start game procedure
        propagateStartGame();
        // create game arena
        createGameArena();
    }

    private void createGameArena() {
        try {
            new GameArena(this).start();
            isInGame = true;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void endFrameMessage(DataOutputStream dos) throws IOException {
        dos.writeInt(-9);
        dos.flush();
    }

    public void broadcastGame(Collection<GameObject> gameObjects, int counter) {
        SortedMap<Integer, Player> scores = createScoreBoard();
        lock.readLock()
            .lock();
        Thread th;
        for (Player p : players.values()) {
            th = new Thread() {

                @Override
                public void run() {
                    DataOutputStream dos = p.getDataOutput();
                    try {
                        for (GameObject go : gameObjects) {
                            go.sendMessages(dos);
                        }
                        counterMessage(dos, counter);
                        for (Player player : scores.values()) {
                            inGameScoreMessage(dos, player.getRoomPosition(), player.points);
                        }
                        endFrameMessage(dos);
                    } catch (IOException e) {
                        handleIOException(e, p);
                    }
                }
            };
            th.start();
        }
        lock.readLock()
            .unlock();
    }

    private void counterMessage(DataOutputStream dos, int i) throws IOException {
        dos.writeInt(-8);
        dos.writeInt(i);
        dos.flush();
    }

    private void endGameMessage(DataOutputStream dos) throws IOException {
        dos.writeInt(-10);
        dos.flush();
    }

    private void inGameScoreMessage(DataOutputStream dos, int position, int i) throws IOException {
        dos.writeInt(-11);
        dos.writeInt(position);
        dos.writeInt(i);
        dos.flush();
    }

    private void scoreMessage(DataOutputStream dos, String name, int position, int score) throws IOException {
        dos.writeInt(7);
        dos.writeUTF(name);
        dos.writeInt(position);
        dos.writeInt(score);
        dos.flush();
    }

    private SortedMap<Integer, Player> createScoreBoard() {
        SortedMap<Integer, Player> scores = new TreeMap<>();
        int position;
        lock.readLock()
            .lock();
        for (Player p : players.values()) {
            position = p.points * 5;
            while (scores.containsKey(position)) {
                position++;
            }
            scores.put(position, p);
        }
        lock.readLock()
            .unlock();
        return scores;
    }

    public void propagateEndGame() {
        isInGame = false;
        broadcastMessage((p) -> {
            endGameMessage(p.getDataOutput());
        });
        SortedMap<Integer, Player> scores = createScoreBoard();
        broadcastMessage((p) -> {
            for (Player player : scores.values()) {
                scoreMessage(p.getDataOutput(), player.getName(), player.getRoomPosition(), player.points);
            }
        });
    }
}
