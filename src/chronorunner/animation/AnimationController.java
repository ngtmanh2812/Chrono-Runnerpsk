package chronorunner.animation;

import java.util.HashMap;
import java.util.Map;

/** Holds named animations and plays one at a time. */
public class AnimationController {

    private final Map<String, Animation> animations = new HashMap<>();
    private String current;

    public void add(String name, Animation animation) {
        animations.put(name, animation);
        if (current == null) {
            current = name;
        }
    }

    /** Switches to a named animation. Restarts it only when the state changes. */
    public void play(String name) {
        if (name.equals(current)) return;
        Animation next = animations.get(name);
        if (next == null) return;
        current = name;
        next.reset();
    }

    public void update(double dt) {
        Animation a = animations.get(current);
        if (a != null) {
            a.update(dt);
        }
    }

    public Animation current() {
        return animations.get(current);
    }

    public String currentName() {
        return current;
    }
}
