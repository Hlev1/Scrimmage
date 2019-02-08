package server.ai;

import server.ai.pathFind.AStar;
import shared.gameObjects.GameObject;
import shared.gameObjects.players.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import shared.util.maths.Vector2;

/** @author Harry Levick (hxl799) */

/**
 * AiAgent is the main body of an ai, creating an AiAgent will create a bot in the world at (x,y).
 * The AiAgent class then has the main loop of the bot, which is inside the startAgent() method,
 * calling this method begins the main loop. Before the startAgent() method is called, you must call
 * the getBot() method so that the bot can be added to the list of gameObjects.
 */
public class AiAgent {

  Bot bot;
  FSA state;
  boolean active;
  ArrayList<GameObject> gameObjects;
  Player targetPlayer;
  AStar pathFinder;

  public AiAgent(Bot b, ArrayList<GameObject> gameObjects) {
    this.bot = b;
    this.state = FSA.INITIAL_STATE;
    this.active = false;
    this.gameObjects = gameObjects;
    this.pathFinder = new AStar(gameObjects, this.bot);
  }

  /** The method that runs the agent. */
  public void startAgent() {
    active = true;
    double prevDist, newDist;
    /**
     * Would I need to fetch all players on each loop, or would a single fetch outside of the loop
     * give a reference to all players that updates with the player updates?
     */
    // Collect all players from the world
    List<Player> allPlayers =
        gameObjects.stream()
            .filter(p -> p instanceof Player)
            .map(Player.class::cast)
            .collect(Collectors.toList());

    // Update the targeted player
    targetPlayer = findTarget(allPlayers);

    while (active) {
      /**
       * The ai can be in one of 6 states at any one time. The state it is in determines the actions
       * that it takes.
       */
      switch (state) {
        case IDLE:
          // TODO what to do in the idle state?
          executeAction(new boolean[] {false, false, false, false, false});
        case CHASING:
          // Find the next best move to take, and execute this move.
          executeAction(pathFinder.optimise(targetPlayer));
          // TODO calculate and execute the best path to the target.
        case FLEEING:
          // TODO calculate and execute the best path away from the target.
        case ATTACKING:
          // TODO think about how an attacking script would work.
        case CHASING_ATTACKING:
          // TODO calculate and execute the best path to the target whilst attacking.
        case FLEEING_ATTACKING:
          // TODO calculate and execute the best path away from the target whilst attacking.
          /**
           * The ai will always be in the initial state on the first loop, so will default allowing
           * us to find the target player for the first time in the default case.
           */
        default:
          Vector2 botPos = new Vector2((float) bot.getX(), (float) bot.getY());
          Vector2 targetPos = new Vector2((float) targetPlayer.getX(), (float) targetPlayer.getY());
          // Calculate the distance to the target from the previous loop
          prevDist = botPos.exactMagnitude(targetPos);
          // Update the target player
          targetPlayer = findTarget(allPlayers);
          targetPos = new Vector2((float) targetPlayer.getX(), (float) targetPlayer.getY());
          // Calculate the distance to the updated target
          newDist = botPos.exactMagnitude(targetPos);

          state = state.next(targetPlayer, bot, prevDist, newDist);
      }
    }
  }

  /**
   * Receives an action and then executes this action. This method will only execute one action at a
   * time (the first action in the list). Since the method will be called inside of the agent loop
   *
   * @param action: an action to exacute.
   */
  private void executeAction(boolean[] action) {
    // TODO decide on the implementation of action execution
    bot.jumpKey = action[Bot.KEY_JUMP];
    bot.leftKey = action[Bot.KEY_LEFT];
    bot.rightKey = action[Bot.KEY_RIGHT];
    bot.click = action[Bot.KEY_CLICK];
  }

  /**
   * Finds the closest player
   *
   * @param allPlayers A list of all players in the world
   * @return The player who is the closest to the bot
   */
  private Player findTarget(List<Player> allPlayers) {
    Player target = null;
    double targetDistance = Double.POSITIVE_INFINITY;
    Vector2 botPos = new Vector2((float) bot.getX(), (float) bot.getY());

    for (Player p : allPlayers) {
      Vector2 playerPos = new Vector2((float) p.getX(), (float) p.getY());
      double distance = botPos.exactMagnitude(playerPos);
      // Update the target if another player is closer
      if (distance < targetDistance) {
        targetDistance = distance;
        target = p;
      }
    }

    return target;
  }
}
