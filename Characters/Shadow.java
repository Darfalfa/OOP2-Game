package Characters;

public class Shadow extends Character {

    public Shadow() {
        super("Shadow Dweller", 90, 8, 35, 1);
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
    public void displaySkills() {
        System.out.println(getName() + "'s Skills:");
        System.out.println("1. Shadow Assassination | Damage: 7-13");
        System.out.println("2. Shadow Severing | Damage: 17-22");
        System.out.println("3. Absolute Darkness | Damage: 28-34 + 2");
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
}