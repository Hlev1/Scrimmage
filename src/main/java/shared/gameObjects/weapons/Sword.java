package shared.gameObjects.weapons;

import shared.gameObjects.Utils.ObjectID;

public class Sword extends Melee {

  protected double range;
  protected double beginAngle;
  protected double endAngle;

  /**
   * Constructor of the Sword class
   *
   * @param x The x position of the sword
   * @param y The y position of the sword
   * @param id The ObjectID of the sword
   * @param damage Damage of the sword
   * @param weight Weight of the sword
   * @param name Name of the sword
   * @param range Range of the sword
   * @param beginAngle The starting angle when the sword swing
   * @param endAngle The ending angle when the sword swing
   */
  public Sword(
      double x,
      double y,
      ObjectID id,
      double damage,
      double weight,
      String name,
      double range,
      double beginAngle,
      double endAngle) {

    super(x, y, id, damage, weight, name, range, beginAngle, endAngle);
  }

  @Override
  public void update() {
  }

  @Override
  public void render() {
  }

  @Override
  public void interpolatePosition(float alpha) {

  }
}