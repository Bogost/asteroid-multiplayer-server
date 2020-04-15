package edu.bo.app.asteroid_multiplayer_server.game;

import java.io.DataOutputStream;
import java.io.IOException;

import edu.bo.app.asteroid_multiplayer_server.Player;
import javafx.geometry.Point2D;

public class PlayerShip extends PlayerObject {

    private static final double ACCELERATION = 1;
    private static final double DUMPING_RATIO = 0.02;
    private static final double ROTATION_SPEED = 5;
    private static final int FIRE_RATE = 10;
    private static final int MAX_NR_OF_BULLETS = 3;

    public int nrOfCurrentBullets;
    public volatile double rotation = 0;// clock wise, 12 = 0, 6 = 180/-180
    private volatile boolean lock = false;

    private volatile boolean bulletLock = false;
    private int bulletTimer = 0;

    public PlayerShip(Player player, GameArena gameArena, Point2D position) {
        super(player, gameArena, 30, 20);
        place(position.getX(), position.getY());
    }

    @Override
    protected void message(DataOutputStream dos, double x, double y) throws IOException {
        dos.writeInt(-1);
        dos.writeInt(player.getRoomPosition());
        dos.writeDouble(x);
        dos.writeDouble(y);
        dos.writeDouble(rotation);
        dos.flush();
    }

    public void accelerate() {
        if (lock) {
            return;
        }
        lock = true;
        double directionX = Math.sin(degreeToRadian(rotation)) * PlayerShip.ACCELERATION;
        double directionY = -1 * Math.cos(degreeToRadian(rotation)) * PlayerShip.ACCELERATION;
        vx += directionX;
        vy += directionY;
    }

    public void decelerate() {
        if (lock) {
            return;
        }
        lock = true;
        double directionX = Math.sin(degreeToRadian(rotation)) * PlayerShip.ACCELERATION;
        double directionY = -1 * Math.cos(degreeToRadian(rotation)) * PlayerShip.ACCELERATION;
        vx -= directionX;
        vy -= directionY;
    }

    private void dumping() {
        vx -= vx * PlayerShip.DUMPING_RATIO;
        vy -= vy * PlayerShip.DUMPING_RATIO;
    }

    public void rotateRight() {
        if (lock) {
            return;
        }
        lock = true;
        rotation -= PlayerShip.ROTATION_SPEED;
    }

    public void rotateLeft() {
        if (lock) {
            return;
        }
        lock = true;
        rotation += PlayerShip.ROTATION_SPEED;
    }

    @Override
    public void calculatePosition() {
        addingVelocitisToCoordinates();
        cloneCalculation();
        dumping();

        lock = false;
        if (bulletLock) {
            bulletTimer--;
            if (bulletTimer <= 0) {
                bulletLock = false;
            }
        }
    }

    public void fireBullet() {
        if (bulletLock || nrOfCurrentBullets >= PlayerShip.MAX_NR_OF_BULLETS) {
            return;
        }
        bulletLock = true;
        nrOfCurrentBullets++;
        bulletTimer = PlayerShip.FIRE_RATE;
        PlayerBullet pb = new PlayerBullet(this);
        gameArena.addGameObject(pb);
    }
}
