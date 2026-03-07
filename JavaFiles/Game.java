import java.util.Scanner;

public class Game {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.println("Choose a character!");
        System.out.println("1: Range");
        System.out.println("2: Mage");
        System.out.println("3: Melee");

        int charChoice = sc.nextInt();

        Hero hero = new Hero("Ranger",100,5);

        System.out.println("\nYou have chosen Ranger!");
        System.out.println("Health: 100");
        System.out.println("Damage: 5");

        hero.displaySkills();

        System.out.println("\nChoose a monster (shadow dweller, evil cultist, armored ghost)");
        sc.nextLine();
        String monsterName = sc.nextLine();

        Monsters monster = new Monsters("Shadow Dweller",80,10);

        System.out.println("\nEnter: Shadow Dweller");
        System.out.println("Health: 80");
        System.out.println("Damage: 10");
        monster.displaySkills();

        int round = 1;

        while(hero.getHealth() > 0 && monster.getHealth() > 0){

            System.out.println("\n======= Round "+round+" =======");

            System.out.println("Choose a move!");
            System.out.println("1: Skill");
            System.out.println("2: Inventory");
            System.out.print("Enter choice: ");

            int move = sc.nextInt();

            if(move == 1){

                hero.displaySkills();
                System.out.print("Choose Skill: ");
                int skill = sc.nextInt();

                int damage = hero.useSkill(skill);

                if(damage == -1){
                    continue;
                }

                System.out.println("\nRanger uses skill on Shadow Dweller");

                monster.takeDamage(damage);

                System.out.println("Shadow Dweller:");
                System.out.println("Health: "+monster.getHealth());

                if(monster.getHealth() <= 0){
                    break;
                }

                int mSkill = monster.randomSkill();
                int mDamage = monster.useSkill(mSkill);

                System.out.println("\nShadow Dweller attacks Ranger");

                hero.takeDamage(mDamage);

                System.out.println("Ranger:");
                System.out.println("Health: "+hero.getHealth());
            }

            hero.reduceCooldowns();
            round++;
        }

        if(hero.getHealth() > 0){
            System.out.println("\nRanger defeated a Shadow Dweller");
        }else{
            System.out.println("\nRanger was defeated");
        }

        sc.close();
    }
}