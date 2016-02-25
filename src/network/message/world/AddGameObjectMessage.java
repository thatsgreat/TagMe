package network.message.world;

import com.jme3.network.serializing.Serializable;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import world.World;
import world.control.LogicControl;
import world.control.ModelControl;
import world.gameobject.logic.Logic;
import world.gameobject.model.Model;

/**
 *
 * @author Marco Klein
 */
@Serializable
public class AddGameObjectMessage extends GameObjectMessage {
    
    protected Logic logic;
    protected Model model;

    public AddGameObjectMessage() {
    }

    public AddGameObjectMessage(Logic logic, Model model, int id) {
        super(id);
        this.logic = logic;
        this.model = model;
    }

    @Override
    public void applyToGameObject(World world, Spatial gameObject) {
        gameObject = new Node("GameObject");
        gameObject.addControl(new ModelControl(world, model));
        gameObject.addControl(new LogicControl(world, logic));
        world.addGameObject(gameObject, id);

    }
    
}
