package Characters;

public class CultistLogic extends Character {
    public CultistLogic(int playerLevel) {
        super("Evil Cultist", 90, 8, 35, playerLevel);
    }

    @Override
    public String getBackgroundInfo(){
        return "Once a scholar of Luminae, the Cultist abandoned reason and embraced forbidden rituals. Through cursed hymns and blood sacrifices, he prepares the way for the abyss. He believes suffering is enlightenment — and pain is a sacred offering.";
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
            case 1: return "Cursed Music";
            case 2: return "Life Drain";
            case 3: return "Exploding Blood";
            default: return "Unknown Skill";
        }
    }

    //placeholder
    @Override
    public String getSkillSfx(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "Ronnix_SwordSlash.wav";
            case 2 -> "Ronnix_DeathThrust.wav";
            case 3 -> "Ronnix_DivineStrike.wav";
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
                int min3 = (int)((28 * multiplier) + (3 * multiplier));
                int max3 = (int)((34 * multiplier) + (3 * multiplier));
                return min3 + " - " + max3 + " + 2";
            default:
                return "0";
        }
    }

    @Override
    public String getSpritePath() {

        if (getLevel() == 8) {
            return "images/CultistSpriteLv2.png";
        }

        return "images/CultistSprite.png"; //7
    }

    @Override
    public int getFrameWidth() {
        return 242;
    }

    @Override
    public int getFrameHeight() {
        return 250;
    }

    @Override
    public int getMaxFrames() {
        return 12; // Both levels use 12 frames
    }
}
