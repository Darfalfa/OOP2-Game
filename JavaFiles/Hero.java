import java.util.HashMap;

public class Hero extends Characters {

    private HashMap<Integer, Integer> cooldown = new HashMap<>();
    private HashMap<Integer, Integer> currentCooldown = new HashMap<>();

    public Hero(String name, int health, int damage) {
        super(name, health, damage);

        cooldown.put(1, 0);
        cooldown.put(2, 2);
        cooldown.put(3, 4);
        cooldown.put(4, 5);

        for(int i=1;i<=4;i++){
            currentCooldown.put(i,0);
        }
    }

    @Override
    public void displaySkills() {
        System.out.println("(1)Basic Attack (20 Damage) - no cooldown");
        System.out.println("(2)Skill 1: Chasing Arrow (30 Damage) - 2 round cooldown");
        System.out.println("(3)Skill 2: Metal Rain (40 Damage) - 4 round cooldown");
        System.out.println("(4)Skill 3: Sagittarius Punishment (50 Damage) - 5 round cooldown");
    }

    @Override
    public int useSkill(int choice) {

        if(currentCooldown.get(choice) > 0){
            System.out.println("Skill " + (choice-1) + " is in cooldown!");
            return -1;
        }

        int damage = 0;

        switch(choice){
            case 1: damage = 20; break;
            case 2: damage = 30; break;
            case 3: damage = 40; break;
            case 4: damage = 50; break;
        }

        currentCooldown.put(choice, cooldown.get(choice));

        return damage;
    }

    public void reduceCooldowns(){
        for(int i=1;i<=4;i++){
            if(currentCooldown.get(i) > 0){
                currentCooldown.put(i, currentCooldown.get(i)-1);
            }
        }
    }
}