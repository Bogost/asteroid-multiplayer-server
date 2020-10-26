package edu.bo.app.asteroid_multiplayer_server.game;

public class EnemyBullet extends EnemyObject {

    public EnemyBody owner;

    public EnemyBullet(GameArena gameArena, double width, double height, EnemyBody owner) {
        super(gameArena, width, height);
        this.owner = owner;
    }

}
