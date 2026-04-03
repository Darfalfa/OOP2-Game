public abstract class Characters{

    private String name;
    private int health;
    private int damage;

    public Characters(String name, int health, int damage) {
        this.name = name;
        this.health = health;
        this.damage = damage;
    }

    public int getHealth() {
        return health;
    }

    public void takeDamage(int dmg) {
        health -= dmg;
        if (health < 0) {
            health = 0;
        }
    }

    public String getName() {
        return name;
    }

    public abstract void displaySkills();

    public abstract int useSkill(int choice);
}