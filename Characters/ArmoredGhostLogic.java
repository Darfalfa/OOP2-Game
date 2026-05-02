package Characters;

public class ArmoredGhostLogic extends Character {
    public ArmoredGhostLogic(int playerLevel) {
        super("Armored Ghost", 90, 8, 35, playerLevel);
    }

    @Override
    public String getBackgroundInfo(){
        return "Once a valiant knight betrayed by his order and sealed forever within cursed spectral armor by the dungeon’s dark sorcery, the Armored Ghost now prowls the third stage as a ruthless minion of the final boss. This unholy fusion of tormented spirit and indestructible iron drags intruders into eternal suffering, its hollow helm echoing with the howls of the damned.";
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
            case 1: return "Ghost Hand";
            case 2: return "Armor Slam";
            case 3: return "Ghost Possession";
            default: return "Unknown Skill";
        }
    }

    @Override
    public String getSkillSfx(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "Armored.wav";
            case 2 -> "Armored.wav";
            case 3 -> "Armored.wav";
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

        if (getLevel() == 5) {
            return "images/ArmoredGhostSpriteLv2.png";
        }

        return "images/ArmoredGhostSprite.png";
    }

    @Override
    public int getFrameWidth() {
        if (getLevel() == 5) {
            return 241; // Level 5 frame width
        }
        return 368; // Level 4 frame width
    }

    @Override
    public int getFrameHeight() {
        if (getLevel() == 5) {
            return 268; // Level 5 frame width
        }
        return 340; // Level 4 frame width
    }

    @Override
    public int getMaxFrames() {
        return 12; // Both levels use 12 frames
    }

    @Override
    public double getScale() {
        // Lv4 frame: 368×340 → 0.25 = ~92×85 px  ✓ consistent mob size
        // Lv5 frame: 241×268 → 0.35 = ~84×94 px  ✓ consistent mob size
        return (getLevel() == 5) ? 0.35 : 0.25;
    }

    @Override
    public int getVerticalOffset() {
        return 10;
    }
}
