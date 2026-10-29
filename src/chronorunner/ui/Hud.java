package chronorunner.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import chronorunner.core.Game;
import chronorunner.entity.Player;
import chronorunner.entity.boss.Boss;
import chronorunner.gfx.Art;
import chronorunner.gfx.PixelFont;
import chronorunner.world.Level;

/** In-game overlay: health, guard, energy, per-slot skill cooldowns and controls. */
public class Hud {

    public void render(Graphics2D g, Level level, List<Player> players) {
        g.setFont(UiKit.SMALL);
        g.setColor(UiKit.MUTED);
        g.drawString("CHAPTER " + level.chapter() + "  —  " + level.tile().placeName, 24, 28);
        if (Player.isGodModeEnabled()) {
            g.setColor(UiKit.ACCENT);
            g.drawString("TEST: GOD MODE", Game.WIDTH - 170, 28);
        }

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
            int py = Game.HEIGHT - 160;
            // Guard sits below the expanded panel so it cannot cover a fighter's feet.
            int guardY = py + 136;
            UiKit.panel(g, px, guardY, 300, 22, new Color(8, 12, 26, 205), new Color(255, 255, 255, 45));
            Color guardColor = p.isDead() ? UiKit.MUTED : p.isBlocking() ? Color.WHITE : UiKit.ACCENT;
            g.setFont(UiKit.SMALL);
            g.setColor(guardColor);
            g.drawString("Block [" + p.getBlockKeyLabel() + "]", px + 14, guardY + 16);
            String guardStatus = p.isDead() ? "—" : p.isBlocking() ? "ĐANG ĐỠ"
                    : p.getBlockCooldownRemaining() > 0
                    ? String.format(java.util.Locale.ROOT, "%.1fs", p.getBlockCooldownRemaining()) : "SẴN SÀNG";
            g.drawString(guardStatus, px + 126, guardY + 16);
            UiKit.bar(g, px + 216, guardY + 8, 70, 10, p.isDead() ? 0 : p.getBlockCooldownRatio(),
                    guardColor, new Color(0, 0, 0, 160));
            UiKit.panel(g, px, py, 300, 136, new Color(8, 12, 26, 225), new Color(255, 255, 255, 45));

            Art.player(p.characterId()).frame(0)
                    .drawPreview(g, px + 24, py + 32, 38, 42);

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
            Color energyColor = p.isCharging() ? new Color(210, 250, 255) : UiKit.ACCENT;
            PixelFont.draw(g, "ENERGY " + (int)p.getEnergy() + "/100  [" + p.getChargeKeyLabel() + "] GIỮ NẠP",
                    px + 14, py + 53, 1, energyColor, 0);
            UiKit.bar(g, px + 14, py + 70, 272, 8, p.getEnergyRatio(), energyColor, new Color(0, 0, 0, 160));
            for (int slot = 0; slot < 3; slot++) {
                int sx = px + 14 + slot * 92;
                boolean ready = p.getSkillCooldownRemaining(slot) <= 0 && p.getEnergy() >= p.getSkillCost(slot) && !p.isDead();
                Color tint = ready ? UiKit.ACCENT : UiKit.MUTED;
                UiKit.panel(g, sx, py + 84, 88, 40, new Color(3, 9, 20, 220), tint);
                String status = p.getSkillCooldownRemaining(slot) > 0
                        ? String.format(java.util.Locale.ROOT, "%.1fs", p.getSkillCooldownRemaining(slot))
                        : p.getSkillCost(slot) + " EN";
                PixelFont.draw(g, "[" + p.getSkillKeyLabel(slot) + "] " + status, sx + 4, py + 85, 1, tint, 0);
                String name = p.getSkillName(slot);
                if (name.length() > 13) name = name.substring(0, 12) + ".";
                PixelFont.draw(g, name, sx + 4, py + 101, 1, Color.WHITE, 0);
                UiKit.bar(g, sx + 4, py + 118, 80, 3, p.getSkillCooldownRatio(slot), tint, new Color(0, 0, 0, 160));
            }

            if (p.isDead()) {
                g.setFont(UiKit.H2);
                g.setColor(UiKit.WARN);
                g.drawString("DOWN", px + 224, py + 26);
            }
        }
        int helpX = 348;
        PixelFont.draw(g, "P1  A/D DI CHUYỂN  W NHẢY  K DASH  J > J > J",
                helpX, Game.HEIGHT - 48, 1, new Color(230, 240, 255, 200), 0);
        PixelFont.draw(g, "THƯỜNG > HEAVY > LAUNCHER  S BLOCK  L NẠP  U/I/O SKILL",
                helpX, Game.HEIGHT - 31, 1, new Color(230, 240, 255, 200), 0);
        if (players.size() > 1) PixelFont.draw(g,
                "P2  TRÁI/PHẢI  N0 NHẢY  N1 COMBO  N3 DASH  N4/5/6 SKILL",
                helpX, Game.HEIGHT - 14, 1, new Color(230, 240, 255, 200), 0);
    }
}
