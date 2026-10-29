import chronorunner.combat.DamageInfo;
import chronorunner.core.InputHandler;
import chronorunner.entity.Player;
import chronorunner.entity.character.CharacterFactory;

public class GodModeCheck {
    public static void main(String[] args) {
        boolean godMode = Boolean.parseBoolean(args[0]);
        if (Player.isGodModeEnabled() != godMode) throw new AssertionError("Wrong launch mode");
        for (int character = 0; character < 3; character++) {
            for (int slot = 0; slot < 2; slot++) {
                Player p = CharacterFactory.create(character, slot, new InputHandler(), 0, 0);
                int hp = p.getHealth();
                p.takeDamage(new DamageInfo(godMode ? 100000 : 10, 200, -300, false));
                if (p.getHealth() != hp - (godMode ? 0 : 10)) throw new AssertionError("Damage mismatch");
                if (godMode && (p.isDead() || p.vx != 0 || p.vy != 0 || p.isInvulnerable()))
                    throw new AssertionError("God mode applied damage effects");
            }
        }
        System.out.println("PASS: all 3 characters, both slots; godMode=" + godMode);
    }
}
