package com.arlight.tetris.game;

/**
 * Demora breve al tocar el suelo. Ocho ticks equivalen a 0.4 segundos:
 * permite un último ajuste sin hacer esperar demasiado a la siguiente pieza.
 */
public class LockDelayHandler {
    public static final int LOCK_DELAY_TICKS = 8;
    public static final int MAX_RESETS = 8;

    private int ticksRemaining = LOCK_DELAY_TICKS;
    private int resetsUsed = 0;
    private boolean grounded = false;

    public void update(boolean isGroundedNow) {
        if (isGroundedNow) {
            if (!grounded) {
                grounded = true;
                ticksRemaining = LOCK_DELAY_TICKS;
            } else {
                ticksRemaining--;
            }
        } else {
            grounded = false;
            ticksRemaining = LOCK_DELAY_TICKS;
        }
    }

    public void onSuccessfulAction() {
        if (grounded && resetsUsed < MAX_RESETS) {
            ticksRemaining = LOCK_DELAY_TICKS;
            resetsUsed++;
        }
    }

    public boolean shouldLock() { return grounded && ticksRemaining <= 0; }

    public void reset() {
        ticksRemaining = LOCK_DELAY_TICKS;
        resetsUsed = 0;
        grounded = false;
    }
}
