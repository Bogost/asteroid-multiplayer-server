package edu.bo.app.asteroid_multiplayer_server;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class AsteroidMultiplayerServer {

    public static void main(String[] args) {
        PlayerTcpService pts;
        try {
            pts = new PlayerTcpService();
            pts.start();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
