package Characters;
public class JakaraLogic extends Character {

    public JakaraLogic() {
        super("Jakara", 80, 6, 100, 1);
    }

    @Override
    public String getBackgroundInfo(){
        return "Once a prodigy born with powerful innate magic, Jakara immersed herself in ancient arcane, supernatural, and occult knowledge. Known as a wise one, she strikes from afar with explosive spells while her body remains physically frail.";
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
                dmg = (int)((random.nextInt(15 - 9 + 1) + 9) * (1 + 0.07 * (getLevel() - 1)));
                setSkillCooldown(1, 0);
                break;

            case 2:
                dmg = (int)((random.nextInt(25 - 19 + 1) + 19) * (1 + 0.07 * (getLevel() - 1)));
                setSkillCooldown(2, 2);
                break;

            case 3:
                dmg = (int)(((random.nextInt(40 - 32 + 1) + 32) * (1 + 0.07 * (getLevel() - 1))) + (4 * (1 + 0.07 * (getLevel() - 1))));
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
            case 1: return "Eldritch Beam";
            case 2: return "Argent Rupture";
            case 3: return "Astral Supernova";
            default: return "Unknown Skill";
        }
    }

    @Override
    public String getSkillSfx(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "Jakara_EldritchBeam.wav";
            case 2 -> "Jakara_ArgentRupture.wav";
            case 3 -> "Jakara_AstralSupernova.wav";
            default -> null;
        };
    }

    @Override
    public String getSkillDamageRange(int skillNumber) {
        double multiplier = (1 + 0.07 * (getLevel() - 1));

        switch (skillNumber) {
            case 1:
                int min1 = (int)(9 * multiplier);
                int max1 = (int)(15 * multiplier);
                return min1 + " - " + max1;
            case 2:
                int min2 = (int)(19 * multiplier);
                int max2 = (int)(25 * multiplier);
                return min2 + " - " + max2;
            case 3:
                int min3 = (int)((32 * multiplier));
                int max3 = (int)((40 * multiplier));

                int bonusDamage = (int)(4 * multiplier);

                return min3 + " - " + max3 + " + " + bonusDamage;
            default:
                return "0";
        }
    }

    @Override
    public String getSkillSprite(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> "images/Jakara_EldritchBeam.png";
            case 2 -> "images/Jakara_ArgentRupture.png";
            case 3 -> "images/Jakara_AstralSupernova.png";
            default -> null;
        };
    }

    @Override
    public int getSkillFrameWidth(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 961;
            case 2 -> 1016;
            case 3 -> 519;
            default -> 0;
        };
    }

    @Override
    public int getSkillFrameHeight(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 643;
            case 2 -> 647;
            case 3 -> 479;
            default -> 0;
        };
    }

    @Override
    public int getSkillMaxFrames(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 20;
            case 2 -> 19;
            case 3 -> 25;
            default -> 1;
        };
    }

    @Override
    public double getSkillRenderScale(int skillNumber) {
        return switch (skillNumber) {
            case 1 -> 4.0;
            case 2 -> 4.0;
            case 3 -> 3.0;
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