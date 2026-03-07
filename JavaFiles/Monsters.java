import java.util.Random;

public class Monsters extends Characters {

    public Monsters(String name, int health, int damage) {
        super(name, health, damage);
    }

    @Override
    public void displaySkills() {
        System.out.println("Basic Attack (10 Damage) - no cooldown");
        System.out.println("Skill 1: Shadow Assassination (20 Damage) - 2 round cooldown");
        System.out.println("Skill 2: Shadow Severing (30 Damage) - 4 round cooldown");
        System.out.println("Skill 3: Absolute Darkness (50 Damage) - 5 round cooldown");
    }

    @Override
    public int useSkill(int choice) {

        switch(choice){
            case 1: return 10;
            case 2: return 20;
            case 3: return 30;
            case 4: return 50;
        }

        return 10;
    }

    public int randomSkill(){
        Random rand = new Random();
        return rand.nextInt(4)+1;
    }
}