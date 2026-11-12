package chronorunner.gfx;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;

/** Shared transparent sprite effects; their geometry never causes damage. */
public final class SwordEffects {
    public enum Kind { CRESCENT, LONG_SLASH, CROSS_BURST, SMOKE }
    private static final File ROOT = new File("assets/effects/phan_sword/v1");
    private static final Properties META = metadata();
    private static final Map<Kind, BufferedImage[]> FRAMES = new EnumMap<>(Kind.class);

    private SwordEffects() { }

    private static Properties metadata() {
        Properties p = new Properties();
        try (var input = Files.newInputStream(new File(ROOT,"effects.properties").toPath())) {
            p.load(input);
            return p;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load sword effect metadata", e);
        }
    }

    private static BufferedImage[] frames(Kind kind) {
        return FRAMES.computeIfAbsent(kind, k -> {
            String name = k.name().toLowerCase(Locale.ROOT);
            int count = Integer.parseInt(META.getProperty(name+".frames"));
            int width = Integer.parseInt(META.getProperty(name+".frameWidth"));
            int height = Integer.parseInt(META.getProperty(name+".frameHeight"));
            if (count<1 || count>32 || width<1 || height<1) throw new IllegalStateException("Invalid effect geometry: "+name);
            BufferedImage[] result = new BufferedImage[count];
            try {
                for (int i=0;i<count;i++) {
                    result[i] = ImageIO.read(new File(ROOT,name+"_"+(i+1)+".png"));
                    if (result[i]==null || result[i].getWidth()!=width || result[i].getHeight()!=height)
                        throw new IllegalStateException("Invalid effect frame: "+name+"/"+(i+1));
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot load sword effect: "+name,e);
            }
            return result;
        });
    }

    public static int count(Kind kind) { return frames(kind).length; }

    public static void validateAll() { for (Kind kind:Kind.values()) frames(kind); }

    /** A stable centre pivot, uniform scale, real alpha and nearest sampling. */
    public static void draw(Kind kind, int frame, Graphics2D g, double x, double y,
                            double scale, boolean flip, float alpha) {
        if (scale<=0 || alpha<=0) return;
        BufferedImage[] bank=frames(kind);
        BufferedImage image=bank[Math.floorMod(frame,bank.length)];
        int width=Math.max(1,(int)Math.round(image.getWidth()*scale));
        int height=Math.max(1,(int)Math.round(image.getHeight()*scale));
        int left=(int)Math.round(x-width/2.0), top=(int)Math.round(y-height/2.0);
        Graphics2D effect=(Graphics2D)g.create();
        effect.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        effect.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        effect.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.min(1,alpha)));
        effect.drawImage(image,flip?left+width:left,top,flip?left:left+width,top+height,
                0,0,image.getWidth(),image.getHeight(),null);
        effect.dispose();
    }
}
