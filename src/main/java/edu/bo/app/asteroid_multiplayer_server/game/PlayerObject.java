package edu.bo.app.asteroid_multiplayer_server.game;

import edu.bo.app.asteroid_multiplayer_server.Player;

public class PlayerObject extends GameObject {

    public final Player player;

    public PlayerObject(Player player, GameArena gameArena, double width, double height) {
        super(gameArena, width, height);
        this.player = player;
    }

}
