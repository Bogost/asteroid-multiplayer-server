package edu.bo.app.asteroid_multiplayer_server.game;

import java.io.DataOutputStream;
import java.io.IOException;

public class PlayerBullet extends PlayerObject {

    private static double VELOCITY = 6;
    private static int LASTING = 90;

    public PlayerShip ship;
    private int lifetime = PlayerBullet.LASTING;

    public PlayerBullet(PlayerShip ship) {
        super(ship.player, ship.gameArena, 5, 5);
        this.ship = ship;
        setBulletVelocity();
        setBulletPosition();
    }

    private void setBulletPosition() {
        double x = ship.x1 + ship.width / 2;
        double y = ship.y1 + ship.height / 2;
        x += vx * (ship.width / (2 * PlayerBullet.VELOCITY));
        y += vy * (ship.width / (2 * PlayerBullet.VELOCITY));
        place(x, y);
    }

    private void setBulletVelocity() {
        vx = Math.sin(degreeToRadian(ship.rotation)) * PlayerBullet.VELOCITY;
        vy = -1 * Math.cos(degreeToRadian(ship.rotation)) * PlayerBullet.VELOCITY;
    }

    @Override
    public void destroy() {
        ship.nrOfCurrentBullets--;
        gameArena.removeGameObject(this);
    }

    @Override
    public void calculatePosition() {
        addingVelocitisToCoordinates();
        cloneCalculation();
        lifetime--;
        if (lifetime <= 0) {
            destroy();
        }
    }

    @Override
    protected void message(DataOutputStream dos, double x, double y) throws IOException {
        dos.writeInt(-2);
        dos.writeInt(player.getRoomPosition());
        dos.writeDouble(x);
        dos.writeDouble(y);
        dos.flush();
    }

}
