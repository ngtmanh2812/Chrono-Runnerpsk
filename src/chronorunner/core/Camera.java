package chronorunner.core;

import java.util.Random;

import chronorunner.util.Collision;

/** Smoothly follows a world point and clamps itself inside the level bounds. */
public class Camera {

    private double x, y;
    private double trauma;
    private double shakeX, shakeY;
    private final Random rng = new Random();
    private final int viewWidth;
    private final int viewHeight;

    public Camera(int viewWidth, int viewHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
    }

    public void follow(double targetCenterX, double targetCenterY, double worldW, double worldH, double dt) {
        double desiredX = targetCenterX - viewWidth / 2.0;
        double desiredY = targetCenterY - viewHeight / 2.0;
        double t = Math.min(1, dt * 8.0);
        x += (desiredX - x) * t;
        y += (desiredY - y) * t;
        clampToWorld(worldW, worldH);
    }

    public void snapTo(double targetCenterX, double targetCenterY, double worldW, double worldH) {
        x = targetCenterX - viewWidth / 2.0;
        y = targetCenterY - viewHeight / 2.0;
        clampToWorld(worldW, worldH);
    }

    private void clampToWorld(double worldW, double worldH) {
        x = Collision.clamp(x, 0, Math.max(0, worldW - viewWidth));
        y = Collision.clamp(y, 0, Math.max(0, worldH - viewHeight));
    }

    /** Adds screen shake energy (0..1). Decays over time. */
    public void addShake(double amount) {
        trauma = Collision.clamp(trauma + amount, 0, 1);
    }

    public void update(double dt) {
        trauma = Math.max(0, trauma - dt * 1.8);
        double magnitude = trauma * trauma * 16.0;
        shakeX = (rng.nextDouble() * 2 - 1) * magnitude;
        shakeY = (rng.nextDouble() * 2 - 1) * magnitude;
    }

    public double offsetX() { return x + shakeX; }
    public double offsetY() { return y + shakeY; }
}
