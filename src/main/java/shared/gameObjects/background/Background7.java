package shared.gameObjects.background;

import java.util.UUID;

public class Background7 extends Background {

  private final String imagePath = "images/backgrounds/background7.png";

  public Background7(UUID objectUUID) {
    super(objectUUID);
  }

  @Override
  public void initialiseAnimation() {
    this.animation.supplyAnimation("default", imagePath);
  }
}
