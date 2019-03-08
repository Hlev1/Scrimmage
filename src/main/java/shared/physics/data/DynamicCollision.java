package shared.physics.data;

import shared.gameObjects.components.BoxCollider;
import shared.gameObjects.components.CircleCollider;
import shared.gameObjects.components.Collider;
import shared.gameObjects.components.ComponentType;
import shared.gameObjects.components.Rigidbody;
import shared.physics.types.CollisionDirection;
import shared.physics.types.RigidbodyType;
import shared.util.maths.Vector2;

/**
 * @author fxa579 Base class to process and manage collisions happening with Dynamic Objects on
 * other Rigidbodies. Used in the backend.
 */
public class DynamicCollision {

  protected Rigidbody bodyA;
  protected Rigidbody bodyB;
  protected Vector2 collisionNormal;
  protected Vector2 penetrationDistance;
  protected CollisionDirection dir;
  protected float pentrationDepth;

  public DynamicCollision(Rigidbody bodyA, Rigidbody bodyB) {
    this.bodyA = bodyA;
    this.bodyB = bodyB;
    calculateCollisionValues();
    // collisionNormal = collisionNormal.normalize();
    process();
  }

  private void calculateCollisionValues() {
    Collider colA = (Collider) bodyA.getParent().getComponent(ComponentType.COLLIDER);
    Collider colB = (Collider) bodyB.getParent().getComponent(ComponentType.COLLIDER);
    switch (colA.getColliderType()) {
      case BOX:
        switch (colB.getColliderType()) {
          case BOX:
            resolveCollision((BoxCollider) colA, (BoxCollider) colB);
            break;
          case CIRCLE:
            resolveCollision((BoxCollider) colA, (CircleCollider) colB);
        }
        break;
      case CIRCLE:
        switch (colB.getColliderType()) {
          case BOX:
            resolveCollision((CircleCollider) colA, (BoxCollider) colB);
            break;
          case CIRCLE:
            resolveCollision((CircleCollider) colA, (CircleCollider) colB);
        }
        break;
      case EDGE:
        switch (colB.getColliderType()) {
        }
        break;
    }
  }

  private void resolveCollision(BoxCollider boxA, BoxCollider boxB) {
    Vector2 n = boxB.getCentre().sub(boxA.getCentre());
    float x_overlap =
        boxA.getSize().getX() * 0.5f + boxB.getSize().getX() * 0.5f - Math.abs(n.getX());
    float y_overlap =
        boxA.getSize().getY() * 0.5f + boxB.getSize().getY() * 0.5f - Math.abs(n.getY());

    penetrationDistance = new Vector2(x_overlap, y_overlap);
    if (penetrationDistance.getX() < penetrationDistance.getY()) {
      if (n.getX() < 0) {
        collisionNormal = Vector2.Left();
      } else {
        collisionNormal =
            bodyB.getBodyType() == RigidbodyType.STATIC ? Vector2.Right() : Vector2.Zero();
      }
      pentrationDepth = x_overlap;
    } else {
      if (n.getY() < 0) {
        collisionNormal = Vector2.Up();
        bodyB.setGrounded(true);
      } else {
        collisionNormal = Vector2.Down();
        bodyA.setGrounded(true);
      }
      pentrationDepth = y_overlap;
    }
  }

  private void resolveCollision(BoxCollider boxA, CircleCollider circB) {
    Vector2 n = circB.getCentre().sub(circB.getCentre());
    Vector2 extents = boxA.getSize().mult(0.5f);
    Vector2 closestPoint = n.clamp(extents.mult(-1), extents);
    boolean inside = false;

    if (n.equals(closestPoint)) {
      inside = true;
      if (Math.abs(n.getX()) > Math.abs(n.getY())) {
        closestPoint =
            new Vector2(
                closestPoint.getX() > 0 ? extents.getX() : extents.getX() * -1,
                closestPoint.getY());
      } else {
        closestPoint =
            new Vector2(
                closestPoint.getX(),
                closestPoint.getY() < 0 ? extents.getY() : extents.getY() * -1);
      }
    }

    Vector2 normal = n.sub(closestPoint);
    float d = normal.magnitude();
    if (inside) {
      collisionNormal = n.mult(-1);
      pentrationDepth = circB.getRadius() - d;
    } else {
      collisionNormal = n;
      pentrationDepth = circB.getRadius() - d;
    }
  }

  private void resolveCollision(CircleCollider circA, CircleCollider circB) {
  }

  private void resolveCollision(CircleCollider circB, BoxCollider boxA) {
    Vector2 n = boxA.getCentre().sub(circB.getCentre());
    Vector2 extents = boxA.getSize().mult(0.5f);
    Vector2 closestPoint = n.clamp(extents.mult(-1), extents);
    boolean inside = false;

    if (n.equals(closestPoint)) {
      inside = true;
      if (Math.abs(n.getX()) > Math.abs(n.getY())) {
        closestPoint =
            new Vector2(
                closestPoint.getX() > 0 ? extents.getX() : extents.getX() * -1,
                closestPoint.getY());
      } else {
        closestPoint =
            new Vector2(
                closestPoint.getX(),
                closestPoint.getY() < 0 ? extents.getY() : extents.getY() * -1);
      }
    }

    Vector2 normal = n.sub(closestPoint);
    float d = normal.magnitude();
    if (inside) {
      collisionNormal = n.mult(-1);
      pentrationDepth = circB.getRadius() - d;
    } else {
      collisionNormal = n;
      pentrationDepth = circB.getRadius() - d;
    }
  }

  public void process() {
    Vector2 velocityCol = bodyB.getVelocity().sub(bodyA.getVelocity());
    float vOnNormal = velocityCol.dot(collisionNormal);
    if (vOnNormal > 0) {
      return;
    }
    float e = Math.max(bodyA.getMaterial().getRestitution(), bodyB.getMaterial().getRestitution());

    float j = -1 * (1 + e) * vOnNormal;
    j /= bodyA.getInv_mass() + bodyB.getInv_mass();

    Vector2 impulse = collisionNormal.mult(j);
    bodyA.setVelocity(bodyA.getVelocity().sub(impulse.mult(bodyA.getInv_mass())));
    bodyB.setVelocity(bodyB.getVelocity().add(impulse.mult(bodyB.getInv_mass())));

    Vector2 positionCorrection = positionCorrection();
    bodyA.correctPosition(positionCorrection.mult(-1 * bodyA.getInv_mass()));
    bodyB.correctPosition(positionCorrection.mult(bodyB.getInv_mass()));
  }

  protected Vector2 positionCorrection() {
    float percent = 0.3f;
    float slop = 0.04f;

    Vector2 correction =
        collisionNormal.mult(
            Math.max(pentrationDepth - slop, 0.0f)
                / (bodyA.getInv_mass() + bodyB.getInv_mass())
                * percent);

    return correction;
    // return penetrationDistance;
  }

  public Rigidbody getBodyA() {
    return bodyA;
  }

  public Rigidbody getBodyB() {
    return bodyB;
  }

  public float getPentrationDepth() {
    return pentrationDepth;
  }

  public Vector2 getCollisionNormal() {
    return collisionNormal;
  }

  public Vector2 getPenetrationDistance() {
    return penetrationDistance;
  }
}
