package edu.bo.app.asteroid_multiplayer_server.game;

import java.io.DataOutputStream;
import java.io.IOException;

public class SmallUfo extends Ufo {

    public SmallUfo(GameArena gameArena) {
        super(gameArena, 20, 40, 100);
        velocity = 20;
    }

    @Override
    protected void message(DataOutputStream dos, double x, double y) throws IOException {
        dos.writeInt(-7);
        dos.writeDouble(x);
        dos.writeDouble(y);
        dos.flush();
    }

}
