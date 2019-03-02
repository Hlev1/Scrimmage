package shared.handlers.levelHandler;

import client.handlers.audioHandler.AudioHandler;
import client.handlers.audioHandler.MusicAssets.PLAYLIST;
import client.main.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import javafx.scene.Group;
import server.ai.Bot;
import shared.gameObjects.GameObject;
import shared.gameObjects.MapDataObject;
import shared.gameObjects.Utils.ObjectID;
import shared.gameObjects.background.Background;
import shared.gameObjects.players.Player;
import shared.util.Path;
import shared.util.maths.Vector2;

public class LevelHandler {

  private HashMap<UUID, GameObject> gameObjects;
  private ArrayList<GameObject> toRemove;
  private HashMap<UUID, Player> players;
  private HashMap<UUID, Bot> bots;
  private Player clientPlayer;
  private ArrayList<Map> maps;
  private GameState gameState;
  private Map map;
  private Map previousMap;
  private Group root;
  private Group backgroundRoot;
  private Group gameRoot;
  private Background background;
  private AudioHandler musicPlayer;
  private Settings settings;
  private ArrayList<GameObject> toCreate;

  public LevelHandler(Settings settings, Group root, Group backgroundRoot, Group gameRoot) {
    this.settings = settings;
    gameObjects = new HashMap<>();
    toCreate = new ArrayList<>();
    toRemove = new ArrayList<>();
    players = new HashMap<>();
    bots = new HashMap<>();
    maps = MapLoader.getMaps(settings.getMapsPath());
    this.root = root;
    this.backgroundRoot = backgroundRoot;
    this.gameRoot = gameRoot;
    musicPlayer = new AudioHandler(settings);
    changeMap(new Map("main_menu.map", Path.convert("src/main/resources/menus/main_menu.map"),
        GameState.MAIN_MENU), true);
    previousMap = null;
  }

  public LevelHandler(Settings settings) {
    this.settings = settings;
    gameObjects = new HashMap<>();
    toRemove = new ArrayList<>();
    players = new HashMap<>();
    bots = new HashMap<>();
    toCreate = new ArrayList<>();
    musicPlayer = new AudioHandler(settings);
  }

  public void changeMap(Map map, Boolean moveToSpawns) {
    previousMap = this.map;
    this.map = map;
    generateLevel(root, backgroundRoot, gameRoot, moveToSpawns);
  }

  public void previousMap(Boolean moveToSpawns) {
    if (previousMap != null) {
      Map temp = this.map;
      this.map = previousMap;
      previousMap = temp;
      generateLevel(root, backgroundRoot, gameRoot, moveToSpawns);
    }
  }

  /**
   * NOTE: This to change the level use change Map Removes current game objects and creates new ones
   * from Map file
   */
  public void generateLevel(
      Group root, Group backgroundGroup, Group gameGroup, Boolean moveToSpawns) {

    gameObjects.keySet().removeAll(players.keySet());
    gameObjects.keySet().removeAll(bots.keySet());
    gameObjects.forEach((key, gameObject) -> gameObject.removeRender());
    gameObjects.forEach((key, gameObject) -> gameObject = null);
    gameObjects.clear();

    // Create new game objects for map
    gameObjects = MapLoader.loadMap(map.getPath());
    gameObjects.forEach(
        (key, gameObject) -> {
          if (gameObject.getId() == ObjectID.MapDataObject) {
            this.background = ((MapDataObject) gameObject).getBackground();
            ArrayList<Vector2> spawnPoints = ((MapDataObject) gameObject).getSpawnPoints();
            if (this.background != null) {
              background.initialise(backgroundGroup);
            }
            if (moveToSpawns && spawnPoints != null && spawnPoints.size() >= players.size()) {
              players.forEach(
                  (key2, player) -> {
                    Vector2 spawn = spawnPoints.get(0);
                    player.setX(spawn.getX());
                    player.setY(spawn.getY());
                    spawnPoints.remove(0);
                  });
            }

          } else {
            gameObject.initialise(gameGroup);
          }
        });
    gameObjects.putAll(players);
    gameObjects.forEach((key, gameObject) -> {
      gameObject.setSettings(settings);
    });
    gameState = map.getGameState();
    players.forEach((key, player) -> player.reset());

    musicPlayer.stopMusic();
    switch (gameState) {
      case IN_GAME:
        musicPlayer.playMusicPlaylist(PLAYLIST.INGAME);
        break;
      case MAIN_MENU:
      case Lobby:
      case Start_Connection:
      case Multiplayer:
      default:
        musicPlayer.playMusicPlaylist(PLAYLIST.MENU);
        break;

    }
    System.gc();
  }

  /**
   * List of all current game object
   *
   * @return All Game Objects
   */
  public HashMap<UUID, GameObject> getGameObjects() {
    clearToRemove(); // Remove every gameObjects we no longer need
    return gameObjects;
  }

  public Background getBackground() {
    return this.background;
  }

  /**
   * Add a new bullet to game object list
   *
   * @param gameObject GameObject to be added
   */
  public void addGameObject(GameObject gameObject) {
    gameObject.initialise(this.gameRoot);
    System.out.println("test");
    this.toCreate.add(gameObject);
  }

  public void addGameObject(ArrayList<GameObject> gameObjects) {
    gameObjects.forEach(gameObject -> gameObject.initialise(this.gameRoot));
    this.toCreate.addAll(gameObjects);
  }

  public void createObjects() {
    toCreate.forEach(gameObject -> gameObjects.put(gameObject.getUUID(), gameObject));
    toCreate.clear();
  }

  /**
   * Remove an existing bullet from game object list
   *
   * @param g GameObject to be removed
   */
  public void removeGameObject(GameObject g) {
    toRemove.add(g); // Will be removed on next frame
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

  public HashMap<UUID, Player> getPlayers() {
    return players;
  }

  public void addPlayer(Player newPlayer, Group root) {
    newPlayer.initialise(root);
    players.put(newPlayer.getUUID(), newPlayer);
    gameObjects.put(newPlayer.getUUID(), newPlayer);
  }

  public void addClientPlayer(Group root) {
    clientPlayer = new Player(500, 200, UUID.randomUUID(), this);
    clientPlayer.initialise(root);
    players.put(clientPlayer.getUUID(), clientPlayer);
    gameObjects.put(clientPlayer.getUUID(), clientPlayer);
  }

  public Player getClientPlayer() {
    return clientPlayer;
  }

  public HashMap<UUID, Bot> getBotPlayerList() {
    return bots;
  }

  public AudioHandler getMusicAudioHandler() {
    return this.musicPlayer;
  }

  /**
   * It removes the image from the imageView, destroy the gameObject and remove it from gameObjects
   * list. Finally clear the list for next frame
   */
  private void clearToRemove() {
    gameObjects.entrySet().removeAll(toRemove);
    toRemove.forEach(gameObject -> gameObject.removeRender());
    toRemove.forEach(gameObject -> gameObject.destroy());
    toRemove.clear();
  }

  public Group getGameRoot() {
    return gameRoot;
  }
}
