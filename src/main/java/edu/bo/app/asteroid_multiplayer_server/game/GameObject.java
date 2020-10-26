package edu.bo.app.asteroid_multiplayer_server.game;

import java.awt.Rectangle;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class GameObject {

    protected volatile double x1, y1, x2, y2, vx, vy;
    protected double height, width;

    protected int cloneX;
    protected int cloneY;

    protected GameArena gameArena;

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }

    public GameObject(GameArena gameArena, double width, double height) {
        this.height = height;
        this.width = width;
        this.gameArena = gameArena;
    }

    protected void addingVelocitisToCoordinates() {
        x1 += vx;
        x2 += vx;
        y1 += vy;
        y2 += vy;
    }

    protected void cloneCalculation() {
        if (x1 < 0) {
            if (x2 < 0) {
                x1 += gameArena.getWidth();
                x2 += gameArena.getWidth();
                cloneX = 0;
            } else {
                cloneX = 1;
            }
        } else if (x2 > gameArena.getWidth()) {
            if (x1 > gameArena.getWidth()) {
                x1 -= gameArena.getWidth();
                x2 -= gameArena.getWidth();
                cloneX = 0;
            } else {
                cloneX = -1;
            }
        } else {
            cloneX = 0;
        }

        if (y1 < 0) {
            if (y2 < 0) {
                y1 += gameArena.getHeight();
                y2 += gameArena.getHeight();
                cloneY = 0;
            } else {
                cloneY = 1;
            }
        } else if (y2 > gameArena.getHeight()) {
            if (y1 > gameArena.getHeight()) {
                y1 -= gameArena.getHeight();
                y2 -= gameArena.getHeight();
                cloneY = 0;
            } else {
                cloneY = -1;
            }
        } else {
            cloneY = 0;
        }
    }

    public void calculatePosition() {
        addingVelocitisToCoordinates();
        cloneCalculation();
    }

    public double getX() {
        return x1;
    }

    public double getY() {
        return y1;
    }

    public double getX2() {
        return x2;
    }

    public double getY1() {
        return y2;
    }

    public double getVx() {
        return vx;
    }

    public void setVx(double vx) {
        this.vx = vx;
    }

    public double getVy() {
        return vy;
    }

    public void setVy(double vy) {
        this.vy = vy;
    }

    public void place(double x, double y) {
        x1 = x;
        x2 = x1 + width;
        y1 = y;
        y2 = y1 + height;
    }

    private static void populateCloneQueue(Queue<Rectangle> cloneQueue, GameObject go) {
        Rectangle r = new Rectangle((int) go.x1, (int) go.y1, (int) go.height, (int) go.width);
        cloneQueue.add(r);

        double x, y;
        if (go.cloneX != 0) {
            x = go.x1 + go.cloneX * go.gameArena.getWidth();
            y = go.y1;
            r = new Rectangle((int) x, (int) y, (int) go.height, (int) go.width);
            cloneQueue.add(r);
        }
        if (go.cloneY != 0) {
            x = go.x1;
            y = go.y1 + go.cloneY * go.gameArena.getHeight();
            r = new Rectangle((int) x, (int) y, (int) go.height, (int) go.width);
            cloneQueue.add(r);
        }
        if (go.cloneX != 0 && go.cloneY != 0) {
            x = go.x1 + go.cloneX * go.gameArena.getWidth();
            y = go.y1 + go.cloneY * go.gameArena.getHeight();
            r = new Rectangle((int) x, (int) y, (int) go.height, (int) go.width);
            cloneQueue.add(r);
        }
    }

    public boolean detectCollision(GameObject go) {
        Queue<Rectangle> thisClones = new LinkedList<>();
        Queue<Rectangle> goClones = new LinkedList<>();
        GameObject.populateCloneQueue(thisClones, this);
        GameObject.populateCloneQueue(goClones, go);

        for (Rectangle r1 : thisClones) {
            for (Rectangle r2 : goClones) {
                if (r1.contains(r2)) {
                    collision(go);
                    return true;
                }
            }
        }
        return false;
    }

    public void collision(GameObject go) {};

    public void destroy() {

    }

    protected void message(DataOutputStream dos, double x, double y) throws IOException {

    }

    public void sendMessages(DataOutputStream dos) throws IOException {
        message(dos, x1, y1);
        if (cloneX != 0) {
            message(dos, x1 + cloneX * gameArena.getWidth(), y1);
        }
        if (cloneY != 0) {
            message(dos, x1, y1 + cloneY * gameArena.getHeight());
        }
        if (cloneX != 0 && cloneY != 0) {
            message(dos, x1 + cloneX * gameArena.getWidth(), y1 + cloneY * gameArena.getHeight());
        }
    }

    protected double degreeToRadian(double degree) {
        return degree * Math.PI / 180;
    }

}
