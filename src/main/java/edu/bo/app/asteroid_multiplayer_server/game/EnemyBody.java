package edu.bo.app.asteroid_multiplayer_server.game;

public class EnemyBody extends EnemyObject {

    public final int points;

    public EnemyBody(GameArena gameArena, double width, double height, int points) {
        super(gameArena, width, height);
        this.points = points;
    }

}
