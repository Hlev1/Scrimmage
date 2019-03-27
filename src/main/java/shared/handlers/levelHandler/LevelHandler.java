package shared.handlers.levelHandler;

import client.handlers.audioHandler.AudioHandler;
import client.handlers.audioHandler.MusicAssets.PLAYLIST;
import client.handlers.effectsHandler.Particle;
import client.handlers.effectsHandler.emitters.ParticleEmitter;
import client.main.Client;
import client.main.Settings;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.scene.Group;
import server.Server;
import server.ai.Bot;
import shared.gameObjects.GameObject;
import shared.gameObjects.MapDataObject;
import shared.gameObjects.Utils.ObjectType;
import shared.gameObjects.background.Background;
import shared.gameObjects.players.Limb;
import shared.gameObjects.players.Player;
import shared.packets.PacketDelete;
import shared.util.Path;
import shared.util.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import shared.util.maths.Vector2;

/**
 * Manager class for handling map changes and dynamically adding and removing GameObjects
 */
public class LevelHandler {

  private ConcurrentLinkedHashMap<UUID, GameObject> gameObjects;
  private CopyOnWriteArrayList<GameObject> toRemove;
  private LinkedHashMap<UUID, Player> players;
  private LinkedHashMap<UUID, Limb> limbs;
  private LinkedHashMap<UUID, Bot> bots;
  private Player clientPlayer;
  private ArrayList<Map> maps;
  private GameState gameState;
  private Map map;
  private LinkedList<Map> playlist;
  private Map previousMap;
  private Group backgroundRoot;
  private Group gameRoot;
  private Group uiRoot;
  private Background background;
  private AudioHandler musicPlayer;
  private Settings settings;
  private ArrayList<GameObject> toCreate;
  private boolean isServer;
  private Server server;

  /**
   * Constructs level handler for client
   * @param settings Settings attached to client
   * @param backgroundRoot root containing background images
   * @param gameRoot main game root containing all the objects
   * @param uiRoot root containing the foreground and UI images
   */
  public LevelHandler(Settings settings, Group backgroundRoot, Group gameRoot,
      Group uiRoot) {
    this.settings = settings;
    gameObjects = new ConcurrentLinkedHashMap.Builder<UUID, GameObject>()
        .maximumWeightedCapacity(500).build();
    toCreate = new ArrayList<>();
    toRemove = new CopyOnWriteArrayList<>();
    players = new LinkedHashMap<>();
    limbs = new LinkedHashMap<>();
    bots = new LinkedHashMap<>();
    maps = MapLoader.getMaps(settings.getMapsPath());
    this.backgroundRoot = backgroundRoot;
    this.gameRoot = gameRoot;
    this.isServer = false;
    this.uiRoot = uiRoot;

    musicPlayer = new AudioHandler(settings, Client.musicActive);
    changeMap(
        new Map("menus/main_menu.map", Path.convert("src/main/resources/menus/main_menu.map")),
        true, false);
    previousMap = null;
    setPlaylist("playlist1");
  }
  /**
   * Constructs level handler for server
   * @param settings Settings attached to server
   * @param backgroundRoot root containing background images
   * @param gameRoot main game root containing all the objects
   * @param server The server the handler is attached to
   */
  public LevelHandler(Settings settings, Group backgroundRoot, Group gameRoot, Server server) {
    this.settings = settings;
    gameObjects = new ConcurrentLinkedHashMap.Builder<UUID, GameObject>()
        .maximumWeightedCapacity(500).build();
    toRemove = new CopyOnWriteArrayList<>();
    players = new LinkedHashMap<>();
    limbs = new LinkedHashMap<>();
    toCreate = new ArrayList<>();
    bots = new LinkedHashMap<>();
    maps = MapLoader.getMaps(settings.getMapsPath());
    this.isServer = true;
    this.backgroundRoot = backgroundRoot;
    this.gameRoot = gameRoot;
    this.server = server;
    changeMap(new Map("LOBBY", Path.convert("src/main/resources/menus/lobby.map")),
        false, true);
    previousMap = null;
    setPlaylist("playlist1");
  }

  /**
   * Changes the current active map
   * @param map Map object containing details for the new map to load
   * @param moveToSpawns If true, moves the players to the spawnpoints found on the map
   * @param isServer If false, it is a client changing their map
   */
  public void changeMap(Map map, Boolean moveToSpawns, Boolean isServer) {
    previousMap = this.map;
    this.map = map;
    if (!isServer) {
      Client.closeSettingsOverlay();
    }

    generateLevel(backgroundRoot, gameRoot, moveToSpawns, isServer);

    if (!isServer) {
      uiRoot.getChildren().clear();
      switch (gameState) {
        case IN_GAME:
        case MULTIPLAYER:
          if (Client.levelHandler != null) {
            Client.setUserInterface();
          }
          break;
      }
    }
  }

  /**
   * Loads the previous map instead of the current one
   * @param moveToSpawns
   */
  public void previousMap(Boolean moveToSpawns) {
    if (previousMap != null) {
      Map temp = this.map;
      this.map = previousMap;
      previousMap = temp;
      generateLevel(backgroundRoot, gameRoot, moveToSpawns, false);
    }
  }

  /**
   * Recreates the level from the data found in the current map object
   */
  public void generateLevel(Group backgroundGroup, Group gameGroup, Boolean moveToSpawns,
      Boolean isServer) {

    settings.resetDeaths();
    gameObjects.keySet().removeAll(players.keySet());
    gameObjects.keySet().removeAll(bots.keySet());
    gameObjects.keySet().removeAll(limbs.keySet());
    gameObjects.forEach((key, gameObject) -> gameObject.removeRender());
    gameObjects.forEach((key, gameObject) -> gameObject = null);
    gameObjects.clear();

    // Create new game objects for map
    gameObjects = MapLoader.loadMap(map.getPath());
    gameObjects.forEach(
        (key, gameObject) -> {
          gameObject.setSettings(settings);
          if (gameObject.getId() == ObjectType.MapDataObject) {
            this.background = ((MapDataObject) gameObject).getBackground();
            ArrayList<Vector2> spawnPoints = ((MapDataObject) gameObject).getSpawnPoints();
            this.map.setGameState(((MapDataObject) gameObject).getGameState());
            if (this.background != null) {
              background.initialise(backgroundGroup, settings);
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
            gameObject.initialise(gameGroup, settings);
          }
        });
    gameObjects.putAll(players);
    gameObjects.putAll(limbs);
    limbs.forEach((key, limbs) -> limbs.reset());
    gameObjects.forEach((key, gameObject) -> gameObject.setSettings(settings));
    gameState = map.getGameState() != null ? map.getGameState() : GameState.MAIN_MENU;
    players.forEach((key, player) -> {
      player.reset();
    });

    if (!isServer) {
      musicPlayer.stopMusic();
      switch (gameState) {
        case IN_GAME:
          musicPlayer.playMusicPlaylist(PLAYLIST.INGAME);
          break;
        case MAIN_MENU:
        case LOBBY:
        case START_CONNECTION:
        case MULTIPLAYER:
        default:
          musicPlayer.playMusicPlaylist(PLAYLIST.MENU);
          break;
      }
    } else {
      server.sendObjects(gameObjects);
    }
  }

  /**
   * List of all current game object
   *
   * @return All Game Objects
   */
  public ConcurrentLinkedHashMap<UUID, GameObject> getGameObjects() {
    clearToRemove(); // Remove every gameObjects we no longer need
    return gameObjects;
  }

  /**
   * List of all gameObjects excluding players
   * @return All non-player Game Objects
   */
  public ConcurrentLinkedHashMap<UUID, GameObject> getGameObjectsFiltered() {
    clearToRemove(); // Remove every gameObjects we no longer need
    ConcurrentLinkedHashMap<UUID, GameObject> filtered = new ConcurrentLinkedHashMap.Builder<UUID, GameObject>()
        .maximumWeightedCapacity(500).build();
    filtered.putAll(gameObjects);
    players.forEach((key, player) -> filtered.remove(key));
    return filtered;
  }

  public Background getBackground() {
    return this.background;
  }

  /**
   * Readd limbs
   */
  public void addLimbs(GameObject gameObject) {
    this.toCreate.add(gameObject);
  }

  /**
   * Add a gameobject to list to be created next update
   *
   * @param gameObject GameObject to be added
   */
  public void addGameObject(GameObject gameObject) {
    if((gameObject instanceof Particle || gameObject instanceof ParticleEmitter) && isServer) return;
    try {
      gameObject.initialise(this.gameRoot, settings);
      this.toCreate.add(gameObject);
      if (isServer) {
        ConcurrentLinkedHashMap<UUID, GameObject> temp = new ConcurrentLinkedHashMap.Builder<UUID, GameObject>()
            .maximumWeightedCapacity(1).build();
          temp.put(gameObject.getUUID(), gameObject);
        server.sendObjects(temp);
      }
    } catch (IllegalStateException e) {
      System.out.println("AI - avoiding placement of object");
    }

  }

  public void addGameObjects(ConcurrentLinkedHashMap<UUID, GameObject> gameObjectsT) {
    gameObjectsT.forEach(((uuid, gameObject) -> {
      if (!(gameObjects.containsKey(uuid) && (gameObject instanceof Particle || gameObject instanceof ParticleEmitter))) {
        this.toCreate.add(gameObject);
        if (gameObject instanceof Player) {
          this.toCreate.addAll(gameObject.getChildren());
          this.toCreate.add(((Player) gameObject).getHandLeft());
          this.toCreate.add(((Player) gameObject).getHandRight());
        }
      }
    }));
  }

  /**
   * Creates game objects that are meant to be created from the previous loop
   */
  public void createObjects() {
    toCreate.forEach(gameObject -> {
      gameObject.initialise(gameRoot, settings);
      if (gameObject instanceof Player && gameObject.getUUID() != clientPlayer.getUUID()) {
        addPlayer((Player) gameObject, settings.getGameRoot());
      } else {
        gameObjects.put(gameObject.getUUID(), gameObject);
      }
    });
    toCreate.clear();
  }

  /**
   * Remove an existing bullet from game object list
   *
   * @param g GameObject to be removed
   */
  public void removeGameObject(GameObject g) {
    toRemove.add(g); // Will be removed on next frame
    if (isServer) {
      PacketDelete delete = new PacketDelete(g.getUUID());
      server.sendToClients(delete.getData(), false);
    }
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
   * Initialise the playlist with a selected playlist name
   * @param playlistName The playlist to be created, must be the name of the directory
   */
  public void setPlaylist(String playlistName) {
    playlist = new LinkedList<>();
    int playlistLength = new File(settings.getMapsPath() + File.separator + playlistName)
        .list().length;

    for (int i = 1; i <= playlistLength; i++) {
      playlist.add(
          new Map(
              "Map" + i,
              Path.convert(settings.getMapsPath() + File.separator + playlistName + File.separator + "map" + i + ".map")));
    }
  }

  public LinkedList<Map> getPlaylist() { return playlist; }

  public Map pollPlayList() {
    int index = new Random().nextInt(getPlaylist().size());
    return playlist.get(index);
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

  public LinkedHashMap<UUID, Player> getPlayers() {
    return players;
  }

  public void addPlayer(Player newPlayer, Group root) {
    newPlayer.initialise(root, settings);
    players.put(newPlayer.getUUID(), newPlayer);
    gameObjects.put(newPlayer.getUUID(), newPlayer);
  }

  public void addPlayer2(Player newPlayer) {
    players.put(newPlayer.getUUID(), newPlayer);
    gameObjects.put(newPlayer.getUUID(), newPlayer);
  }

  public void addClientPlayer(Group root) {
    clientPlayer = new Player(500, 200, UUID.randomUUID());
    clientPlayer.initialise(root, settings);
    players.put(clientPlayer.getUUID(), clientPlayer);
    gameObjects.put(clientPlayer.getUUID(), clientPlayer);
  }

  public Player getClientPlayer() {
    return clientPlayer;
  }

  public LinkedHashMap<UUID, Bot> getBotPlayerList() {
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
    gameObjects.values().removeAll(toRemove);
    toRemove.forEach(gameObject -> gameObject.removeRender());
    toRemove.forEach(gameObject -> gameObject.destroy());
    toRemove.clear();
  }

  public Group getGameRoot() {
    return gameRoot;
  }
  
  public Group getBackgroundRoot() {
    return backgroundRoot;
  }

  public LinkedHashMap<UUID, Limb> getLimbs() {
    return limbs;
  }


}
