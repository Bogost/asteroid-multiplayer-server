package edu.bo.app.asteroid_multiplayer_server;

import java.io.IOException;
import java.net.ServerSocket;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import edu.bo.app.asteroid_multiplayer_common.XmlLoader;

public class PlayerTcpService {

    public RoomManager gameRooms;
    public RoomManager gameChatRooms;

    private int gamePort;
    private int chatPort;

    private ServerSocket gameSocket;
    private ServerSocket chatSocket;

    public PlayerTcpService() throws ParserConfigurationException, SAXException, IOException {
        XmlLoader xmlLoader = new XmlLoader(Config.CONFIG_PATH);
        gamePort = Integer.parseInt(xmlLoader.getValue("root", "networking", "ports", "game"));
        chatPort = Integer.parseInt(xmlLoader.getValue("root", "networking", "ports", "chat"));
        gameSocket = new ServerSocket(gamePort);
        chatSocket = new ServerSocket(chatPort);
        long timeout = Long.parseLong(xmlLoader.getValue("root", "networking", "timeout"));
        timeout = timeout * 60 * 1000;
        gameRooms = new RoomManager(timeout);
        gameChatRooms = new RoomManager(timeout);
    }

    public void start() {
        PlayerTcpService pts = this;

        Thread th1 = new Thread("player listener") {

            @Override
            public void run() {
                Player p;
                while (true) {
                    try {
                        p = new Player(gameSocket.accept(), pts);
                        p.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        th1.start();

        Thread th2 = new Thread("chatter listener") {

            @Override
            public void run() {
                Chatter p;
                while (true) {
                    try {
                        p = new Chatter(chatSocket.accept(), pts);
                        p.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        th2.start();
    }

}
