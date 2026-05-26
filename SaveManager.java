import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public class SaveManager {

    private static final File SAVE_DIR = new File("saves");
    private static final File LEADERBOARD_FILE = new File(SAVE_DIR, "leaderboard.txt");

    public static class SaveRecord {
        public String name;
        public String character;
        public long timeFinishedMillis;
        public int level;
        public int monstersKilled;

        public SaveRecord(String name, String character, int level, int monstersKilled) {
            this(name, character, 0L, level, monstersKilled);
        }

        public SaveRecord(String name, String character, long timeFinishedMillis, int level, int monstersKilled) {
            this.name = name;
            this.character = cleanCharacter(character);
            this.timeFinishedMillis = Math.max(0L, timeFinishedMillis);
            this.level = level;
            this.monstersKilled = monstersKilled;
        }
    }

    public static class GameSave {
        public String name = "Player";
        public String character = "Unknown";
        public int level = 1;
        public int monstersKilled = 0;
        public int world = 1;
        public boolean inDungeon = false;
        public int dungeon = 0;
        public int playerX = 0;
        public int playerY = 0;
        public int hp = 0;
        public int defense = 0;
        public int currentXp = 0;
        public int gold = 0;
        public int healthPotion = 0;
        public int expPotion = 0;
        public long elapsedMillis = 0L;
        public int puzzlePieceCount = 0;
        public boolean[] puzzlePieces = new boolean[4];
        public boolean[] storyTriggered = new boolean[11];
        public boolean[] khaiDialogueTriggered = new boolean[10];
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

        records.sort(SaveManager::compareRecords);

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
        savePlayer(name, character, 0L, level, monstersKilled);
    }

    public static void savePlayer(String name, String character, long timeFinishedMillis, int level, int monstersKilled) {
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
                if (timeFinishedMillis > 0 &&
                        (record.timeFinishedMillis == 0 || timeFinishedMillis < record.timeFinishedMillis)) {
                    record.timeFinishedMillis = timeFinishedMillis;
                }
                record.level = Math.max(record.level, level);
                record.monstersKilled = Math.max(record.monstersKilled, monstersKilled);
                updated = true;
                break;
            }
        }

        if (!updated) {
            records.add(new SaveRecord(cleanName, cleanCharacter, timeFinishedMillis, level, monstersKilled));
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

    public static List<GameSave> loadGameSaves() {
        ensureSaveFile();
        List<GameSave> saves = new ArrayList<>();
        File[] files = SAVE_DIR.listFiles((dir, fileName) -> fileName.endsWith(".progress"));
        if (files == null) return saves;

        for (File file : files) {
            GameSave save = loadGameSaveFile(file);
            if (save != null) {
                saves.add(save);
            }
        }

        saves.sort(Comparator.comparing(s -> s.name.toLowerCase()));
        return saves;
    }

    public static GameSave loadGameSave(String name) {
        File file = progressFile(name);
        if (!file.exists()) return null;
        return loadGameSaveFile(file);
    }

    public static void saveGameProgress(GameSave save) {
        ensureSaveFile();
        if (save == null) return;

        save.name = cleanName(save.name);
        save.character = cleanCharacter(save.character);
        savePlayer(save.name, save.character, save.level, save.monstersKilled);

        Properties props = new Properties();
        props.setProperty("name", save.name);
        props.setProperty("character", save.character);
        props.setProperty("level", String.valueOf(save.level));
        props.setProperty("monstersKilled", String.valueOf(save.monstersKilled));
        props.setProperty("world", String.valueOf(save.world));
        props.setProperty("inDungeon", String.valueOf(save.inDungeon));
        props.setProperty("dungeon", String.valueOf(save.dungeon));
        props.setProperty("playerX", String.valueOf(save.playerX));
        props.setProperty("playerY", String.valueOf(save.playerY));
        props.setProperty("hp", String.valueOf(save.hp));
        props.setProperty("defense", String.valueOf(save.defense));
        props.setProperty("currentXp", String.valueOf(save.currentXp));
        props.setProperty("gold", String.valueOf(save.gold));
        props.setProperty("healthPotion", String.valueOf(save.healthPotion));
        props.setProperty("expPotion", String.valueOf(save.expPotion));
        props.setProperty("elapsedMillis", String.valueOf(Math.max(0L, save.elapsedMillis)));
        props.setProperty("puzzlePieceCount", String.valueOf(save.puzzlePieceCount));
        props.setProperty("puzzlePieces", joinBooleans(save.puzzlePieces));
        props.setProperty("storyTriggered", joinBooleans(save.storyTriggered));
        props.setProperty("khaiDialogueTriggered", joinBooleans(save.khaiDialogueTriggered));

        try (FileWriter writer = new FileWriter(progressFile(save.name))) {
            props.store(writer, "Great Ruins of Khai progress save");
        } catch (IOException e) {
            System.err.println("Could not write progress save.");
        }
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
            if (parts.length == 5) {
                return new SaveRecord(parts[0], parts[1], Long.parseLong(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
            }
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
                bw.write(record.name + "|" + cleanCharacter(record.character) + "|" + record.timeFinishedMillis + "|" + record.level + "|" + record.monstersKilled);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not write leaderboard save file.");
        }
    }

    private static GameSave loadGameSaveFile(File file) {
        Properties props = new Properties();
        try (FileReader reader = new FileReader(file)) {
            props.load(reader);
        } catch (IOException e) {
            return null;
        }

        GameSave save = new GameSave();
        save.name = cleanName(props.getProperty("name", "Player"));
        save.character = cleanCharacter(props.getProperty("character", "Unknown"));
        save.level = intProp(props, "level", 1);
        save.monstersKilled = intProp(props, "monstersKilled", 0);
        save.world = intProp(props, "world", 1);
        save.inDungeon = Boolean.parseBoolean(props.getProperty("inDungeon", "false"));
        save.dungeon = intProp(props, "dungeon", 0);
        save.playerX = intProp(props, "playerX", 0);
        save.playerY = intProp(props, "playerY", 0);
        save.hp = intProp(props, "hp", 0);
        save.defense = intProp(props, "defense", 0);
        save.currentXp = intProp(props, "currentXp", 0);
        save.gold = intProp(props, "gold", 0);
        save.healthPotion = intProp(props, "healthPotion", 0);
        save.expPotion = intProp(props, "expPotion", 0);
        save.elapsedMillis = longProp(props, "elapsedMillis", 0L);
        save.puzzlePieceCount = intProp(props, "puzzlePieceCount", 0);
        readBooleans(props.getProperty("puzzlePieces", ""), save.puzzlePieces);
        readBooleans(props.getProperty("storyTriggered", ""), save.storyTriggered);
        readBooleans(props.getProperty("khaiDialogueTriggered", ""), save.khaiDialogueTriggered);
        return save;
    }

    private static File progressFile(String name) {
        return new File(SAVE_DIR, safeFileName(cleanName(name)) + ".progress");
    }

    private static String safeFileName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static int intProp(Properties props, String key, int fallback) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long longProp(Properties props, String key, long fallback) {
        try {
            return Long.parseLong(props.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int compareRecords(SaveRecord a, SaveRecord b) {
        boolean aFinished = a.timeFinishedMillis > 0;
        boolean bFinished = b.timeFinishedMillis > 0;

        if (aFinished && bFinished) {
            int byTime = Long.compare(a.timeFinishedMillis, b.timeFinishedMillis);
            if (byTime != 0) return byTime;
        } else if (aFinished != bFinished) {
            return aFinished ? -1 : 1;
        }

        int byLevel = Integer.compare(b.level, a.level);
        if (byLevel != 0) return byLevel;

        int byKills = Integer.compare(b.monstersKilled, a.monstersKilled);
        if (byKills != 0) return byKills;

        return a.name.compareToIgnoreCase(b.name);
    }

    public static String formatTime(long millis) {
        if (millis <= 0) return "--";

        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static String joinBooleans(boolean[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    private static void readBooleans(String value, boolean[] target) {
        String[] parts = value.split(",");
        for (int i = 0; i < target.length && i < parts.length; i++) {
            target[i] = Boolean.parseBoolean(parts[i].trim());
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
