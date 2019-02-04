package server.ai.pathFind;

/** @author Harry Levick (hxl799) */

import server.ai.Bot;
import shared.gameObjects.GameObject;
import shared.gameObjects.players.Player;

import java.util.ArrayList;
import java.util.List;
import shared.util.maths.Vector2;

/**
 * The main file for the A* planner. - search(): This function is the core search algorithm,
 * searching for an optimal path. - optimize(): Function controlling the search and extracting plans
 * to return to the Application Programming Interface.
 */
public class AStar {

  List<GameObject> worldScene;
  // The current best position fouond by the planner.
  public SearchNode bestPosition;
  // The furthest position found by the planner (sometimes different to the best).
  public SearchNode furthestPosition;
  double startingBotXPos;
  // The open list of A*, contains all the unexplored search nodes.
  ArrayList<SearchNode> openList;
  // The closed list of A*
  ArrayList<int[]> closedList = new ArrayList<int[]>(); // Not sure why int[]

  // The plan generated by the planner.
  private ArrayList<boolean[]> currentPlan;
  int ticksBeforeReplanning = 0;

  // The enemy that we are path-finding to.
  Player enemy;
  // The bot that the path-finding is concerned with.
  Bot bot;

  public static final int visitedListPenalty = 1500; // penalty for being in the visited-states list

  /**
   * A SearchNode is a node in the A* search, consisting of an action (that got to the current
   * node), the world state after this action was used, and information about the parent node.
   */
  public class SearchNode {
    // The distance from the start of the search to this node.
    private double distanceElapsed;
    // The optimal distance to reach the goal node AFTER simulating with the selected action.
    private double remainingDistance = 0;
    // The parent node
    public SearchNode parentNode;
    // The list of game objects in this scene
    public List<GameObject> sceneSnapshot;
    // The bot that the path-finding is concerned with.
    public double botX;
    public double botY;

    // Not sure on the use yet. - used in mario a*
    public boolean hasBeenHurt = false;
    public boolean visited = false;
    // The action used to get to the child node.
    boolean[] action;

    // Not sure on the use yet. - used in mario a*
    int repetitions;

    public SearchNode(boolean[] action, int repetitions, SearchNode parent) {
      this.parentNode = parent;
      if (parentNode != null) {
        this.botY = parent.botY + calcYChange(parent.action);
        this.botX = parent.botY + calcXChange(parent.action);
        // Calculate the heuristic value of this node
        this.remainingDistance = estimateRemainingDistance(action);
        distanceElapsed = parent.distanceElapsed + (parent.remainingDistance - remainingDistance);
      } else {
        this.remainingDistance = calcRemainingH(enemy);
        distanceElapsed = 0;
        this.botX = bot.getX();
        this.botY = bot.getY();
      }
      this.action = action;
      this.repetitions = repetitions;
    }

    private double calcXChange(boolean[] action) {
      if (action[Bot.KEY_LEFT]) {
        return -10; // TODO change
      } else if (action[Bot.KEY_RIGHT]) {
        return 10; // TODO change
      } else return 0;
    }

    private double calcYChange(boolean[] action) {
      if (action[Bot.KEY_JUMP]) {
        return -10; // TODO change
      } else return 0;
    }

    /**
     * Calculate the heuristic value for the node.
     * @param enemy the target / goal
     * @return the distance
     */
    public double calcRemainingH(Player enemy, ArrayList<GameObject> allItems) {
      Vector2 botPos = new Vector2((float) bot.getX(), (float) bot.getY());
      Vector2 enemyPos = new Vector2((float) enemy.getX(), (float) enemy.getY());
      double distanceToEnemy = botPos.exactMagnitude(enemyPos);

      GameObject closestItem = findClosestItem(allItems);
      Vector2 itemPos = new Vector2((float) closestItem.getX(), (float) closestItem.getY());
      double distanceToItem = botPos.exactMagnitude(itemPos);
      // The heuristic value is the combined distance of the bot->enemy + bot->item
      double totalH = distanceToEnemy + distanceToItem;

      return totalH;
    }

    /**
     * Generate all the possible children of the node by calculating the result of all possible
     * actions.
     *
     * @return The list of children nodes.
     */
    public ArrayList<SearchNode> generateChildren() {
      ArrayList<SearchNode> list = new ArrayList<>();
      ArrayList<boolean[]> possibleActions = createPossibleActions(this);

      for (boolean[] action : possibleActions) {
        list.add(new SearchNode(action, repetitions, this));
      }

      return list;
    }

    /**
     * Estimate the time remaining to get to the goal for a child node that uses the action.
     *
     * @param action The action to use.
     * @return Time remaining.
     */
    public double estimateRemainingDistance(boolean[] action) {
      return 0.0;
    }

    public double getRemainingDistance() {
      return remainingDistance;
    }

    /**
     * Simulate the world state after we have applied the action of this node, using the parent
     * state.
     */
    public double simulatePos() {
      // Set the state to the parents scene
      worldScene = parentNode.sceneSnapshot;
      parentNode.sceneSnapshot = backupState();

      for (int i = 0; i < repetitions; i++) {
        // Run the simulator
        advanceStep(action);
      }
      // Set the remaining distance after we've simulated the effects of our action.
      remainingDistance = calcRemainingDist();
      if (visited) {
        remainingDistance += visitedListPenalty;
      }
      sceneSnapshot = backupState();

      return remainingDistance;
    }

    public void advanceStep(boolean[] action) {
      // Advance the world scene to a new scene that would be the case if we applied the action.
      // TODO
    }

    /**
     * Finds the closest pick-upable item
     *
     * @param allItems A list of all the items in the world.
     * @return The item that is the closest to the bot
     */
    private GameObject findClosestItem(List<GameObject> allItems) {
      Player target = null;
      double targetDistance = Double.POSITIVE_INFINITY;

      for (GameObject item : allItems) {
        double distance = calcDistance(bot, p);
        // Update the target if another player is closer
        if (distance < targetDistance) {
          targetDistance = distance;
          target = p;
        }
      }

      return target;
    }
  }

  /**
   * Constructor
   *
   * @param worldScene The list of gameObject's in the world.
   * @param bot the bot that this path-finding is concerned with.
   */
  public AStar(List<GameObject> worldScene, Bot bot) {
    this.worldScene = worldScene;
    this.bot = bot;
  }

  /**
   * Main function, this calls the A* planner and extracts and returns the optimal action.
   *
   * @return The action to take.
   */
  public boolean[] optimise(Player enemy) {
    this.enemy = enemy;

    long startTime = System.currentTimeMillis();
    List<GameObject> currentState = backupState();

    // if (workScene == null) { workScene = worldScene; }

    // How many ticks to plan ahead into the future
    int planAhead = 1;
    // How many actions the bot takes for each search step
    int stepsPerSearch = 2; // TODO not sure what this variable does?

    ticksBeforeReplanning--;
    if (ticksBeforeReplanning <= 0 || currentPlan.size() == 0) {
      // We are done planning, extract the plan and prepare the planner for the next planning
      // iteration.
      currentPlan = extractPlan();
      if (currentPlan.size() < planAhead) {
        planAhead = currentPlan.size();
      }

      // workScene = backupState();
      startSearch(stepsPerSearch);
      ticksBeforeReplanning = planAhead;
    }
    // Load the future world state used by the planner.
    // restoreState(workScene);
    search(startTime, enemy);
    // workScene = backupState();

    // Select the next action from our plan
    boolean[] action = new boolean[5];
    if (currentPlan.size() > 0) {
      action = currentPlan.remove(0);
    }

    // restoreState(currentState);

    return action;
  }

  /**
   * The main search function
   *
   * @param startTime
   */
  private void search(long startTime, Player enemy) {
    SearchNode current = bestPosition;
    // Is the current node good (= we're not getting hurt)
    boolean currentGood = false;
    int ticks = 0;

    // Search until we're at the enemy coordinates
    while ((openList.size() != 0) && (bot.getX() != enemy.getX()) && (bot.getY() != enemy.getY())) {
      ticks++;

      // Pick the best node from the open-list
      current = pickBestPos(openList);
      currentGood = false;

      // Simulate the consequences of the action associated with the chosen node
      double realRemainingDistance = current.simulatePos();

      // Now act on what we get as a remaining time.
    }
  }

  /**
   * Make a clone of the current world state (copying the bots state, all enemies, and some level
   * info).
   *
   * @return The clone state.
   */
  public List<GameObject> backupState() {
    ArrayList<GameObject> sceneCopy =
        (ArrayList<GameObject>) ((ArrayList<GameObject>) worldScene).clone();

    return sceneCopy;
  }


  /**
   * Extract the plan by taking the best node and going back to the root, recording the actions
   * taken at each step.
   */
  private ArrayList<boolean[]> extractPlan() {
    ArrayList<boolean[]> actions = new ArrayList<>();

    // Just do nothing if no best position exists
    if (bestPosition == null) {
      for (int i = 0; i < 2; i++) {
        actions.add(createAction(false, false, false, false));
      }

      return actions;
    }

    SearchNode current = bestPosition;
    while (current.parentNode != null) {

      for (int i = 0; i < current.repetitions; i++) {
        actions.add(0, current.action);
      }
    }

    return actions;
  }

  /**
   * Initialise the planner
   *
   * @param repetitions
   */
  private void startSearch(int repetitions) {
    SearchNode startPosition = new SearchNode(null, repetitions, null);
    startPosition.sceneSnapshot = backupState();

    openList = new ArrayList<SearchNode>();
    closedList.clear();
    openList.addAll(startPosition.generateChildren());
    startingBotXPos = bot.getX();

    bestPosition = startPosition;
    furthestPosition = startPosition;
  }

  private boolean[] createAction(boolean jump, boolean left, boolean right, boolean click) {
    boolean[] action = new boolean[5];
    action[Bot.KEY_JUMP] = jump;
    action[Bot.KEY_LEFT] = left;
    action[Bot.KEY_RIGHT] = right;
    action[Bot.KEY_CLICK] = click;

    return action;
  }

  private ArrayList<boolean[]> createPossibleActions(SearchNode currentPos) {
    ArrayList<boolean[]> possibleActions = new ArrayList<>();

    // Jump
    if (canJumpHigher(currentPos, true)) {
      // Just jump
      possibleActions.add(createAction(true, false, false, false));
      // Jump to the right
      possibleActions.add(createAction(true, false, true, false));
      // Jump to the left
      possibleActions.add(createAction(true, true, false, false));
    }
    // Right
    possibleActions.add(createAction(false, false, true, false));
    // Left
    possibleActions.add(createAction(false, true, false, false));

    return possibleActions;
  }

  /**
   * Check to see if the action of jumping makes any difference in the given world state.
   * @param currentPos The state in which we are going to jump.
   * @param checkParent
   * @return
   */
  public boolean canJumpHigher(SearchNode currentPos, boolean checkParent) {
    if (currentPos.parentNode != null
        && checkParent
        && canJumpHigher(currentPos.parentNode, false)) {
      return true;
    }

    return bot.mayJump() || bot.jumpTime > 0;
  }

  private SearchNode pickBestPos(ArrayList<SearchNode> openList) {
    SearchNode bestPos = null;
    double bestPosCost = Double.POSITIVE_INFINITY;

    for (SearchNode current : openList) {
      double currentCost = current.getRemainingDistance() + current.distanceElapsed;
      if (currentCost < bestPosCost) {
        bestPos = current;
        bestPosCost = currentCost;
      }
    }
    openList.remove(bestPos);

    return bestPos;
  }
}
