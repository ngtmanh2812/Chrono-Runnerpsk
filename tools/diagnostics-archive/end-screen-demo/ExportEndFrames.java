import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import chronorunner.animation.AnimationController;
import chronorunner.combat.DamageInfo;
import chronorunner.core.Camera;
import chronorunner.core.Game;
import chronorunner.core.GameState;
import chronorunner.entity.Player;
import chronorunner.entity.boss.Boss;
import chronorunner.ui.PlayScreen;
import chronorunner.world.Level;

/** Generates preview frames using the existing renderer. No game source/save edits. */
public class ExportEndFrames {
    static Field field(Object target, String name) throws Exception {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try { Field f=c.getDeclaredField(name); f.setAccessible(true); return f; }
            catch(NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    static Object get(Object target, String name) throws Exception { return field(target,name).get(target); }

    static void capture(String state) throws Exception {
        boolean won=state.equals("victory");
        Game game=new Game();
        game.getSession().chapter=1;
        game.getSession().playerCount=1;
        Method register=Game.class.getDeclaredMethod("registerScreens"); register.setAccessible(true); register.invoke(game);
        game.setState(GameState.PLAYING);
        PlayScreen screen=(PlayScreen)game.screen(GameState.PLAYING);
        Level level=(Level)get(screen,"level");
        Player player=screen.players().get(0);
        Boss boss=level.boss();
        player.x=670; player.y=level.groundY()-player.height; player.grounded=true;
        boss.x=1150; boss.y=level.groundY()-boss.height; boss.grounded=true;
        field(boss,"facing").setInt(boss,-1);
        player.update(0,screen);
        if (state.equals("pause")) {
            player.takeDamage(new DamageInfo(28,0,0,false));
            player.update(0.7,screen);
            boss.takeDamage(new DamageInfo((int)(boss.getMaxHealth()*0.42),0,0,true));
            boss.update(0,screen);
            field(boss,"hurtFlash").setDouble(boss,0);
            field(boss,"stun").setDouble(boss,0);
            ((AnimationController)get(boss,"anim")).play("idle");
        } else if (won) {
            player.takeDamage(new DamageInfo(64,0,0,false));
            player.update(0.7,screen);
            boss.takeDamage(new DamageInfo(boss.getMaxHealth(),0,0,true));
        } else {
            boss.takeDamage(new DamageInfo((int)(boss.getMaxHealth()*0.63),0,0,true));
            boss.update(0,screen);
            field(boss,"hurtFlash").setDouble(boss,0);
            field(boss,"stun").setDouble(boss,0);
            AnimationController animation=(AnimationController)get(boss,"anim");
            animation.play("attack"); animation.update(0.1);
            player.takeDamage(new DamageInfo(player.getHealth(),0,0,false));
            player.update(0.42,screen);
        }
        Camera camera=(Camera)get(screen,"camera");
        camera.snapTo(1000,Game.HEIGHT/2.0,level.width(),level.height());
        // Render at the finishing hit, before the current game's end overlay.
        // The demo adds its own end screen on top of this untouched arena frame.
        BufferedImage frame=new BufferedImage(Game.WIDTH,Game.HEIGHT,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=frame.createGraphics(); screen.render(g); g.dispose();
        String stem="out/end-screen-demo/"+state+"-gameplay";
        ImageIO.write(frame,"png",new File(stem+".png"));
        ImageWriter writer=ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam settings=writer.getDefaultWriteParam();
        settings.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); settings.setCompressionQuality(0.68f);
        try(ImageOutputStream output=ImageIO.createImageOutputStream(new File(stem+".jpg"))) {
            writer.setOutput(output); writer.write(null,new javax.imageio.IIOImage(frame,null,null),settings);
        } finally { writer.dispose(); }
        System.out.println(state+": player HP "+player.getHealth()+", boss HP "+boss.getHealth()+", JPEG "+new File(stem+".jpg").length()+" bytes");
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("chronorunner.debugCollision","false");
        System.setProperty("chronorunner.godMode","false");
        if(args.length>0) capture(args[0]);
        else { capture("game-over"); capture("victory"); capture("pause"); }
    }
}
