package edu.bo.app.asteroid_multiplayer_server.game;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import edu.bo.app.asteroid_multiplayer_server.Room;

// change to junit5
public class GameArenaTest {

    @Test
    public void addsObjectToItself() {
        Room mockRoom = Mockito.mock(Room.class);

        try {
            GameArena testArena = new GameArena(mockRoom);
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
