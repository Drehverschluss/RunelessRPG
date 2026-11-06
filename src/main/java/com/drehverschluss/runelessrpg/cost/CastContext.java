package com.drehverschluss.runelessrpg.cost;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tells the mana check in {@code Ammo.ammoForSpell} which step of a cast is being checked. Set by mixins around
 * {@code SpellExecution.performSpell} (channel tick / release) and the tooltip; without a context the check is the
 * one for starting a cast.
 */
public final class CastContext {
    public enum Stage {
        /** Starting a cast (client prediction, server request) – also the default. */
        START,
        /** A channel tick on the server. */
        CHANNEL_TICK,
        /** Releasing / finishing a cast on the server. */
        RELEASE,
        /** Passive trigger. */
        TRIGGER,
        /** Return Spell Engine's original result (used for the tooltip's item line). */
        BYPASS
    }

    private static final ThreadLocal<Deque<Stage>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private CastContext() {}

    public static void push(Stage stage) {
        STACK.get().push(stage);
    }

    public static void pop() {
        Deque<Stage> stack = STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static Stage current() {
        Stage stage = STACK.get().peek();
        return stage == null ? Stage.START : stage;
    }

    /** Drops all stages of the current thread (called at the start of every tick). */
    public static void clear() {
        STACK.get().clear();
    }
}
