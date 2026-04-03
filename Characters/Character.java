package Characters;

import java.util.Random;

abstract public class Character {
    public Random random = new Random();

    private String name, title;
    private int[] skillCooldowns;
    private int hp, maxHp, baseHp;
    private int defense, maxDefense, baseDefense;
    private int level;
    private int potionCount = 0;
    private int baseExp;
    private int currentXp = 0;
    private int nextLevelXp;
    private int gold;

    public String RESET = "\033[0m";
    public String RED = "\033[31m";
    public String GREEN = "\033[32m";
    public String BLUE = "\033[34m";
    public String YELLOW = "\033[33m";
    public String PURPLE = "\033[35m";

    public Character(String name, int hp, int defense, int baseExp, int level) {
        this.name = name;
        this.baseHp = hp;
        this.baseDefense = defense;
        this.baseExp = baseExp;
        this.level = level;
        recalcStats();
        this.skillCooldowns = new int[3];
    }

    private void recalcStats() {
        this.maxHp = (int)(baseHp * (1 + 0.08 * (level - 1)));
        this.hp = maxHp;
        this.maxDefense = (int)(baseDefense * (1 + 0.05 * (level - 1)));
        this.defense = maxDefense;
        this.nextLevelXp = (int)(baseExp * Math.pow(level, 1.5));
    }

    public void gainXp(int amount) {
        currentXp += amount;
        System.out.println(GREEN + name + " gains " + amount + " XP! Current XP: " + currentXp + "/" + nextLevelXp + RESET);

        while (currentXp >= nextLevelXp) {
            currentXp -= nextLevelXp;
            levelUp();
        }
    }

    public void receiveBattleRewards(int xp, int goldReward) {
        System.out.println(GREEN + "\nBattle Rewards:" + RESET);
        gainXp(xp);

        gold += goldReward;
        System.out.println(YELLOW + name + " received " + goldReward + " gold! Total Gold: " + gold + RESET);
    }

    public int getGold() {
        return gold;
    }

    private void levelUp() {
        level++;
        recalcStats();
        hp = maxHp;
        System.out.println(GREEN + name + " leveled up! Now level " + level + "!" + RESET);
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getDefense() {
        return defense;
    }

    public int getMaxDefense() {
        return maxDefense;
    }

    public int getLevel() {
        return level;
    }

    public int getPotionCount() {
        return potionCount;
    }

    public void setPotionCount(int potionCount) {
        this.potionCount = potionCount;
    }

    public int getCurrentXp() {
        return currentXp;
    }

    public int getNextLevelXp() {
        return nextLevelXp;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public int getSkillCooldown(int skillNumber) {
        return skillCooldowns[skillNumber - 1];
    }

    public int[] getSkillCooldowns() {
        return skillCooldowns;
    }

    public void setHp(int hp) {
        this.hp = Math.min(hp, maxHp);
    }

    public void setDefense(int defense) { 
        this.defense = defense; 
    }

    public void setMaxDefense(int maxDefense) { 
        this.maxDefense = maxDefense; 
    }

    public void setLevel(int level) {
        this.level = level;
        recalcStats();
    }

    public int takeDamage(int dmg) {
        int remainingDamage = dmg;

        if (defense > 0) {
            if (defense >= remainingDamage) {
                defense -= remainingDamage;
                remainingDamage = 0;
            } else {
                remainingDamage -= defense;
                defense = 0;
            }
        }

        hp -= remainingDamage;
        if (hp < 0) 
            hp = 0;

        return dmg;
    }

    public void usePotion() {
        if (potionCount > 0) {
            int missingHp = maxHp - hp;
            int healed = (int)(missingHp * 0.30);
            setHp(hp + healed);
            System.out.println(BLUE + name + RESET + " uses " + PURPLE + "HP Potion" + RESET + "! Restores " + GREEN + healed + " HP" + RESET + "!");
            potionCount--;
            System.out.println(BLUE + name + RESET + " has " + GREEN + hp + " HP remaining!" + RESET);
            System.out.println(YELLOW + "Potions left: " + potionCount + RESET);
        } else {
            System.out.println(RED + "No potions left!" + RESET);
        }
    }

    public boolean isSkillAvailable(int skillNumber) {
        return skillCooldowns[skillNumber - 1] == 0;
    }

    public void setSkillCooldown(int skillNumber, int turns) {
        skillCooldowns[skillNumber - 1] = turns;
    }

    public void reduceCooldowns() {
        for (int i = 0; i < skillCooldowns.length; i++) {
            if (skillCooldowns[i] > 0) skillCooldowns[i]--;
        }
    }

    public void resetCooldowns() {
        for (int i = 0; i < skillCooldowns.length; i++) {
            skillCooldowns[i] = 0;
        }
    }

    public int getXpReward() {
        return baseExp;
    }

    public int getGoldReward() {
        if (level >= 1 && level <= 3) return random.nextInt(5 - 2 + 1) + 2;
        else if (level >= 4 && level <= 6) return random.nextInt(10 - 5 + 1) + 5;
        else if (level >= 7 && level <= 10) return random.nextInt(18 - 10 + 1) + 10;
        else return 5;
    }

    public void restoreStats() {
        setHp(getMaxHp());
        setDefense(getMaxDefense());
    }

    public int loseRandomExp() {
        double percent = 0.05 + (Math.random() * 0.05);

        int loss = Math.max(1, (int)(currentXp * percent));
        currentXp -= loss;

        if (currentXp < 0) currentXp = 0;

        System.out.println("Lost " + loss + " XP (" + (int)(percent * 100) + "%)");

        return loss;
    }

    public abstract int useSkill(int skillNumber, Character enemy);
    public abstract void displaySkills();
    public abstract String getSkillName(int skillNumber);
}