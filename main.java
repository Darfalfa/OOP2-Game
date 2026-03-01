import java.util.Scanner;
public class main {
   static int level = 1;
   static int currentExp = 0;
   static int maxExp = 100; // EXP needed to level up

   // USER DETAILS
   int userHealth;
   int userDamage;
   
    


   



   public static void main(String[] args) {
       Scanner sc = new Scanner(System.in);
    //    subclass obj = new subclass();
       monsters m  = new monsters();
       skills s = new skills();
       int userCharChoice;
    
      
       while (true) {
           System.out.println("Choose a character!");
           System.out.println("1: Range");
           System.out.println("2: Mage");
           System.out.println("3: Melee");
           System.out.print("Enter character: ");
           userCharChoice = sc.nextInt();
           sc.nextLine();
           if(userCharChoice == 1){
                
           }



           System.out.println("\nLevel: " + level);
           System.out.println("EXP: " + currentExp + " / " + maxExp);
           System.out.println("1. Continue");
           System.out.println("2. Exit");
           System.out.print("Choose: ");
           int choice = sc.nextInt();

           if (choice == 1) {
                System.out.print("Enter monster: ");
                int monsterSelection = sc.nextInt();
                if(monsterSelection == 1){
                    m.shadowDweller();
                } else if(monsterSelection == 2){
                    m.evilCultist();
                } else if(monsterSelection == 3){
                    m.armoredGhost();
                } else {
                    System.out.println("Invalid Character!");
                }

           } 
           
       }
    



   }

}