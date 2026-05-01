package Characters;

public class BloodmancerLogic extends Character {
    public BloodmancerLogic() {
        super("Bloodmancer", 90, 8, 35, 6);
    }

    @Override
    public String getBackgroundInfo(){
        return "Once a devoted servant of the abyss, the Bloodmancer sacrificed his humanity to gain dominion over life itself. He believes suffering is purification, and blood is the purest currency of power.";
    }

    @Override
    public int useSkill(int skillNumber, Character enemy) {
        if (skillNumber < 1 || skillNumber > 3) {
            return 0;
        }

        if (!isSkillAvailable(skillNumber)) {
            return 0;
        }

        int dmg = 0;

        switch (skillNumber) {
            case 1:
                dmg = (int)((random.nextInt(13 - 7 + 1) + 7) * (1 + 0.07 * (getLevel() - 1)));
                setSkillCooldown(1, 0);
                break;
            case 2:
                dmg = (int)((random.nextInt(22 - 17 + 1) + 17) * (1 + 0.07 * (getLevel() - 1)));
                setSkillCooldown(2, 1);
                break;
            case 3:
                dmg = (int)((random.nextInt(34 - 28 + 1) + 28 + 2) * (1 + 0.07 * (getLevel() - 1)));
                setSkillCooldown(3, 3);
                break;
            default:
                return 0;
        }

        return enemy.takeDamage(dmg);
    }

    @Override
    public int getXpReward() {
        return random.nextInt(35 - 10 + 1) + 10;
    }

    @Override
    public String getSkillName(int skillNumber) {
        switch (skillNumber) {
            case 1: return "Curse Wave";
            case 2: return "Soul Drain";
            case 3: return "Hemoburst";
            default: return "Unknown Skill";
        }
    }

    //placeholder
    @Override
    public String getSkillSfx(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "B2_V1.wav";
            case 2 -> "B2_V2.wav";
            case 3 -> "B2_V3.wav";
            default -> null;
        };
    }

    @Override
    public String getSkillDamageRange(int skillNumber) {
        double multiplier = (1 + 0.07 * (getLevel() - 1));

        switch (skillNumber) {
            case 1:
                int min1 = (int)(7 * multiplier);
                int max1 = (int)(13 * multiplier);
                return min1 + " - " + max1;
            case 2:
                int min2 = (int)(17 * multiplier);
                int max2 = (int)(22 * multiplier);
                return min2 + " - " + max2;
            case 3:
                int min3 = (int)((28 * multiplier) + (3));
                int max3 = (int)((34 * multiplier) + (3));
                return min3 + " - " + max3 + " + 2";
            default:
                return "0";
        }
    }

    @Override
    public String getSpritePath() {
        return "images/Bloodmancer.png";
    }

    @Override
    public int getFrameWidth() {
        return 544;
    }

    @Override
    public int getFrameHeight() {
        return 620;
    }

    @Override
    public int getMaxFrames() {
        return 1;
    }

    @Override
    public String getSkillSprite(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "images/Bloodmancer_CurseWave.png";
            case 2 -> "images/Bloodmancer_SoulDrain.png";
            case 3 -> "images/Bloodmancer_Hemoburst.png";
            default -> null;
        };
    }

    @Override
    public int getSkillFrameWidth(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 544;
            case 2 -> 544;
            case 3 -> 543;
            default -> 0;
        };
    }

    @Override
    public int getSkillFrameHeight(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 620;
            case 2 -> 664;
            case 3 -> 735;
            default -> 0;
        };
    }

    @Override
    public int getSkillMaxFrames(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 30;
            case 2 -> 30;
            case 3 -> 30;
            default -> 1;
        };
    }

    @Override
    public double getSkillRenderScale(int skillNumber) {
        return 1.0;
    }

    @Override
    public int getVerticalOffset() {
        return -8;
    }

    @Override
    public double getScale() {
        // Not used for sizing because Bloodmancer uses the boss draw path.
        return 0.81;
    }

    @Override
    public boolean isFinalBoss() {
        // true so computeEnemyDrawSize uses the boss path for both idle and skills.
        return true;
    }

    /**
     * Bloodmancer should be visually smaller than the true final bosses (Khai, Noctyx).
     * The idle frame is 544x620, matching the skill sheets' placement box.
     */
    @Override
    public double getBossHeightFraction() {
        return 0.74;
    }
}
