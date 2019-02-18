package client.main;

import client.handlers.audioHandler.AudioHandler;
import client.handlers.connectionHandler.ConnectionHandler;
import client.handlers.inputHandler.KeyboardInput;
import client.handlers.inputHandler.MouseInput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shared.gameObjects.players.Player;
import shared.handlers.levelHandler.LevelHandler;
import shared.packets.PacketEnd;
import shared.packets.PacketGameState;
import shared.packets.PacketPlayerJoin;
import shared.physics.Physics;
import shared.util.Path;

public class Client extends Application {

  private static final Logger LOGGER = LogManager.getLogger(Client.class.getName());
  public static LevelHandler levelHandler;
  public static Settings settings;
  public static boolean multiplayer;
  public static ConnectionHandler connectionHandler;
  public static AudioHandler audio;

  private final float timeStep = 0.0166f;
  private final String gameTitle = "Alone in the Dark";
  private final int port = 4445;

  private KeyboardInput keyInput;
  private MouseInput mouseInput;
  private Group root;
  private Group backgroundRoot;
  public static Group gameRoot;
  private Scene scene;
  private float maximumStep;
  private long previousTime;
  private float accumulatedTime;
  private float elapsedSinceFPS = 0f;
  private int framesElapsedSinceFPS = 0;

  public static void main(String args[]) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    setupRender(primaryStage);
    levelHandler = new LevelHandler(settings, root, backgroundRoot, gameRoot);
    keyInput = new KeyboardInput();
    mouseInput = new MouseInput();
    // Setup Input
    scene.setOnKeyPressed(keyInput);
    scene.setOnKeyReleased(keyInput);
    scene.setOnMousePressed(mouseInput);
    scene.setOnMouseMoved(mouseInput);
    scene.setOnMouseReleased(mouseInput);
    scene.setOnMouseDragged(mouseInput);
    audio = new AudioHandler(settings);

    // Main Game Loop
    new AnimationTimer() {
      @Override
      public void handle(long now) {

        if (multiplayer) {
          processServerPackets();
        }

        if (previousTime == 0) {
          previousTime = now;
          return;
        }

        float secondElapsed = (now - previousTime) / 1e9f; // time elapsed in seconds
        float secondsElapsedCapped = Math.min(secondElapsed, maximumStep);
        accumulatedTime += secondsElapsedCapped;
        previousTime = now;

        if (accumulatedTime < timeStep) {
          float timeSinceInterpolation = timeStep - (accumulatedTime - secondElapsed);
          float alphaRemaining = secondElapsed / timeSinceInterpolation;
          levelHandler
              .getGameObjects()
              .forEach(gameObject -> gameObject.interpolatePosition(alphaRemaining));
          return;
        }

        while (accumulatedTime >= 2 * timeStep) {
          levelHandler.getGameObjects().forEach(gameObject -> gameObject.update());
          accumulatedTime -= timeStep;
        }
        /**Calculate Score*/
        ArrayList<Player> alive = new ArrayList<>();
        for (Player p : levelHandler.getPlayers()) {
          if (p.isActive()) {
            alive.add(p);
          }
          if (alive.size() > 1) {
            break;
          }
        }
        if (alive.size() == 1) {
          alive.forEach(player -> player.increaseScore());
        }
        /** Apply Input */
        levelHandler.getClientPlayer().applyInput(multiplayer, connectionHandler);
        // Update bot
        levelHandler.getBotPlayerList()
            .forEach(bot -> bot.applyInput(multiplayer, connectionHandler));
        /** Render Game Objects */
        levelHandler.getGameObjects().forEach(gameObject -> gameObject.render());
        levelHandler.getBackground().render();
        /** Check Collisions */
        Physics.gameObjects = levelHandler.getGameObjects();
        levelHandler
            .getGameObjects()
            .forEach(gameObject -> gameObject.updateCollision(levelHandler.getGameObjects()));
        /** Update Game Objects */
        levelHandler.getGameObjects().forEach(gameObject -> gameObject.update());
        accumulatedTime -= timeStep;
        float alpha = accumulatedTime / timeStep;
        levelHandler.getGameObjects().forEach(gameObject -> gameObject.interpolatePosition(alpha));

        calculateFPS(secondElapsed, primaryStage);
      }
    }.start();
  }

  public void calculateFPS(float secondElapsed, Stage primaryStage) {
    elapsedSinceFPS += secondElapsed;
    framesElapsedSinceFPS++;
    if (elapsedSinceFPS >= 0.5f) {
      int fps = Math.round(framesElapsedSinceFPS / elapsedSinceFPS);
      primaryStage.setTitle(gameTitle + "   --    FPS: " + fps);
      elapsedSinceFPS = 0;
      framesElapsedSinceFPS = 0;
    }
  }

  public void init() {
    maximumStep = 0.0166f;
    previousTime = 0;
    accumulatedTime = 0;
    settings = new Settings();
    multiplayer = false;
    // Start off screen
  }

  private void setupRender(Stage primaryStage) {
    root = new Group();
    backgroundRoot = new Group();
    gameRoot = new Group();

    root.getChildren().add(backgroundRoot);
    root.getChildren().add(gameRoot);

    primaryStage.setTitle(gameTitle);
    primaryStage.getIcons().add(new Image(Path.convert("images/logo.png")));

    // todo TESTING: change controls here
    Button btnPlay = new Button();
    btnPlay.setText("Play");
    btnPlay.setOnAction(
        new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent event) {
            audio.playMusic("FUNK_GAME_LOOP");
          }
        });
    btnPlay.setLayoutX(10);
    btnPlay.setLayoutY(10);
    root.getChildren().add(btnPlay);
    Button btnStop = new Button();
    btnStop.setText("Stop");
    btnStop.setOnAction(
        new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent event) {
            audio.stopMusic();
          }
        });
    btnStop.setLayoutX(100);
    btnStop.setLayoutY(10);
    root.getChildren().add(btnStop);
    Button btnVolL = new Button();
    btnVolL.setText("Vol 20");
    btnVolL.setOnAction(
        new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent event) {
            // audio.setMusicVolume(0.2f);
            settings.setMusicVolume(0.2);
            audio.updateMusicVolume();
          }
        });
    btnVolL.setLayoutX(200);
    btnVolL.setLayoutY(10);
    root.getChildren().add(btnVolL);
    Button btnVolH = new Button();
    btnVolH.setText("Vol 100");
    btnVolH.setOnAction(
        new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent event) {
            // audio.setMusicVolume(1.0f);
            settings.setMusicVolume(1.0);
            audio.updateMusicVolume();
          }
        });
    btnVolH.setLayoutX(300);
    btnVolH.setLayoutY(10);
    root.getChildren().add(btnVolH);
    Slider sldVol = new Slider();
    sldVol.setValue(settings.getMusicVolume() * 100);
    sldVol
        .valueProperty()
        .addListener(
            new InvalidationListener() {
              @Override
              public void invalidated(Observable observable) {
                settings.setMusicVolume(sldVol.getValue() / 100f);
                audio.updateMusicVolume();
              }
            });
    sldVol.setLayoutX(400);
    sldVol.setLayoutY(10);
    root.getChildren().add(sldVol);
    Button btnSfx = new Button();
    btnSfx.setText("SFX");
    btnSfx.setOnAction(
        new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent event) {
            // audio.setMusicVolume(1.0f);
            audio.playSFX("CHOOSE_YOUR_CHARACTER");
          }
        });
    btnSfx.setLayoutX(550);
    btnSfx.setLayoutY(10);
    root.getChildren().add(btnSfx);

    scene = new Scene(root, 1920, 1080);

    primaryStage.setScene(scene);
    primaryStage.setFullScreen(false);
    primaryStage.show();

    // Start Music

  }

  private void processServerPackets() {
    if (connectionHandler.received.size() != 0) {
      try {
        String message = (String) connectionHandler.received.take();
        int messageID = Integer.parseInt(message.substring(0, 1));
        switch (messageID) {
          //PlayerJoin
          case 4:
            PacketPlayerJoin packetPlayerJoin = new PacketPlayerJoin(message);
            levelHandler.addPlayer(
                new Player(packetPlayerJoin.getX(), packetPlayerJoin.getY(),
                    packetPlayerJoin.getUUID()));
            break;
          //End
          case 6:
            PacketEnd packetEnd = new PacketEnd(message);
            multiplayer = false;
            //Show score board
            //Main Menu
            break;
          case 7:
            PacketGameState gameState = new PacketGameState(message);
            HashMap<UUID, String> data = gameState.getGameObjects();
            levelHandler.getGameObjects()
                .forEach(gameObject -> gameObject.setState(data.get(gameObject.getUUID())));
            break;
          default:
            System.out.println(messageID);
            System.out.println(message);

        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
