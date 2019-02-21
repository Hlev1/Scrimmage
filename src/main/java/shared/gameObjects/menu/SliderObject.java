package shared.gameObjects.menu;

import java.util.UUID;
import javafx.scene.Group;
import javafx.scene.control.Slider;
import javafx.scene.text.Text;
import shared.gameObjects.GameObject;
import shared.gameObjects.Utils.ObjectID;

public abstract class SliderObject extends GameObject {

  private final float yOffset = 30;
  protected transient Slider slider;
  protected String label;

  public SliderObject(double x, double y, double sizeX, double sizeY, String label, ObjectID id,
      UUID objectUUID) {
    super(x, y, sizeX, sizeY, id, objectUUID);
    slider = new Slider();
    this.label = label;
  }

  @Override
  public void interpolatePosition(float alpha) {
  }

  @Override
  public void initialise(Group root) {
    slider = new Slider();
    super.initialise(root);
    //   slider.setStyle( todo fix this
    //  ".slider {-fx-border-color: red;-fx-background-color: transparent;}" +
    //".thumb {-fx-background-image: url(\"images/buttons/slider_thumb.png\");}} " +
    // ".track {-fx-background-color: red;}");
    // + "-fx-background-color: transparent;}");
    //   + ".slider .track {-fx-background-image: url(\"images/buttons/slider_bar.png\");}"
    //  + "-fx-background-color: transparent;");
    slider.setLayoutX(getX());
    slider.setLayoutY(getY() + yOffset);
    slider.setMinSize(transform.getSize().getX(), transform.getSize().getY());
    slider.setMaxSize(transform.getSize().getX(), transform.getSize().getY());
    slider.setMin(0);
    slider.setMax(100);
    slider.setShowTickLabels(true);
    slider.setShowTickMarks(true);
    slider.setMajorTickUnit(20);
    slider.setMinorTickCount(5);
    slider.setBlockIncrement(10);
    root.getChildren().add(slider);
    Text text = new Text(label);
    text.setLayoutX(getX());
    text.setLayoutY(getY());
    root.getChildren().add(text);
  }

  public void initialiseAnimation() {
    this.animation.supplyAnimation("default", "images/buttons/slider_thumb.png");
  }

  @Override
  public String getState() {
    return null;
  }

  @Override
  public void update() {
    super.update();
  }

  @Override
  public void render() {
    super.render();
    slider.relocate(getX(), getY());
  }

  public Slider getSlider() {
    return slider;
  }
}