package network.gamemode;

import com.jme3.network.Server;
import world.GameObjectControl;

/**
 * Marco Klein
 *
 * @author Marco Klein
 */
public abstract class GameMode {
    
    protected Server server;
    
    protected GameModeManager manager;

    public GameMode(GameModeManager manager) {
        this.manager = manager;
        this.server = manager.getServer();
    }

    public abstract void playerJoined(GameObjectControl player);
    public abstract void playerLeft(GameObjectControl player);
    
    public abstract void playerCollision(GameObjectControl playerA, GameObjectControl playerB);
    
    
}
