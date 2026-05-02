package Characters;

public class AyaLogic extends Character {

    public AyaLogic() {
        super("Aya", 90, 8, 100, 1);
    }

    @Override
    public String getBackgroundInfo(){
        return "A master of the bow and wind’s swift whisper, Aya strikes from the shadows with deadly precision. Trained under the Moonlight Rangers, she learned to harness the power of the wind and stars, making each arrow a messenger of fate.";
    }

    @Override
    public int useSkill(int skillNumber, Character enemy) {
        if (skillNumber < 1 || skillNumber > 3) {
            return 0;
        }

        if (!isSkillAvailable(skillNumber)) {
            return 0;
        }

        int dmg;

        switch (skillNumber) {
            case 1:
                dmg = (int)((random.nextInt(16 - 7 + 1) + 7) * (1 + 0.07 * (getLevel() - 1)));
                setSkillCooldown(1, 0);
                break;

            case 2:
                dmg = (int)((random.nextInt(24 - 19 + 1) + 19) * (1 + 0.07 * (getLevel() - 1)));
                setSkillCooldown(2, 2);
                break;

            case 3:
                dmg = (int)(((random.nextInt(39 - 27 + 1) + 27) * (1 + 0.07 * (getLevel() - 1))) + (2 * (1 + 0.07 * (getLevel() - 1))));
                setSkillCooldown(3, 4);
                break;

            default:
                return 0;
        }

        return enemy.takeDamage(dmg);
    }


    @Override
    public String getSkillName(int skillNumber) {
        switch (skillNumber) {
            case 1: return "Chasing Arrow";
            case 2: return "Metal Rain";
            case 3: return "Sagittarius Punishment";
            default: return "Unknown Skill";
        }
    }

    @Override
    public String getSkillSfx(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "Aya_ChasingArrow.wav";
            case 2 -> "Aya_MetalRain.wav";
            case 3 -> "Aya_SagittariusPunishment.wav";
            default -> null;
        };
    }

    @Override
    public String getSkillDamageRange(int skillNumber) {
        double multiplier = (1 + 0.07 * (getLevel() - 1));

        switch (skillNumber) {
            case 1:
                int min1 = (int)(7 * multiplier);
                int max1 = (int)(16 * multiplier);
                return min1 + " - " + max1;

            case 2:
                int min2 = (int)(19 * multiplier);
                int max2 = (int)(24 * multiplier);
                return min2 + " - " + max2;

            case 3:
                int min3 = (int)((27 * multiplier));
                int max3 = (int)((39 * multiplier));

                int bonusDamage = (int)(2 * multiplier);

                return min3 + " - " + max3 + " + " + bonusDamage;

            default:
                return "0";
        }
    }

    @Override
    public String getSkillSprite(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "images/Aya_ChasingArrow.png";
            case 2 -> "images/Aya_MetalRain.png";
            case 3 -> "images/Aya_SagittariusPunishment.png";
            default -> null;
        };
    }

    @Override
    public int getSkillFrameWidth(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 1013;
            case 2 -> 970;
            case 3 -> 964;
            default -> 0;
        };
    }

    @Override
    public int getSkillFrameHeight(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 644;
            case 2 -> 647;
            case 3 -> 640;
            default -> 0;
        };
    }

    @Override
    public int getSkillMaxFrames(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 16;
            case 2 -> 20;
            case 3 -> 20;
            default -> 1;
        };
    }

    @Override
    public double getSkillRenderScale(int skillNumber) {
        return switch (skillNumber) {
            case 3 -> 0.55;
            default -> 1.0;
        };
    }

    @Override
    public int getSkillVerticalOffset(int skillNumber) {
        return switch (skillNumber) {
            case 3 -> -210;
            default -> 0;
        };
    }

    @Override
    public int getVerticalOffset() {
        return 30;
    }

    @Override
    public double getScale() {
        return 0.66;
    }

    @Override
    public double getWorldMapScale() {
        return 1.20;
    }

    @Override
    public int getWorldMapVerticalOffset() {
        return 65;
    }
}