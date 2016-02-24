/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package world;

import com.jme3.asset.TextureKey;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.ChaseCamera;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import world.controls.DestroyableControl;
import world.controls.ObstacleControl;
import world.controls.PlayerControl;

/**
 * Creates GameObjects like Players or Obstacles.
 *
 * @author Marco Klein
 */
public class GameObjectFactory {
    
    private World world;

    public GameObjectFactory(World world) {
        this.world = world;
    }
    
    /**
     * Creates a new Player by adding a PlayerControl and a player model
     * to the returned Spatial.
     * 
     * Additionally player properties are defined.
     * 
     * @return 
     */
    public Spatial createPlayer(Vector3f startLocation, ColorRGBA color) {
        Node player = new Node();
        // move player to start location
        player.setLocalTranslation(startLocation);
        // add physics to player
        BetterCharacterControl characterControl = new BetterCharacterControl(0.8f, 2f, 1f);
        characterControl.setJumpForce(new Vector3f(0, 10f, 0));
        player.addControl(characterControl);
        player.addControl(new PlayerControl(world, 300));
        
        // set up geometry
        Geometry geometry = ModelFactory.createSphere(world.getApp().getAssetManager(), 1, color);
        // move geometry in center of player
        geometry.setLocalTranslation(0, 1f, 0);
        geometry.setName("PlayerGeometry");
        player.attachChild(geometry);
        
        // attach a camera
        
//        CameraNode camNode = new CameraNode("Player Camera", world.getApp().getCamera());
//        camNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
//        player.attachChild(camNode);
//        camNode.setLocalTranslation(0, 5, -5);
//        camNode.lookAt(player.getLocalTranslation(), Vector3f.UNIT_Y);
        new ChaseCamera(world.getApp().getCamera(), geometry, world.getApp().getInputManager());
        return player;
    }
    
    /**
     * Creates a new Obstacle using the given geometry.
     * Use ModelFactory.create* to create a geometry.
     * 
     * @param geometry
     * @return 
     */
    public Spatial createObstacle(Geometry geometry, float initialSpeed, Vector3f startTargetLocation) {
        Node obstacle = new Node();
        obstacle.attachChild(geometry);
        obstacle.addControl(new ObstacleControl(initialSpeed, startTargetLocation));
        obstacle.addControl(new DestroyableControl());
        RigidBodyControl rigidControl = new RigidBodyControl(1);
        obstacle.addControl(rigidControl);
        rigidControl.setKinematic(true);
        
        // add an obstacle material
        Material mat = new Material(world.getApp().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key = new TextureKey("Textures/Terrain/BrickWall/BrickWall.jpg");
        key.setGenerateMips(true);
        Texture tex  = world.getApp().getAssetManager().loadTexture(key);
        mat.setTexture("ColorMap", tex);
        
        geometry.setMaterial(mat);
        return obstacle;
    }
    
    
    
    
}
