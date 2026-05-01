package Characters;

public class FinalBossLogic extends Character {
    public FinalBossLogic() {
        super("Khai", 90, 8, 35, 9);
    }

    @Override
    public String getBackgroundInfo(){
        return "none";
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
            case 1: return "Execute";
            case 2: return "Sudden Quiz";
            case 3: return "Codechum Challenge";
            default: return "Unknown Skill";
        }
    }

    //placeholder
    @Override
    public String getSkillSfx(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "B3_V1.wav";
            case 2 -> "B3_V2.wav";
            case 3 -> "B3_V3.wav";
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
        return "images/Khai.png";
    }

    @Override
    public int getFrameWidth() {
        return 432;
    }

    @Override
    public int getFrameHeight() {
        return 640;
    }

    @Override
    public int getMaxFrames() {
        return 1;
    }

    @Override
    public String getSkillSprite(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "images/Khai_Execute.png";
            case 2 -> "images/Khai_SuddenQuiz.png";
            case 3 -> "images/Khai_Codechum.png";
            default -> null;
        };
    }

    @Override
    public int getSkillFrameWidth(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 960;
            case 2 -> 960;
            case 3 -> 960;
            default -> 0;
        };
    }

    @Override
    public int getSkillFrameHeight(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 640;
            case 2 -> 640;
            case 3 -> 640;
            default -> 0;
        };
    }

    @Override
    public int getSkillMaxFrames(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 16;
            case 2 -> 16;
            case 3 -> 16;
            default -> 1;
        };
    }

    @Override
    public int getSkillHorizontalOffset(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> -260;
            case 2 -> -250;
            case 3 -> -240;
            default -> 0;
        };
    }

    @Override
    public int getVerticalOffset() {
        return 40;
    }

    @Override
    public double getScale() {
        return 0.68;
    }

    @Override
    public boolean isFinalBoss() {
        return true;
    }
}
