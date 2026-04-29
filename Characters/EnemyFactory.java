package Characters;

public class EnemyFactory {

    public static Character createEnemyForLevel(int playerLevel) {

        if (playerLevel <= 2) {
            return new ShadowLogic(playerLevel);

        } else if (playerLevel == 3) {
            return new NoctyxLogic();

        } else if (playerLevel <= 5) {
            return new ArmoredGhostLogic(playerLevel);

        } else if (playerLevel == 6) {
            return new BloodmancerLogic();

        } else if (playerLevel <= 8) {
            return new CultistLogic(playerLevel);

        } else {
            return new FinalBossLogic();
        }
    }
}