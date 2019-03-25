package shared.gameObjects.weapons;

import java.util.UUID;
import shared.gameObjects.Utils.ObjectType;
import shared.gameObjects.players.Player;
import shared.util.Path;
import shared.util.maths.Vector2;

/**
 * Melee weapon of type Sword
 */
public class Sword extends Melee {

  /**
   * Path to image
   */
  private static String imagePath = "images/weapons/sword.png";
  /**
   * Maximum angle to aim before switching holding hand
   */
  private static float AIM_ANGLE_MAX = 110f;
  /** Index indicating current angle when attacking */
  private int currentAngleIndex;
  /**
   * -1 if aiming Left, 1 if aiming Right
   */
  private double attackAngleSign;

  /**
   * Constructor of the Sword class
   *
   * @param x The x position of the sword
   * @param y The y position of the sword
   * @param name Name of the sword
   * @param holder Player who holds this sword
   * @param uuid The UUID of the sword
   */
  public Sword(double x, double y, String name, Player holder, UUID uuid) {
    super(
        x,
        y,
        19,
        121,
        ObjectType.Weapon,
        20, // hazard
        10, // weight
        name,
        30, // ammo
        60, // fireRate
        17, // pivotX
        40, //pivotY
        holder,
        50, // range
        50, // beginAngle
        20, // endAngle
        true, // singleHanded
        uuid);

    attackAngleSign = 1;
    this.weaponRank = 1;
  }

  /**
   * Constructor that duplicate a Sword with different UUID
   *
   * @param that Source of duplication
   */
  public Sword(Sword that) {
    this(that.getX(), that.getY(), that.name, that.holder, UUID.randomUUID());
  }

  @Override
  public void fire(double mouseX, double mouseY) {
    super.fire(mouseX, mouseY);
  }

  @Override
  public void render() {
    super.render();

    if (startedThrowing || holder == null) { return; }

    // start here
    imageView.getTransforms().clear();

    double mouseX = holder.mouseX;
    double mouseY = holder.mouseY;
    Vector2 mouseV = new Vector2((float) mouseX, (float) mouseY);
    Vector2 gripV = new Vector2((float) this.getGripX(), (float) this.getGripY());
    Vector2 mouseSubGrip = mouseV.sub(gripV);
    angleRadian = mouseSubGrip.normalize().angleBetween(Vector2.Zero());  // radian
    double angle = angleRadian * 180 / PI;  // degree

    // Change the facing of the player when aiming the other way and
    // Set the angle of rotation
    double angleHorizontal;  // degree
    if (holder.isAimingLeft()) {
      angleHorizontal = (mouseSubGrip.angleBetween(Vector2.Left())) * 180 / PI;
      if (angleHorizontal > AIM_ANGLE_MAX) {
        holder.setAimingLeft(false);
        angle = angleHorizontal - 180f;
      }
      if (angleHorizontal > 90f) {
        angle = angleHorizontal * (mouseY > this.getGripY() ? -1 : 1);
      }
    } else { // holder aiming Right
      angleHorizontal = (mouseSubGrip.angleBetween(Vector2.Right())) * 180 / PI;
      if (angleHorizontal > AIM_ANGLE_MAX) {
        holder.setAimingLeft(true);
        angle = 180f - angleHorizontal;
      }
      if (angleHorizontal > 90f) {
        angle = angleHorizontal * (mouseY > this.getGripY() ? 1 : -1);
      }
    }

    angleRadian = angle * PI / 180;

    // Rotate and translate the image
    if (holder.isAimingLeft()) {
      imageView.setScaleX(-1);
      rotate.setAngle(-angle);
      imageView.getTransforms().add(rotate);
      imageView.setTranslateX(this.getGripFlipX());
      imageView.setTranslateY(this.getGripFlipY());
    } else {
      imageView.setScaleX(1);
      rotate.setAngle(angle);
      imageView.getTransforms().add(rotate);
      imageView.setTranslateX(this.getGripX());
      imageView.setTranslateY(this.getGripY());
    }
    // end here

    if (this.attacking) {
      this.imageView
          .setRotate((45 * attackAngleSign) + (attackAngleSign * -1 * getAngle(currentAngleIndex)));
      // set incrementation of angles for frames
      currentAngleIndex += 4;
      if (currentAngleIndex >= (int) (beginAngle + endAngle + 1)) {
        attacking = false;
        currentAngleIndex = 0;
        this.imageView.setRotate(0);
      }
    }
  }

  @Override
  public void initialiseAnimation() {
    this.animation.supplyAnimation(
        "default", Path.convert(this.imagePath));
  }

  @Override
  public double getGripX() {
    if (holder.isAimingLeft()) {
      return getGripFlipX();
    }

    this.imageView.setScaleX(1);
    if (!attacking) {
      attackAngleSign = 1;
    }
    return holderHandPos[0] - 6;
  }

  @Override
  public double getGripY() {
    if (holder.isAimingLeft()) {
      return getGripFlipY();
    }

    this.imageView.setScaleX(1);
    if (!attacking) {
      attackAngleSign = 1;
    }
    return holderHandPos[1] - 34;
  }

  @Override
  public double getGripFlipX() {
    this.imageView.setScaleX(-1);
    if (!attacking) {
      attackAngleSign = -1;
    }
    return holderHandPos[0] - 34;
  }

  @Override
  public double getGripFlipY() {
    this.imageView.setScaleX(-1);
    if (!attacking) {
      attackAngleSign = -1;
    }
    return holderHandPos[1] - 38;
  }
}
