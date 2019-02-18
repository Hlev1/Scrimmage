package shared.handlers.levelHandler;

import client.main.Settings;
import java.util.ArrayList;
import java.util.UUID;
import javafx.scene.Group;
import server.ai.Bot;
import shared.gameObjects.GameObject;
import shared.gameObjects.Utils.ObjectID;
import shared.gameObjects.players.Player;
import shared.gameObjects.weapons.MachineGun;
import shared.gameObjects.weapons.Sword;
import shared.util.Path;

public class LevelHandler {

  private ArrayList<GameObject> gameObjects;
  private ArrayList<GameObject> toRemove;
  private ArrayList<Player> players;
  private ArrayList<Bot> bots;
  private Player clientPlayer;
  private Bot botPlayer;
  private ArrayList<Map> maps;
  private GameState gameState;
  private Map map;
  private Group root;
  private Group backgroundRoot;
  private Group gameRoot;

  public LevelHandler(Settings settings, Group root, Group backgroundRoot, Group gameRoot) {
    gameObjects = new ArrayList<>();
    toRemove = new ArrayList<>();
    players = new ArrayList<>();
    bots = new ArrayList<>();
    maps = MapLoader.getMaps(settings.getMapsPath());
    this.root = root;
    this.backgroundRoot = backgroundRoot;
    this.gameRoot = gameRoot;
    
    clientPlayer = new Player(500, 200, 80, 110, UUID.randomUUID());
    clientPlayer.setHolding(
        //new MachineGun(500, 500, 116, 33, "MachineGun@LevelHandler", clientPlayer, UUID.randomUUID()));
        new Sword(500, 500, 50, 50, "Sword@LevelHandler", clientPlayer, UUID.randomUUID()));
    clientPlayer.initialise(gameRoot);
    players.add(clientPlayer);
    
    changeMap(new Map("main_menu.map", Path.convert("src/main/resources/menus/main_menu.map"),
        GameState.IN_GAME));
    
    gameObjects.add(clientPlayer.getHolding());
    clientPlayer.getHolding().initialise(gameRoot);
    
    botPlayer = new Bot(500, 500, 80, 110, UUID.randomUUID(), gameObjects);
    botPlayer.setHolding(
        new Sword(500, 500, 50, 50, "Sword@LevelHandler", botPlayer, UUID.randomUUID())
    );
    botPlayer.getHolding().initialise(gameRoot);
    botPlayer.initialise(gameRoot);
    bots.add(botPlayer);
    gameObjects.add(botPlayer);
    gameObjects.add(botPlayer.getHolding());
  }


  public LevelHandler(Settings settings) {
    gameObjects = new ArrayList<>();
    toRemove = new ArrayList<>();
    players = new ArrayList<>();
    bots = new ArrayList<>();
  }

  public void changeMap(Map map) {
    this.map = map;
    generateLevel(root, backgroundRoot, gameRoot);
  }


  /**
   * NOTE: This to change the level use change Map Removes current game objects and creates new ones
   * from Map file
   */
  public void generateLevel(Group root, Group backgroundGroup, Group gameGroup) {

    gameObjects.removeAll(players);
    gameObjects.removeAll(bots);
    gameObjects.forEach(gameObject -> gameObject.removeRender());
    gameObjects.forEach(gameObject -> gameObject = null);
    gameObjects.clear();

    // Create new game objects for map
    gameObjects = MapLoader.loadMap(map.getPath());
    gameObjects.forEach(
        gameObject -> {
          if (gameObject.getId() == ObjectID.Background) {
            gameObject.initialise(backgroundGroup);
          } else {
            gameObject.initialise(gameGroup);
          }
        });
    gameObjects.addAll(players);
    gameObjects.addAll(bots);
    gameState = map.getGameState();
    //System.gc();
  }

  /**
   * List of all current game object
   *
   * @return All Game Objects
   */
  public ArrayList<GameObject> getGameObjects() {
    clearToRemove();    // Remove every gameObjects we no longer need
    return gameObjects;
  }

  /**
   * Add a new bullet to game object list
   *
   * @param g GameObject to be added
   */
  public void addGameObject(GameObject g) {
    this.gameObjects.add(g);
    g.initialise(this.gameRoot);
  }

  /**
   * Remove an existing bullet from game object list
   *
   * @param g GameObject to be removed
   */
  public void delGameObject(GameObject g) {
    toRemove.add(g);  // Will be removed on next frame
  }

  /**
   * List of all available maps
   *
   * @return All Maps
   */
  public ArrayList<Map> getMaps() {
    return maps;
  }


  /**
   * Current State of Game, eg Main_Menu or In_Game
   *
   * @return Game State
   */
  public GameState getGameState() {
    return gameState;
  }

  /**
   * Current map that is loaded
   *
   * @return Current Map
   */
  public Map getMap() {
    return map;
  }

  public ArrayList<Player> getPlayers() {
    return players;
  }

  public void addPlayer(Player newPlayer) {
    players.add(newPlayer);
    gameObjects.add(newPlayer);
  }

  public Player getClientPlayer() {
    return clientPlayer;
  }

  public Bot getBotPlayer() {
    return botPlayer;
  }

  /**
   * It removes the image from the imageView, destroy the gameObject and remove it from gameObjects
   * list. Finally clear the list for next frame
   */
  private void clearToRemove() {
    gameObjects.removeAll(toRemove);
    toRemove.forEach(gameObject -> gameObject.removeRender());
    toRemove.forEach(gameObject -> gameObject.destroy());
    toRemove.clear();
  }
}
