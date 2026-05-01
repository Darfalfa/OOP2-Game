import java.util.HashMap;
import java.util.Map;

public class StoryManager {

    private Map<String, String> stories;

    public StoryManager() {
        stories = new HashMap<>();
        loadStories();
    }

    private void loadStories() {

        stories.put("INTRO",
                "Long before the world fell into ruin, three great cities stood as pillars of balance.\n\n" +
                        "Lunaris, Lumenaria, and Valdrath thrived in peace and power.\n\n" +
                        "But a mysterious system was discovered by a scholar named Khai.\n\n" +
                        "When it was activated, the system declared corruption.\n\n" +
                        "A cataclysm began.\n\n" +
                        "Monsters emerged. Cities fell. Civilization was lost.\n\n" +
                        "Fragments of the system scattered across the world.\n\n" +
                        "Khai collapsed... and something else took control."
        );

        stories.put("WORLD_1_START",
                "The ruins of Lunaris lie ahead.\n\n" +
                        "Once a city of eternal night, it is now consumed by endless darkness.\n\n" +
                        "Shadows move without form, and fear lingers in silence.\n\n" +
                        "The first system fragments are hidden within."
        );

        stories.put("WORLD_2_START",
                "The ruler of shadows has fallen.\n\n" +
                        "The darkness weakens, but it does not disappear.\n\n" +
                        "The recovered fragments reveal traces of the system.\n\n" +
                        "Something is watching... and guiding the path forward.\n\n\n" +
                        "The path leads to Lumenaria.\n\n" +
                        "Once a sacred city of faith and music, it is now the Cursed Domain.\n\n" +
                        "Rituals have replaced prayer, and corruption runs deep.\n\n" +
                        "The system’s influence grows stronger here."
        );

        stories.put("WORLD_3_START",
                "The source of corruption in Lumenaria is destroyed.\n\n" +
                        "But the truth begins to unfold.\n\n" +
                        "The system was not only activated — it was designed.\n\n" +
                        "The fragments reveal the existence of a hidden controller.\n\n" +
                        "The final ruins lie within Valdrath.\n\n" +
                        "Once the greatest fortress, it is now the Haunted Armory.\n\n" +
                        "Fallen warriors continue to fight, bound by unseen forces.\n\n" +
                        "The final fragments await."
        );

        stories.put("FINAL_BOSS_BEFORE",
                "The system fragments are nearly complete.\n\n" +
                        "Memories begin to return.\n\n" +
                        "Khai did not disappear.\n\n" +
                        "He became part of the system.\n\n" +
                        "The one guiding this world is the Administrator."
        );

        stories.put("FINAL_BOSS_AFTER",
                "The system reaches its final state.\n\n" +
                        "This world was never just destruction.\n\n" +
                        "It was a test.\n\n" +
                        "A perfect artificial world designed to measure human will.\n\n" +
                        "The final choice now remains."
        );

        stories.put("ENDING_RESTORE",
                "The remaining system power restores the world.\n\n" +
                        "Monsters disappear. Cities rise again.\n\n" +
                        "The system shuts down peacefully.\n\n" +
                        "The world is saved.\n\n" +
                        "But the memory of the ruins remains."
        );

        stories.put("ENDING_REPLACE",
                "The system accepts a new ruler.\n\n" +
                        "The world remains in ruin.\n\n" +
                        "Monsters continue to exist.\n\n" +
                        "A new controller rises.\n\n" +
                        "The cycle begins again."
        );
    }

    public String getStory(String storyId) {
        return stories.getOrDefault(storyId, "");
    }
}