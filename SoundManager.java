import java.io.File;
import javax.sound.sampled.*;

public class SoundManager {

    private static Clip currentBgm;
    private static float musicVolume = 0.8f;
    private static float sfxVolume = 0.7f;

    public static void playBgm(String fileName) {
        try {
            stopBgm();

            File soundFile = new File("sounds/" + fileName);
            AudioInputStream audio = AudioSystem.getAudioInputStream(soundFile);

            currentBgm = AudioSystem.getClip();
            currentBgm.open(audio);
            applyVolume(currentBgm, musicVolume);
            currentBgm.loop(Clip.LOOP_CONTINUOUSLY);
            currentBgm.start();

        } catch (Exception e) {
            System.err.println("Error playing BGM: " + fileName);
            e.printStackTrace();
        }
    }

    public static void stopBgm() {
        if (currentBgm != null) {
            currentBgm.stop();
            currentBgm.close();
            currentBgm = null;
        }
    }

    public static void playSfx(String fileName) {
        try {
            File soundFile = new File("sounds/" + fileName);
            AudioInputStream audio = AudioSystem.getAudioInputStream(soundFile);

            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            applyVolume(clip, sfxVolume);
            clip.start();

        } catch (Exception e) {
            System.err.println("Error playing SFX: " + fileName);
            e.printStackTrace();
        }
    }

    public static void setMusicVolume(float volume) {
        musicVolume = Math.max(0f, Math.min(1f, volume));

        if (currentBgm != null) {
            applyVolume(currentBgm, musicVolume);
        }
    }

    public static void setSfxVolume(float volume) {
        sfxVolume = Math.max(0f, Math.min(1f, volume));
    }

    private static void applyVolume(Clip clip, float volume) {
        if (clip == null) return;

        try {
            FloatControl gainControl =
                (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            if (volume <= 0f) {
                gainControl.setValue(gainControl.getMinimum());
            } else {
                float dB = (float)(20.0 * Math.log10(volume));
                gainControl.setValue(Math.max(gainControl.getMinimum(), dB));
            }

        } catch (Exception e) {
            System.err.println("Volume control not supported.");
        }
    }
}