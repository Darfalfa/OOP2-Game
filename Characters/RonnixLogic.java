package Characters;

public class RonnixLogic extends Character {

    public RonnixLogic() {
        super("Ronnix Moonstrke", 100, 10, 100, 1);
    }

    @Override
    public String getBackgroundInfo(){
        return "Once a renowned warrior of the Iron Vanguard, Cabanesh forged his body and spirit through countless battles. He wields his sword and shield not just as a weapon, but as a testament to honor and resilience. No enemy can stand before him for long, and no darkness can quench his unwavering resolve.";
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
                dmg = (int)((random.nextInt(14 - 8 + 1) + 8) * (1 + 0.07 * (getLevel() - 1)));
                setSkillCooldown(1, 0);
                break;

            case 2:
                dmg = (int)((random.nextInt(24 - 18 + 1) + 18) * (1 + 0.07 * (getLevel() - 1)));
                setSkillCooldown(2, 2);
                break;

            case 3:
                dmg = (int)(((random.nextInt(38 - 30 + 1) + 30) * (1 + 0.07 * (getLevel() - 1))) + (3 * (1 + 0.07 * (getLevel() - 1))));
                setSkillCooldown(3, 4);
                break;

            default:
                return 0;
        }

        int dealt = enemy.takeDamage(dmg);

        return dealt; 
    }


    @Override
    public String getSkillName(int skillNumber) {
        switch (skillNumber) {
            case 1: return "Sword Slash";
            case 2: return "Death Thrust";
            case 3: return "Divine Strike";
            default: return "Unknown Skill";
        }
    }

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
                int min1 = (int)(8 * multiplier);
                int max1 = (int)(14 * multiplier);
                return min1 + " - " + max1;
            case 2:
                int min2 = (int)(18 * multiplier);
                int max2 = (int)(24 * multiplier);
                return min2 + " - " + max2;
            case 3:
                int min3 = (int)((30 * multiplier) + (3 * multiplier));
                int max3 = (int)((38 * multiplier) + (3 * multiplier));
                return min3 + " - " + max3 + " + 3";
            default:
                return "0";
        }
    }


}