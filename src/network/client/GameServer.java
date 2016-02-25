package network.client;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.network.ConnectionListener;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import network.NetworkAppState;
import network.NetworkSerializer;
import network.message.IdentificationMessage;
import network.message.NewPlayerMessage;
import network.message.SetPlayerMessage;
import network.message.world.AddGameObjectMessage;
import network.message.world.InitWorldMessage;
import network.message.world.RemoveGameObjectMessage;
import network.message.world.UpdateGameObjectPositionMessage;
import network.message.world.UpdateLogicMessage;
import network.message.world.WorldMessage;
import world.World;
import world.WorldListener;
import world.control.LogicControl;
import world.control.ModelControl;
import world.gameobject.logic.ObstacleLogic;
import world.gameobject.logic.PlayerLogic;
import world.gameobject.logic.StaticPhysicsLogic;
import world.gameobject.model.GroundModel;
import world.gameobject.model.ObstacleModel;
import world.gameobject.model.PlayerModel;

/**
 * Holds clients which will be handled.
 * 
 * Player names are stored in the HostedConnection with the key "PlayerName"
 *
 * @author Marco Klein
 */
public class GameServer extends NetworkAppState implements MessageListener<HostedConnection>, ConnectionListener, WorldListener {
    private static final Logger LOG = Logger.getLogger(GameServer.class.getName());
    public static final String NAME = "TagMe";
    public static final int VERSION = 1;
    public static final int TCP_PORT = 5110;
    public static final int UDP_PORT = 5111;
    
    private Application app;
    private Server server;
    
    private Random random = new Random();
    
    private ArrayList<HostedConnection> identifiedConnections = new ArrayList<>();

    public GameServer(World world) {
        super(world);
        NetworkSerializer.registerClasses();
    }

    @Override
    public void stateAttached(AppStateManager stateManager) {
        app = stateManager.getApplication();
        // initialize
        try {
            server = Network.createServer(NAME, VERSION, TCP_PORT, UDP_PORT);
        } catch (IOException ex) {
            Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        server.start();
        
        // add listeners
        server.addMessageListener(this);
        server.addConnectionListener(this);
        LOG.log(Level.INFO, "Server created and started on port {0}", TCP_PORT);
        
        // TODO add listener again when world gets reset
        world.addListener(this);
        
        // initialize world
        Node ground = new Node();
        ground.addControl(new ModelControl(world, new GroundModel()));
        ground.addControl(new LogicControl(world, new StaticPhysicsLogic()));
        world.addGameObject(ground);
        
        Node obstacle = new Node();
        obstacle.addControl(new ModelControl(world, new ObstacleModel(new Vector3f(20, 20, 20))));
        obstacle.addControl(new LogicControl(world, new ObstacleLogic(new Vector3f(20, 20, 20), 0, new Vector3f(20, 20, 20))));
        world.addGameObject(obstacle);
        
        
        initObstacles();
        LOG.info("World initialized.");
    }
    
    private void initObstacles() {
        Vector3f worldSize = new Vector3f(200, 40, 200);
        for (int i = 0; i < 200; i++) {
            Vector3f obstacleSize = new Vector3f();
            obstacleSize.x = (float) (random.nextInt(1000) + 10) / 100f;
            obstacleSize.y = (float) (random.nextInt(400) + 10) / 100f;
            obstacleSize.z = (float) (random.nextInt(1000) + 10) / 100f;
            
            Vector3f targetPosition = new Vector3f();
            // get a random position
            targetPosition.x = (float) random.nextInt((int) (worldSize.x * 1000)) / 1000f;
            targetPosition.y = (float) random.nextInt((int) (worldSize.y * 1000)) / 1000f;
            targetPosition.z = (float) random.nextInt((int) (worldSize.z * 1000)) / 1000f;
            
            Node obstacle = new Node();
            obstacle.addControl(new ModelControl(world, new ObstacleModel(obstacleSize)));
            obstacle.addControl(new LogicControl(world, new ObstacleLogic(targetPosition, 0, targetPosition)));
            
            
            world.addGameObject(obstacle);
        }
    }

    @Override
    public void stateDetached(AppStateManager stateManager) {
        server.removeMessageListener(this);
        // cleanup
        server.close();
    }

    @Override
    public void messageReceived(final HostedConnection source, final Message m) {
        if (m instanceof UpdateGameObjectPositionMessage) {
            // broadcast location updates
            // TODO test if client is allowed to update object
            server.broadcast(Filters.notEqualTo(source), m);
        } if (m instanceof IdentificationMessage) {
            final IdentificationMessage identification = (IdentificationMessage) m;
            // TODO check identification
            LOG.log(Level.INFO, "{0} has connected.", identification.getPlayerName());
            source.setAttribute("PlayerName", identification.getPlayerName());
            source.setAttribute("Identified", true);
            
            // identification accepted - let player join the game
            app.enqueue(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    
                    // prepare the init world message
                    InitWorldMessage message = new InitWorldMessage(world.getWorldSize(), world.getGameObjects());
                    LOG.info("Sending " + world.getGameObjects().length + " game objects.");
                    source.send(message);
                    
                    
                    // add a player model to the client
                    Node playerNode = new Node();
                    playerNode.addControl(new ModelControl(world, new PlayerModel(ColorRGBA.White)));
                    int id = world.addGameObject(playerNode);
                    
                    //int id = world.generateGameObjectId();
                    source.setAttribute("PlayerId", id);
                    
                    // let user know who his player is
//                    source.send(new UpdateModelMessage(new PlayerModel(ColorRGBA.White), id));
                    UpdateLogicMessage playerMsg = new UpdateLogicMessage(new PlayerLogic(new Vector3f(0, 50, 0)), id);
                    source.send(playerMsg);
                    source.send(new SetPlayerMessage(id));
                    
                    // inform other players about player
                    server.broadcast(Filters.notEqualTo(source), new NewPlayerMessage(identification.getPlayerName(), id));
                    LOG.info("Player joined game.");
                    
                    identifiedConnections.add(source);
                    return null;
                }
                
            });
        } else if (source.getAttribute("Identified")) {
            // only proceed message if hosted connection is identified
            if (m instanceof WorldMessage) {
                ((WorldMessage) m).applyToWorld(world);
            }
        }
    }

    @Override
    public void connectionAdded(Server server, HostedConnection conn) {
    }

    @Override
    public void connectionRemoved(Server server, HostedConnection conn) {
        LOG.info("Client disconnected.");
        // remove player
        if (identifiedConnections.contains(conn)) {
            identifiedConnections.remove(conn);
            world.removeGameObject((int) conn.getAttribute("PlayerId"));
        }
    }

    @Override
    public void gameObjectAdded(Spatial gameObject) {
        LOG.info("Adding GameObject to world.");
        // inform clients
        server.broadcast(new AddGameObjectMessage(gameObject));
    }

    @Override
    public void gameObjectRemoved(Spatial gameObject) {
        LOG.info("Removing GameObject from world.");
        server.broadcast(new RemoveGameObjectMessage((int) gameObject.getUserData("Id")));
    }

    
    
}
