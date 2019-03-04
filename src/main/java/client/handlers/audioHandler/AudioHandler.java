package client.handlers.audioHandler;

import client.handlers.audioHandler.MusicAssets.PLAYLIST;
import client.main.Settings;
import java.io.File;
import java.util.ArrayList;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class AudioHandler {

  private MediaPlayer musicPlayer;
  private MediaPlayer effectPlayer;
  private Media musicMedia;
  private Media effectMedia;
  private Settings settings;

  private int trackPos = 0;
  private ArrayList<String> playlist = new ArrayList<>();
  private boolean playingPlaylist = false;
  private PLAYLIST currentPlaylist;

  private MusicAssets musicAssets = new MusicAssets();
  private EffectsAssets effectsAssets = new EffectsAssets();

  public AudioHandler(Settings settings) {
    this.settings = settings;
  }

  /**
   * Starts game music and continuously plays it
   *
   * @param trackName Music resource to play
   */
  public void playMusic(String trackName) {
    String path = musicAssets.getTrackPath(trackName);
    if (!(path == null)) {
      musicMedia = new Media(new File(path).toURI().toString());
      musicPlayer = new MediaPlayer(musicMedia);
      updateMusicVolume();
      musicPlayer.play();
    } else {
      // todo log error
    }
  }

  public void playMusicPlaylist(PLAYLIST playlistSet) {
    if ((currentPlaylist != null) && (playlistSet != currentPlaylist)) {
      //playlist change
      trackPos = 0;
    }
    currentPlaylist = playlistSet;
    playingPlaylist = true;
    playlist = musicAssets.getPlaylist(playlistSet);
    playMusic(playlist.get(trackPos));
    musicPlayer.setOnEndOfMedia(new Runnable() {
      @Override
      public void run() {
        incrementTrack();
        playMusicPlaylist(playlistSet);
      }
    });

  }

  /**
   * Stop any game music from playing
   */
  public void stopMusic() {
    if (musicPlayer != null) {
      if (playingPlaylist) {
        incrementTrack();
      }
      musicPlayer.stop();
    }
  }

  /**
   * Plays in game sound effect
   *
   * @param trackName sound effect resource
   */
  public void playSFX(String trackName) {
    String path = effectsAssets.getTrackPath(trackName);
    if (!(path == null)) {
      effectMedia = new Media(new File(path).toURI().toString());
      effectPlayer = new MediaPlayer(effectMedia);
      updateEffectVolume();
      effectPlayer.play();
    } else {
      // todo log error
    }
  }

  private void incrementTrack() {
    trackPos++;
    if (trackPos == playlist.size()) {
      trackPos = 0;
    }
  }

  public void stopSFX() {
    if (effectPlayer != null) {
      effectPlayer.stop();
    }
  }

  public void updateMusicVolume() {
    if (musicPlayer != null) {
      musicPlayer.setVolume(settings.getMusicVolume());
    }
  }

  public void updateEffectVolume() {
    if (effectPlayer != null) {
      effectPlayer.setVolume(settings.getSoundEffectVolume());
    }
  }
}
