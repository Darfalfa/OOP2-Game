package Characters;

public class ShadowLogic extends Character {

    public ShadowLogic(int playerLevel) {
        super("Shadow Dweller", 90, 8, 35, playerLevel);
    }



    @Override
    public String getBackgroundInfo(){
        return "Born from the abyss where light dares not linger, the Shadow Dweller is not a creature—but a manifestation of forgotten fears. It thrives in silence, feeding on doubt and striking when hope flickers weakest. Legends say it was once a royal assassin who embraced the void so completely that it embraced him back, turning him into a living shadow that hunts without mercy. ";
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
            case 1: return "Shadow Assassination";
            case 2: return "Shadow Severing";
            case 3: return "Absolute Darkness";
            default: return "Unknown Skill";
        }
    }

    @Override
    public String getSkillSfx(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "W1_M1.wav";
            case 2 -> "W1_M1.wav";
            case 3 -> "W1_M1.wav";
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
                int min3 = (int)((30 * multiplier));
                int max3 = (int)((36 * multiplier));
                return min3 + " - " + max3;
            default:
                return "0";
        }
    }

    @Override
    public String getSpritePath() {

        if (getLevel() == 2) {
            return "images/ShadowSpriteLv2.png";
        }

        return "images/ShadowSprite.png";
    }

    @Override
    public int getFrameWidth() {
        return 959;
    }

    @Override
    public int getFrameHeight() {
        return 639; //
    }

    @Override
    public int getMaxFrames() {
        return 12; // Both levels use 12 frames
    }

    @Override
    public double getScale() {
        // Shadow sprite sheet frames are 959×639 px.
        // 0.10 → ~96×64 px on screen — consistent regular-mob size.
        return 0.10;
    }
}