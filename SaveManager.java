import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SaveManager {

    private static final File SAVE_DIR = new File("saves");
    private static final File LEADERBOARD_FILE = new File(SAVE_DIR, "leaderboard.txt");

    public static class SaveRecord {
        public String name;
        public String character;
        public int level;
        public int monstersKilled;

        public SaveRecord(String name, String character, int level, int monstersKilled) {
            this.name = name;
            this.character = cleanCharacter(character);
            this.level = level;
            this.monstersKilled = monstersKilled;
        }
    }

    public static List<SaveRecord> loadLeaderboard() {
        ensureSaveFile();

        List<SaveRecord> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(LEADERBOARD_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                SaveRecord record = parseRecord(line);
                if (record != null) {
                    records.add(record);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read leaderboard save file.");
        }

        records.sort(Comparator
                .comparingInt((SaveRecord r) -> r.level).reversed()
                .thenComparingInt((SaveRecord r) -> r.monstersKilled).reversed()
                .thenComparing(r -> r.name.toLowerCase()));

        return records;
    }

    public static SaveRecord loadPlayer(String name) {
        String cleanName = cleanName(name);
        for (SaveRecord record : loadLeaderboard()) {
            if (record.name.equalsIgnoreCase(cleanName)) {
                return record;
            }
        }
        return new SaveRecord(cleanName, "Unknown", 1, 0);
    }

    public static void savePlayer(String name, int level, int monstersKilled) {
        SaveRecord existing = loadPlayer(name);
        savePlayer(name, existing.character, level, monstersKilled);
    }

    public static void savePlayer(String name, String character, int level, int monstersKilled) {
        String cleanName = cleanName(name);
        String cleanCharacter = cleanCharacter(character);
        List<SaveRecord> records = loadLeaderboard();
        boolean updated = false;

        for (SaveRecord record : records) {
            if (record.name.equalsIgnoreCase(cleanName)) {
                record.name = cleanName;
                if (!"Unknown".equalsIgnoreCase(cleanCharacter)) {
                    record.character = cleanCharacter;
                }
                record.level = Math.max(record.level, level);
                record.monstersKilled = Math.max(record.monstersKilled, monstersKilled);
                updated = true;
                break;
            }
        }

        if (!updated) {
            records.add(new SaveRecord(cleanName, cleanCharacter, level, monstersKilled));
        }

        writeLeaderboard(records);
    }

    public static boolean nameExists(String name) {
        String cleanName = cleanName(name);
        for (SaveRecord record : loadLeaderboard()) {
            if (record.name.equalsIgnoreCase(cleanName)) {
                return true;
            }
        }
        return false;
    }

    public static String cleanName(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            clean = "Player";
        }
        return clean.replace("|", "").replace("\n", " ").replace("\r", " ");
    }

    public static String cleanCharacter(String character) {
        String clean = character == null ? "" : character.trim();
        if (clean.isEmpty()) {
            clean = "Unknown";
        }
        return clean.replace("|", "").replace("\n", " ").replace("\r", " ");
    }

    private static SaveRecord parseRecord(String line) {
        String[] parts = line.split("\\|");

        try {
            if (parts.length == 4) {
                return new SaveRecord(parts[0], parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            }
            if (parts.length == 3) {
                return new SaveRecord(parts[0], "Unknown", Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    private static void writeLeaderboard(List<SaveRecord> records) {
        ensureSaveFile();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(LEADERBOARD_FILE, false))) {
            for (SaveRecord record : records) {
                bw.write(record.name + "|" + cleanCharacter(record.character) + "|" + record.level + "|" + record.monstersKilled);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not write leaderboard save file.");
        }
    }

    private static void ensureSaveFile() {
        if (!SAVE_DIR.exists()) {
            SAVE_DIR.mkdirs();
        }

        if (!LEADERBOARD_FILE.exists()) {
            try {
                LEADERBOARD_FILE.createNewFile();
            } catch (IOException e) {
                System.err.println("Could not create leaderboard save file.");
            }
        }
    }
}
