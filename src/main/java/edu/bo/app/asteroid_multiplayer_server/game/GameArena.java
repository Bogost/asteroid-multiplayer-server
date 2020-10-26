package edu.bo.app.asteroid_multiplayer_server.game;

import java.io.IOException;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import edu.bo.app.asteroid_multiplayer_common.ListManager;
import edu.bo.app.asteroid_multiplayer_common.XmlLoader;
import edu.bo.app.asteroid_multiplayer_server.Config;
import edu.bo.app.asteroid_multiplayer_server.Room;
import javafx.geometry.Point2D;

public class GameArena {

    private long gameTime;

    private double height;
    private double width;
    private long fps;

    private Room room;

    private ListManager<GameObject> gameObjectsManager = new ListManager<>();

    private Point2D[] shipsPosition = new Point2D[4];

    private Random random = new Random();

    public GameArena(Room room) throws ParserConfigurationException, SAXException, IOException {
        XmlLoader xmlLoader = new XmlLoader(Config.CONFIG_PATH);
        height = Double.parseDouble(xmlLoader.getValue("root", "game", "space", "height"));
        width = Double.parseDouble(xmlLoader.getValue("root", "game", "space", "width"));
        fps = Long.parseLong(xmlLoader.getValue("root", "game", "fps"));

        gameTime = 60 * fps;

        this.room = room;

        setStartShipsPosition();
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }

    public long getFps() {
        return fps;
    }

    private void setStartShipsPosition() {
        shipsPosition[0] = new Point2D(100, 100);
        shipsPosition[1] = new Point2D(100, height - 100);
        shipsPosition[2] = new Point2D(width - 100, 100);
        shipsPosition[3] = new Point2D(width - 100, height - 100);
    }

    private void createPlayerShips() {
        room.forEveryPlayer((p) -> {
            PlayerShip ps = new PlayerShip(p, this, shipsPosition[p.getRoomPosition()]);
            gameObjectsManager.add(ps);
            p.ship = ps;
        });
    }

    public void addGameObject(GameObject go) {
        gameObjectsManager.add(go);
    }

    public void removeGameObject(GameObject go) {
        gameObjectsManager.remove(go);
    }

    private void addRandomEnemies() {
        if (random.nextInt() % fps == 0) {
            EnemyObject eo;

            if (random.nextInt() % 2 == 0) {
                eo = new BigUfo(this);
            } else {
                eo = new SmallUfo(this);
            }

            positionEnemy(eo);
        }
    }

    private void positionEnemy(EnemyObject eo) {
        double beyondArena = 100;

        double perimeter = height * 2 + width * 2;
        double perimeterPosition = random.nextInt() % (int) perimeter;
        double x, y;
        int place;
        // perimeterPositio to vector
        if (perimeterPosition < height) {
            x = 0;
            y = perimeterPosition;
            place = 0;
        } else if (perimeterPosition < height + width) {
            x = perimeterPosition - height;
            y = height;
            place = 1;
        } else if (perimeterPosition < height * 2 + width) {
            x = width;
            y = perimeterPosition - (height + width);
            place = 2;
        } else {
            x = perimeterPosition - (height * 2 + width);
            y = 0;
            place = 3;
        }

        // set velocity vector direction
        double direction = random.nextInt() % 180;
        direction = direction + (place - 1) * 90;
        direction %= 360;

        //
    }

    public void start() {
        Thread th = new Thread() {

            @Override
            public void run() {
                createPlayerShips();
                System.out.println("create ships");

                long timeStamp;
                long timeDelay = 0;
                long frameTime = 1000 / fps;

                for (long i = 0; i < gameTime; i++) {
                    timeStamp = System.currentTimeMillis();
                    // -----------------------------------------------------------------
                    // iterate through players
                    for (GameObject go : gameObjectsManager.getList()) {
                        go.calculatePosition();
                    }
                    // detect collisions
                    for (GameObject go1 : gameObjectsManager.getList()) {
                        for (GameObject go2 : gameObjectsManager.getList()) {
                            if (go1.detectCollision(go2)) {
                                CollisionHandler.handleCollisions(go1, go2);
                            }
                        }
                    }

                    addRandomEnemies();

                    gameObjectsManager.conductAdding();
                    gameObjectsManager.conductRemoving();
                    // -----------------------------------------------------------------
                    timeDelay += frameTime - (System.currentTimeMillis() - timeStamp);
                    // broadcast game objects
                    room.broadcastGame(gameObjectsManager.getList(), (int) ((gameTime - i) / fps));
                    // skip frame
                    if (timeDelay < 0) {
                        System.out.println(timeDelay);
                        continue;
                    }
                    try {
                        sleep(timeDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    timeDelay = 0;
                }
                room.propagateEndGame();
            }
        };
        th.start();
    }

}
