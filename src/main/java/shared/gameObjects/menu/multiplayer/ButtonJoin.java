package shared.gameObjects.menu.multiplayer;

import java.util.UUID;
import shared.gameObjects.Utils.ObjectID;
import shared.gameObjects.menu.ButtonObject;

public class ButtonJoin extends ButtonObject {

  /**
   * Base class used to create an object in game. This is used on both the client and server side to
   * ensure actions are calculated the same
   *
   * @param x X coordinate of object in game world
   * @param y Y coordinate of object in game world
   * @param id Unique Identifier of every game object
   */
  public ButtonJoin(double x, double y, ObjectID id, UUID objectUUID) {
    super(x, y, id, "images/buttons/multiplayer_unpressed.png", objectUUID,
        "images/buttons/multiplayer_pressed.png");
  }
}
