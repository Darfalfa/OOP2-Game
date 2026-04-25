package Characters;

public class ShadowLogic extends Character {

    public ShadowLogic() {
        super("Shadow Dweller", 90, 8, 35, 1);
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
}