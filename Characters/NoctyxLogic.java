package Characters;

public class NoctyxLogic extends Character {
    public NoctyxLogic() {
        super("Noctyx", 90, 8, 35, 3);
    }

    @Override
    public String getBackgroundInfo(){
        return "Born from the remains of the Shadow Dweller, Noctyx has fully synchronized with the abyss. It no longer hides in darkness — it commands it. What was once an assassin is now a sovereign of shadows.";
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
            case 1: return "Shadow Blink";
            case 2: return "Dark Slash";
            case 3: return "Void Burst";
            default: return "Unknown Skill";
        }
    }

    //placeholder
    @Override
    public String getSkillSfx(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "B1_V1.wav";
            case 2 -> "B1_V2.wav";
            case 3 -> "B1_V3.wav";
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
        return "images/Noctyx.png";
    }

    @Override
    public int getFrameWidth() {
        return 680;
    }

    @Override
    public int getFrameHeight() {
        return 656;
    }

    @Override
    public int getMaxFrames() {
        return 1;
    }

    @Override
    public String getSkillSprite(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "images/Noctyx_ShadowBlink.png";
            case 2 -> "images/Noctyx_DarkSlash.png";
            case 3 -> "images/Noctyx_VoidBurst.png";
            default -> null;
        };
    }

    @Override
    public int getSkillFrameWidth(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 640;
            case 2 -> 639;
            case 3 -> 557;
            default -> 0;
        };
    }

    @Override
    public int getSkillFrameHeight(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 640;
            case 2 -> 639;
            case 3 -> 515;
            default -> 0;
        };
    }

    @Override
    public int getSkillMaxFrames(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 30;
            case 2 -> 25;
            case 3 -> 25;
            default -> 1;
        };
    }

    @Override
    public int getVerticalOffset() {
        return 20;
    }

    @Override
    public double getScale() {
        return 0.4;
    }

    @Override
    public boolean isFinalBoss() {
        return true;
    }
}
