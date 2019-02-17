package server.ai.pathFind;

/**
 * @author Harry Levick (hxl799)
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import server.ai.Bot;
import shared.gameObjects.GameObject;
import shared.gameObjects.players.Player;
import shared.gameObjects.weapons.Melee;
import shared.gameObjects.weapons.Weapon;
import shared.physics.Physics;
import shared.physics.data.Collision;
import shared.physics.types.RigidbodyType;
import shared.util.maths.Vector2;

/**
 * The main file for the A* planner. - search(): This function is the core search algorithm,
 * searching for an optimal path. - optimise(): Function controlling the search and extracting plans
 * to return to the Application Programming Interface.
 */
public class AStar {

  List<GameObject> worldScene;
  public static final int visitedListPenalty = 1500; // penalty for being in the visited-states list
  // The current best position fouond by the planner.
  public SearchNode bestPosition;
  // The furthest position found by the planner (sometimes different to the best).
  public SearchNode furthestPosition;
  // The open list of A*, contains all the unexplored search nodes.
  ArrayList<SearchNode> openList;
  // The closed list of A*
  ArrayList<SearchNode> closedList;
  // The plan generated by the planner.
  private List<boolean[]> currentPlan;
  // The enemy that we are path-finding to.
  Player enemy;
  // The bot that the path-finding is concerned with.
  Bot bot;

  /**
   * Constructor
   *
   * @param worldScene The list of gameObject's in the world.
   * @param bot the bot that this path-finding is concerned with.
   */
  public AStar(List<GameObject> worldScene, Bot bot) {
    this.worldScene = worldScene;
    this.bot = bot;
    currentPlan = new ArrayList<>();
  }

  /**
   * Main function, this calls the A* planner and extracts and returns the optimal action.
   *
   * @return The action to take.
   */
  public boolean[] optimise(Player enemy) {
    this.enemy = enemy;

    // If there is no plan, or if the current plan no longer leads to the enemy, create a new plan.
    if (currentPlan.size() == 0 || !atEnemy(bestPosition.botX, bestPosition.botY)) {
      initSearch();
      // Run the search
      search();
      // Extract the plan from the search.
      currentPlan = extractPlan();
    }

    // Select the next action from our plan
    boolean[] action = new boolean[5];
    if (currentPlan.size() > 0) {
      action = currentPlan.remove(0);
    }
    // Before returning the action
    return action;
  }

  /**
   * The main search function
   */
  private void search() {
    // Set the current node to the best position, in case the bot is already at the enemy.
    SearchNode current = bestPosition;
    // Is the current node good (= we're not getting hurt)
    boolean currentGood = false;

    // Search until we're at the enemy coordinates
    while ((openList.size() != 0) && !atEnemy(current.botX, current.botY)) {
      // Pick the best node from the open-list
      current = pickBestPos(openList);
      currentGood = false;

      // get the heuristic value of the best node
      double nodeH = current.getRemainingDistance();

      // Now act on what we get as a remaining distance.
      if (!current.visited && isInClosed(current)) {
        /**
         * If the node is not directly visited, but it is close to a node that has been visited.
         * If the node is already in the closed list (i.e. has been explored before), put some
         * penalty on it and put it back into the pool.
         * Closed List -> Nodes too close to a node in the closed list are considered visited, even
         * though they are a bit different.
         */
        nodeH += visitedListPenalty;
        current.visited = true;
        current.remainingDistance = nodeH;
        openList.add(current);

      } else {
        // Accept the node
        currentGood = true;
        // Put it into the visited list.
        closedList.add(current);
        // Add all children of the current node to the open list.
        openList.addAll(current.generateChildren());

      }

      if (currentGood) {
        // The current node is the best node
        bestPosition = current;

      }

    }

  }

  /**
   * Check if the current position of the bot is close enough to the enemy.
   *
   * @return true if the bot is close enough to the enemy.
   */
  private boolean atEnemy(double bX, double bY) {
    double xDiff = Math.abs(bX - enemy.getX());
    double yDiff = Math.abs(bY - enemy.getY());
    Melee tempMelee;

    if (bot.getHolding().isMelee()) {
      if (xDiff <= (tempMelee = (Melee) bot.getHolding()).getRange()) {
        return true;
      } else {
        return false;
      }
    } else if (xDiff <= 100 && yDiff <= 100) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns if a node is already in the closed list
   */
  private boolean isInClosed(SearchNode node) {
    // Is the x and y coords of the given node too close the the coords of a node in the visited
    // list?
    double nodeX = node.botX;
    double nodeY = node.botY;
    double xDiff = 3.0;
    double yDiff = 3.0;

    for (SearchNode n : closedList) {

      if ((Math.abs(n.botX) - nodeX < xDiff) &&
          (Math.abs(n.botY) - nodeY < yDiff)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Extract the plan by taking the best node and going back to the root, recording the actions
   * taken at each step.
   */
  private ArrayList<boolean[]> extractPlan() {
    ArrayList<boolean[]> actions = new ArrayList<>();

    // Just do nothing if no best position exists
    if (bestPosition == null) {
      return actions;
    }

    SearchNode current = bestPosition;
    while (current.parentNode != null) {
      actions.add(0, current.action);
      current = current.parentNode;
    }

    return actions;
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
   * Initialise the planner
   */
  private void initSearch() {
    SearchNode startPosition = new SearchNode(null, null);
    startPosition.sceneSnapshot = backupState();

    openList = new ArrayList<SearchNode>();
    closedList = new ArrayList<SearchNode>();
    openList.addAll(startPosition.generateChildren());

    bestPosition = startPosition;
    furthestPosition = startPosition;
  }

  private ArrayList<boolean[]> createPossibleActions(SearchNode currentPos) {
    ArrayList<boolean[]> possibleActions = new ArrayList<>();

    Vector2 botPosition = this.bot.getTransform().getPos();
    Vector2 botSize = this.bot.getTransform().getSize();

    // Box cast to the left
    // TODO possibly change this to .Left()
    Collision viscinityLeft = Physics
        .boxcast(botPosition.add(Vector2.Left().mult(botSize)), botSize);
    if (viscinityLeft == null ||
        viscinityLeft.getCollidedObject().getBodyType() != RigidbodyType.STATIC ||
        botPosition.exactMagnitude(viscinityLeft.getPointOfCollision()) > 10) {
      // If no collision, or if the collision is far away
      possibleActions.add(createAction(false, true, false, false));
    }

    // Box cast to the right
    Collision viscinityRight = Physics
        .boxcast(botPosition.add(Vector2.Right().mult(botSize)), botSize);

    if (viscinityRight == null ||
        viscinityRight.getCollidedObject().getBodyType() != RigidbodyType.STATIC ||
        botPosition.exactMagnitude(viscinityRight.getPointOfCollision()) > 10) {
      // If no collision, or if the collision is far away
      possibleActions.add(createAction(false, false, true, false));
    }

    // Box cast upwards
    Collision viscinityUp = Physics.boxcast(botPosition.add(Vector2.Up().mult(botSize)), botSize);
    // TODO: add a way of detecting if we can jump + (left or right)
    // If no collision, or if collision is far away
    if (viscinityUp == null ||
        viscinityUp.getCollidedObject().getBodyType() != RigidbodyType.STATIC /**||
     botPosition.exactMagnitude(viscinityUp.getPointOfCollision()) > 10*/) {
      // Just jump
      possibleActions.add(createAction(true, false, false, false));
      // Jump to the right
      possibleActions.add(createAction(true, false, true, false));
      // Jump to the left
      possibleActions.add(createAction(true, true, false, false));
    }

    return possibleActions;
  }

  private boolean[] createAction(boolean jump, boolean left, boolean right, boolean click) {
    boolean[] action = new boolean[5];
    action[Bot.KEY_JUMP] = jump;
    action[Bot.KEY_LEFT] = left;
    action[Bot.KEY_RIGHT] = right;
    action[Bot.KEY_CLICK] = click;

    return action;
  }

  /**
   * Check to see if the action of jumping makes any difference in the given world state.
   *
   * @param currentPos The state in which we are going to jump.
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

  /**
   * A SearchNode is a node in the A* search, consisting of an action (that got to the current
   * node), the world state after this action was used, and information about the parent node.
   */
  public class SearchNode {

    // The distance from the start of the search to this node.
    double distanceElapsed;
    // The optimal distance to reach the goal node AFTER simulating with the selected action.
    double remainingDistance = 0;
    // The parent node
    SearchNode parentNode;
    // The list of game objects in this scene
    List<GameObject> sceneSnapshot;
    // The bot that the path-finding is concerned with.
    double botX;
    double botY;

    boolean visited = false;
    // The action used to get to the child node.
    boolean[] action;

    public SearchNode(boolean[] action, SearchNode parent) {
      // Instantiate the sceneSnapshot with the world scene so that it is large enough to take
      // the copy of the world scene on the next line.
      sceneSnapshot = new ArrayList<>(worldScene);
      // Take a copy of the current world state
      Collections.copy(sceneSnapshot, worldScene);
      this.parentNode = parent;
      if (parentNode != null) {
        double xChange = calcXChange(action);
        double yChange = calcYChange(action);
        this.botY = parent.botY + yChange;
        this.botX = parent.botX + xChange;
        // Calculate the heuristic value of the node.
        this.remainingDistance = calcRemainingH(enemy, getItems(sceneSnapshot));
        // Calculate the distance from the starting node to the current node
        distanceElapsed =
            parent.distanceElapsed + Math.sqrt(Math.pow(xChange, 2) + Math.pow(yChange, 2));
      } else {
        // This is the starting node so distanceElapsed is 0
        distanceElapsed = 0;
        this.botX = bot.getX();
        this.botY = bot.getY();
        // Calculate the heuristic value of the node.
        this.remainingDistance = calcRemainingH(enemy, getItems(sceneSnapshot));
      }

      this.action = action;
    }

    private double calcXChange(boolean[] action) {
      if (action[Bot.KEY_LEFT]) {
        return -10; // TODO change
      } else if (action[Bot.KEY_RIGHT]) {
        return 10; // TODO change
      } else {
        return 0.0;
      }
    }

    private double calcYChange(boolean[] action) {
      if (action[Bot.KEY_JUMP]) {
        return -10; // TODO change
      } else {
        return 0.0;
      }
    }

    /**
     * Calculate the heuristic value for the node.
     * @param enemy the target / goal
     * @return the distance
     */
    public double calcRemainingH(Player enemy, List<Weapon> allItems) {
      Vector2 botPos = new Vector2((float) botX, (float) botY);
      Vector2 enemyPos = new Vector2((float) enemy.getX(), (float) enemy.getY());
      double totalH = botPos.exactMagnitude(enemyPos);

      if (!allItems.isEmpty()) {
        GameObject closestItem = findClosestItem(allItems);
        Vector2 itemPos = new Vector2((float) closestItem.getX(), (float) closestItem.getY());
        double distanceToItem = botPos.exactMagnitude(itemPos);
        // The heuristic value is the combined distance of the bot->enemy + bot->item
        // The heuristic value for the item is weighted to add preference to pick the items up.
        totalH += (distanceToItem * 2);
      }

      return totalH;
    }

    /**
     * Find all items in the world, currently only finds weapons because no such item yet
     * implemented.
     * TODO change for items.
     * @param allObjects all objects in the world
     * @return list of weapons
     */
    private List<Weapon> getItems(List<GameObject> allObjects) {
      // Collect all weapons from the world
      List<Weapon> allWeapons =
          allObjects.stream()
              .filter(w -> w instanceof Weapon)
              .map(Weapon.class::cast)
              .collect(Collectors.toList());

      return allWeapons;
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
        list.add(new SearchNode(action, this));
      }

      return list;
    }

    public double getRemainingDistance() {
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
    private GameObject findClosestItem(List<Weapon> allItems) {
      GameObject closestItem = null;
      Vector2 botPos = new Vector2((float) bot.getX(), (float) bot.getY());
      double targetDistance = Double.POSITIVE_INFINITY;

      for (GameObject item : allItems) {
        Vector2 itemPos = new Vector2((float) item.getX(), (float) item.getY());
        double distance = botPos.exactMagnitude(itemPos);
        // Update the target if another player is closer
        if (distance < targetDistance) {
          targetDistance = distance;
          closestItem = item;
        }
      }

      return closestItem;
    }
  }

}
