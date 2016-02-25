package network;

import com.jme3.network.serializing.Serializer;
import network.message.IdentificationMessage;
import network.message.NewPlayerMessage;
import network.message.SetPlayerMessage;
import network.message.world.AddGameObjectMessage;
import network.message.world.InitWorldMessage;
import network.message.world.UpdateLogicMessage;
import network.message.world.UpdateModelMessage;
import world.gameobject.logic.PlayerLogic;
import world.gameobject.model.PlayerModel;

/**
 * Used to serialize all Messages which will be used for server-client communication.
 *
 * @author Marco Klein
 */
public class NetworkSerializer {
    public static void registerClasses() {
        Serializer.registerClass(IdentificationMessage.class);
        
        Serializer.registerClass(InitWorldMessage.class);
        Serializer.registerClass(SetPlayerMessage.class);
        Serializer.registerClass(NewPlayerMessage.class);
        Serializer.registerClass(AddGameObjectMessage.class);
        
        
        Serializer.registerClass(UpdateLogicMessage.class);
        Serializer.registerClass(UpdateModelMessage.class);
        
        
        // model
        Serializer.registerClass(PlayerModel.class);
        
        
        // logic
        Serializer.registerClass(PlayerLogic.class);
        
        
    }
    
}
