package chronorunner.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/** Reads and writes {@code saves/progress.properties}. Fails soft. */
public final class SaveManager {

    private static final Path SAVE_DIR = Paths.get("saves");
    private static final Path SAVE_FILE = SAVE_DIR.resolve("progress.properties");

    private SaveManager() {
    }

    public static Progress load() {
        Progress progress = new Progress();
        if (!Files.exists(SAVE_FILE)) {
            return progress;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(SAVE_FILE)) {
            props.load(in);
            progress.unlockThrough(Integer.parseInt(props.getProperty("unlockedChapters", "1")));
            progress.setScreenShake(Boolean.parseBoolean(props.getProperty("screenShake", "true")));
        } catch (IOException | NumberFormatException e) {
            System.err.println("Could not read save file, starting fresh: " + e.getMessage());
        }
        return progress;
    }

    public static void save(Progress progress) {
        Properties props = new Properties();
        props.setProperty("unlockedChapters", String.valueOf(progress.getUnlockedChapters()));
        props.setProperty("screenShake", String.valueOf(progress.isScreenShake()));
        try {
            Files.createDirectories(SAVE_DIR);
            try (OutputStream out = Files.newOutputStream(SAVE_FILE)) {
                props.store(out, "Chrono Runner progress");
            }
        } catch (IOException e) {
            System.err.println("Could not write save file: " + e.getMessage());
        }
    }
}
