package chronorunner.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import chronorunner.core.Game;
import chronorunner.entity.Player;
import chronorunner.entity.boss.Boss;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelSprite;
import chronorunner.world.Level;

/** In-game overlay: boss health, phase, player health and skill cooldowns. */
public class Hud {

    public void render(Graphics2D g, Level level, List<Player> players) {
        g.setFont(UiKit.SMALL);
        g.setColor(UiKit.MUTED);
        g.drawString("CHAPTER " + level.chapter() + "  —  " + level.tile().placeName, 24, 28);

        Boss boss = level.boss();
        if (boss != null) {
            int barW = 560;
            int x = (Game.WIDTH - barW) / 2;
            int y = 62;
            UiKit.bar(g, x, y, barW, 22, boss.healthRatio(),
                    new Color(220, 70, 90), new Color(24, 16, 26, 210));
            g.setColor(new Color(255, 255, 255, 130));
            for (double marker : new double[]{0.7, 0.4}) {
                int mx = x + (int) (barW * marker);
                g.drawLine(mx, y, mx, y + 22);
            }
            UiKit.centered(g, boss.getName(), UiKit.H2, Color.WHITE,
                    Game.WIDTH / 2, y - 8);
            UiKit.centered(g, "PHASE " + boss.getPhase(), UiKit.SMALL, UiKit.MUTED,
                    x + barW - 44, y - 8);
        }

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int px = i == 0 ? 24 : Game.WIDTH - 324;
            int py = Game.HEIGHT - 98;
            // A separate thin strip leaves HP and the existing skill row intact.
            UiKit.panel(g, px, py - 26, 300, 22, new Color(8, 12, 26, 205), new Color(255, 255, 255, 45));
            Color guardColor = p.isDead() ? UiKit.MUTED : p.isBlocking() ? Color.WHITE : UiKit.ACCENT;
            g.setFont(UiKit.SMALL);
            g.setColor(guardColor);
            g.drawString("Block [" + p.getBlockKeyLabel() + "]", px + 14, py - 10);
            String guardStatus = p.isDead() ? "—" : p.isBlocking() ? "ĐANG ĐỠ"
                    : p.getBlockCooldownRemaining() > 0
                    ? String.format(java.util.Locale.ROOT, "%.1fs", p.getBlockCooldownRemaining()) : "SẴN SÀNG";
            g.drawString(guardStatus, px + 126, py - 10);
            UiKit.bar(g, px + 216, py - 18, 70, 10, p.isDead() ? 0 : p.getBlockCooldownRatio(),
                    guardColor, new Color(0, 0, 0, 160));
            UiKit.panel(g, px, py, 300, 74, new Color(8, 12, 26, 205), new Color(255, 255, 255, 45));

            Art.player(p.characterId()).frame(0)
                    .drawAnchored(g, px + 24, py + 32, 1, false, PixelSprite.Mode.NORMAL, 1f);

            g.setFont(UiKit.BODY);
            g.setColor(Color.WHITE);
            g.drawString("P" + (i + 1) + "  " + p.getName(), px + 42, py + 26);

            Color hpColor = p.healthRatio() > 0.5 ? new Color(80, 220, 120)
                    : p.healthRatio() > 0.25 ? new Color(240, 200, 70)
                    : new Color(230, 70, 80);
            UiKit.bar(g, px + 14, py + 34, 196, 16, p.healthRatio(), hpColor, new Color(0, 0, 0, 160));

            g.setFont(UiKit.SMALL);
            g.setColor(UiKit.MUTED);
            g.drawString(p.getHealth() + " / " + p.getMaxHealth(), px + 216, py + 47);
            g.drawString(p.getSkillName(), px + 14, py + 65);
            UiKit.bar(g, px + 216, py + 56, 70, 10, p.getSkillCooldownRatio(), UiKit.ACCENT, new Color(0, 0, 0, 160));

            if (p.isDead()) {
                g.setFont(UiKit.H2);
                g.setColor(UiKit.WARN);
                g.drawString("DOWN", px + 224, py + 26);
            }
        }
    }
}
