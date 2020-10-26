package edu.bo.app.asteroid_multiplayer_server.game;

import edu.bo.app.asteroid_multiplayer_server.Player;

public class CollisionHandler {

    private static class CollisionHandlerState {

        int playerShip = 0;
        int enemyObject = 0;
        int playerBullet = 0;
        int enemyBullet = 0;

        Player player = null;
        EnemyBody enemy = null;

        boolean same = false;

        private void addGameObjectToHandlerState(GameObject go) {
            if (go instanceof PlayerObject) {
                Player p = ((PlayerObject) go).player;
                if (p == player) {
                    same = true;
                } else {
                    player = p;
                }

                if (go instanceof PlayerShip) {
                    playerShip++;
                }
                if (go instanceof PlayerBullet) {
                    playerBullet++;
                }
            } else {
                if (go instanceof EnemyObject) {
                    if (go instanceof EnemyBody) {
                        enemy = (EnemyBody) go;
                    }
                    enemyObject++;
                }
                if (go instanceof EnemyBullet) {
                    EnemyBody eb = ((EnemyBullet) go).owner;
                    if (eb == enemy) {
                        same = true;
                    } else {
                        enemy = eb;
                    }

                    enemyBullet++;
                }
            }
        }

        private CollisionHandlerState(GameObject go1, GameObject go2) {
            addGameObjectToHandlerState(go1);
            addGameObjectToHandlerState(go2);
        }
    }

    public static void handleCollisions(GameObject go1, GameObject go2) {
        CollisionHandlerState state = new CollisionHandlerState(go1, go2);

        if (state.playerShip == 1 && state.enemyObject == 1) {
            state.player.points -= 100;

            go1.destroy();
            go2.destroy();
        } else if (state.playerShip == 2) {
            // momentum caluculation
        } else if (state.playerBullet == 1 && state.enemyObject == 1 && state.enemyBullet == 0) {
            state.player.points += state.enemy.points;

            go1.destroy();
            go2.destroy();
        } else if (state.playerShip == 1 && state.playerBullet == 1 && !state.same) {
            // momentum caluculation
        } else if (state.enemyBullet == 1 && state.playerBullet == 0 && !state.same) {
            // destroy both except bullet owner
            go1.destroy();
            go2.destroy();
        }
    }
}
