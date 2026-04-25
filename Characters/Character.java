package Characters;

import java.util.Random;

abstract public class Character {
    public Random random = new Random();

    private String name;
    private int[] skillCooldowns;
    private int hp, maxHp, baseHp;
    private int defense; //defense during gameplay 
    private int maxDefense; //max defense at the current level
    private int baseDefense; //starting defense - does not change
    private int level;
    private int baseExp;
    private int currentXp = 0;
    private int nextLevelXp;
    private int gold;
    private int healthPotion = 0;
    private int expPotion = 0;

    public Character(String name, int hp, int defense, int baseExp, int level) {
        this.name = name;
        this.baseHp = hp;
        this.baseDefense = defense;
        this.baseExp = baseExp;
        this.level = level;
        recalcStats();
        this.skillCooldowns = new int[3];
    }

    //SETTERS

    public void setExpPotion(int val) {
        expPotion = val;
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

    public void setHealthPotion(int val) {
        healthPotion = val;
    }

    public void setLevel(int level) {
        this.level = level;
        recalcStats();
    }

    //GETTERS

    public int getGold() {
        return gold;
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

    public int getCurrentXp() {
        return currentXp;
    }

    public int getNextLevelXp() {
        return nextLevelXp;
    }

    public String getName() {
        return name;
    }

    public int getSkillCooldown(int skillNumber) {
        return skillCooldowns[skillNumber - 1];
    }

    public int[] getSkillCooldowns() {
        return skillCooldowns;
    }

    public int getHealthPotion() {
        return healthPotion;
    }

    public int getXpReward() {
        return baseExp;
    }

    public int getExpPotion() {
        return expPotion;
    }

    public int getGoldReward() {
        if (level >= 1 && level <= 3) 
            return random.nextInt(5 - 2 + 1) + 2;
        else if (level >= 4 && level <= 6) 
            return random.nextInt(10 - 5 + 1) + 5;
        else if (level >= 7 && level <= 10) 
            return random.nextInt(18 - 10 + 1) + 10;
        else 
            return 5;
    }

    //LOGICS
    public void recalcStats() {
        this.maxHp = (int)(baseHp * (1 + 0.08 * (level - 1)));
        this.hp = maxHp;
        this.maxDefense = (int)(baseDefense * (1 + 0.05 * (level - 1)));
        this.defense = maxDefense;
        this.nextLevelXp = (int)(baseExp * Math.pow(level, 1.5));
    } 

    public void gainXp(int amount) {
        currentXp += amount;
        
        while (currentXp >= nextLevelXp) {
            currentXp -= nextLevelXp;
            levelUp();
        }
    }

    public void receiveBattleRewards(int xp, int goldReward) {
        gainXp(xp);

        gold += goldReward;
    }

    public void addGold(int amount) {
        gold += amount;
        if (gold < 0) gold = 0;
    }

    public void levelUp() {
        level++;
        recalcStats();
        hp = maxHp;
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

    public void restoreStats() {
        setHp(getMaxHp());
        setDefense(getMaxDefense());
    }

    public int loseRandomExp() {
        double percent = 0.05 + (Math.random() * 0.05);

        int loss = Math.max(1, (int)(currentXp * percent));
        currentXp -= loss;

        if (currentXp < 0) currentXp = 0;

        return loss;
    }

    public String useHealthPotion() {

        if (healthPotion > 0) {
            int missingHp = maxHp - hp;
            int healAmount = (int)(missingHp * 0.30);

            setHp(Math.min(hp + healAmount, maxHp));

            healthPotion--;

            return name + " uses HP Potion!"
                    + "\nRestored " + healAmount + " HP."
                    + "\nPotions left: " + healthPotion;

        } else {
            return "No HP potions left!";
        }
    }

    public String useExpPotion() {

        if (expPotion > 0) {

            int gained = (int)(getNextLevelXp() * (0.15 + Math.random() * 0.10));

            gainXp(gained);
            expPotion--;

            return name + " uses EXP Potion! Gained " + gained + " EXP.\n"
                    + "Potions left: " + expPotion;

        } else {
            return "No EXP potions left!";
        }
    }

    public abstract String getSkillDamageRange(int skillNumber);
    public abstract String getBackgroundInfo();
    public abstract int useSkill(int skillNumber, Character enemy);
    public abstract String getSkillName(int skillNumber);
}