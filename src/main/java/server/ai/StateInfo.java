package server.ai;

import shared.gameObjects.players.Player;
import shared.gameObjects.weapons.Melee;

public class StateInfo {

  protected static Melee tempMelee;
  protected static double weaponRange;
  protected static int ammoLeft;
  protected static int botHealth;
  protected static int enemyHealth;

  protected static void setInfo(Player target, Player bot) {
    weaponRange =
        (bot.getHolding().isGun())
            ? Double.POSITIVE_INFINITY
            : (tempMelee = (Melee) bot.getHolding()).getRange();
    ammoLeft = (bot.getHolding().isGun()) ? bot.getHolding().getAmmo() : 0;

    botHealth = bot.getHealth();
    enemyHealth = target.getHealth();
  }
}
