package edu.bo.app.asteroid_multiplayer_server.game;

public class EnemyObject extends GameObject {

    protected float velocity;

    public EnemyObject(GameArena gameArena, double width, double height) {
        super(gameArena, width, height);
        // TODO Auto-generated constructor stub
    }

    public float getVelocity() {
        return velocity * gameArena.getFps();
    }
}
