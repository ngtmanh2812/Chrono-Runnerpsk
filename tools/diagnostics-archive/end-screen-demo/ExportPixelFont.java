import chronorunner.gfx.PixelFont;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/** Preview-only export. Does not change game sources or assets. */
public class ExportPixelFont {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        PixelFont.has('A');
        Field field = PixelFont.class.getDeclaredField("GLYPHS");
        field.setAccessible(true);
        Map<Character, int[]> glyphs = new TreeMap<>((Map<Character, int[]>) field.get(null));
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Character, int[]> entry : glyphs.entrySet()) {
            if (!first) json.append(',');
            first = false;
            json.append('"').append((int) entry.getKey()).append("\":[");
            for (int i = 0; i < entry.getValue().length; i++) {
                if (i > 0) json.append(',');
                json.append(entry.getValue()[i]);
            }
            json.append(']');
        }
        json.append('}');
        Files.writeString(Path.of(args[0]), json, StandardCharsets.UTF_8);
        System.out.println("Exported " + glyphs.size() + " pixel glyphs for preview");
    }
}
