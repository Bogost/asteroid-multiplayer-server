package edu.bo.app.asteroid_multiplayer_server.game;

import java.io.DataOutputStream;
import java.io.IOException;

public class BigUfo extends Ufo {

    public BigUfo(GameArena gameArena) {
        super(gameArena, 40, 80, 50);
        velocity = 10;
    }

    @Override
    protected void message(DataOutputStream dos, double x, double y) throws IOException {
        dos.writeInt(-6);
        dos.writeDouble(x);
        dos.writeDouble(y);
        dos.flush();
    }

}
