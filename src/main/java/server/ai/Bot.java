package server.ai;

import shared.gameObjects.Utils.ObjectID;
import shared.gameObjects.players.Player;

/** @author Harry Levick (hxl799) */
public class Bot extends Player {

  boolean jumpKey, leftKey, rightKey, click;
  double mouseX, mouseY;

  public Bot(double x, double y, ObjectID id) {
    super(x, y, id);

  }

  /**
   * Receives an action and then executes this action.
   */
  private void executeAction() {
    // TODO decide on the implementation of action execution.
  }

}
