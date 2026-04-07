package Characters;

public class Jakara extends Character {

    public Jakara() {
        super("Jakara", 80, 6, 100, 1);
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
                setSkillCooldown(2, 3);
                break;

            case 3:
                dmg = (int)(((random.nextInt(40 - 32 + 1) + 32) * (1 + 0.07 * (getLevel() - 1))) + (4 * (1 + 0.07 * (getLevel() - 1))));
                setSkillCooldown(3, 5);
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
                int min3 = (int)((32 * multiplier) + (4 * multiplier));
                int max3 = (int)((40 * multiplier) + (4 * multiplier));
                return min3 + " - " + max3 + " + 4";
            default:
                return "0";
        }
    }

    @Override
    public void displaySkills() {
        System.out.println(BLUE + "------------------ " + getName() + "'s Skills ------------------" + RESET);
        for (int i = 1; i <= 3; i++) {
            String skillName = getSkillName(i);
            String damageRange = getSkillDamageRange(i);
            int cooldown = getSkillCooldown(i);
            String status;

            if (isSkillAvailable(i)) {
                status = GREEN + "Ready" + RESET;
            } else {
                status = YELLOW + "Cooldown: " + cooldown + " turn(s)" + RESET;
            }

            System.out.println(i + ". " + skillName + " | Damage: " + damageRange + " | " + status);
        }
    }
}