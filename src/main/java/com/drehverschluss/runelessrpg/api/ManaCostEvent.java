package com.drehverschluss.runelessrpg.api;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.spell_engine.api.spell.Spell;

/**
 * Fired on {@link net.neoforged.neoforge.common.NeoForge#EVENT_BUS} every time RunelessRPG calculates the mana cost
 * of a spell for a player, after base cost, {@code mana_cost} (with its lower limit) and {@code overcharge} were applied.
 * Listeners may change the final cost.
 * <p>
 * <b>Fired on both logical sides.</b> The client calculates costs itself for the cast prediction, the "missing mana"
 * message and the spell tooltip (every frame while a tooltip is shown). Listeners must therefore calculate exactly the
 * same result on client and server, otherwise the client predicts a different outcome than the server decides.
 * Keep listeners cheap.
 * <p>
 * The event is only fired for spells that cost mana ({@link ManaCategory} other than {@code NONE} and {@code FREE}).
 * For channeled spells the cost is the total for the full channel duration.
 */
public class ManaCostEvent extends Event {
    private final Player player;
    private final Holder<Spell> spell;
    private final ManaCategory category;
    private final double baseCost;
    private double cost;

    public ManaCostEvent(Player player, Holder<Spell> spell, ManaCategory category, double baseCost, double cost) {
        this.player = player;
        this.spell = spell;
        this.category = category;
        this.baseCost = baseCost;
        this.cost = cost;
    }

    /** @return the casting player (client or server instance) */
    public Player getPlayer() {
        return player;
    }

    /** @return the spell */
    public Holder<Spell> getSpell() {
        return spell;
    }

    /** @return the resolved category */
    public ManaCategory getCategory() {
        return category;
    }

    /** @return the base cost before player attributes (tier table × category multiplier, or the datapack value) */
    public double getBaseCost() {
        return baseCost;
    }

    /** @return the current final cost */
    public double getCost() {
        return cost;
    }

    /** Sets the final cost; negative values are treated as 0. */
    public void setCost(double cost) {
        this.cost = Math.max(0.0, cost);
    }
}
