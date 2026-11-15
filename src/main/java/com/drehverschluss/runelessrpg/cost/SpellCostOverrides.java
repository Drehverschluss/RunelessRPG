package com.drehverschluss.runelessrpg.cost;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Per-spell costs from data files {@code data/<ns>/runelessrpg/spell_costs/*.json}:
 * <pre>{@code
 * {
 *   "costs":   {"wizards:fireball": 6},       // fixed base cost, always applies
 *   "factors": {"wizards:frostbolt": 0.7}     // factor on the tier cost, follows tierCosts and the category
 * }
 * }</pre>
 * <ul>
 *   <li>Several files with the <b>same path</b>: only the one from the highest pack counts (like recipes or loot
 *       tables). A datapack with {@code data/runelessrpg/runelessrpg/spell_costs/rpg_series.json} replaces the built-in
 *       file, {@code {"costs": {}}} turns it off.</li>
 *   <li>Different files are merged in pack order (then by file name); for the same spell the later entry wins, fixed
 *       cost or factor. Inside one file a fixed cost wins over a factor for the same spell.</li>
 *   <li>Entries for spells that are not installed are skipped silently.</li>
 *   <li>Entries from RunelessRPG's own jar are marked as built-in (source {@code BUILTIN}).</li>
 * </ul>
 */
public final class SpellCostOverrides {
    private static final String DIRECTORY = "runelessrpg/spell_costs";
    private static final String OWN_PACK_ID = "mod/" + RunelessRPG.MODID;

    /**
     * One entry.
     *
     * @param amount  fixed base cost, or the factor on the tier cost
     * @param factor  true = {@code amount} is a factor on the tier cost
     * @param builtin true = from RunelessRPG's own data
     */
    public record Value(double amount, boolean factor, boolean builtin) {
        public static final StreamCodec<ByteBuf, Value> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Value::amount,
                ByteBufCodecs.BOOL, Value::factor,
                ByteBufCodecs.BOOL, Value::builtin,
                Value::new);
    }

    private static volatile Map<ResourceLocation, Value> serverValues = Map.of();
    private static volatile Map<ResourceLocation, Value> clientValues = Map.of();

    private SpellCostOverrides() {}

    public static Optional<Value> get(boolean clientSide, ResourceLocation spellId) {
        return Optional.ofNullable((clientSide ? clientValues : serverValues).get(spellId));
    }

    public static Map<ResourceLocation, Value> serverValues() {
        return serverValues;
    }

    public static void setClientValues(Map<ResourceLocation, Value> values) {
        clientValues = Map.copyOf(values);
    }

    /** Reload listener for the server data; needs the registry access to validate spell ids. */
    public static final class Loader extends SimplePreparableReloadListener<Map<ResourceLocation, Value>> {
        private final RegistryAccess registryAccess;

        public Loader(RegistryAccess registryAccess) {
            this.registryAccess = registryAccess;
        }

        @Override
        protected Map<ResourceLocation, Value> prepare(ResourceManager manager, ProfilerFiller profiler) {
            Map<String, Integer> packOrder = new HashMap<>();
            List<String> packs = manager.listPacks().map(pack -> pack.packId()).toList();
            for (int i = 0; i < packs.size(); i++) {
                packOrder.put(packs.get(i), i);
            }

            record Entry(ResourceLocation file, Resource resource, int pack) {}
            List<Entry> entries = new ArrayList<>();
            manager.listResourceStacks(DIRECTORY, id -> id.getPath().endsWith(".json")).forEach((file, stack) -> {
                // The stack is ordered from the lowest to the highest pack: only the top file of a path counts
                if (!stack.isEmpty()) {
                    Resource top = stack.get(stack.size() - 1);
                    entries.add(new Entry(file, top, packOrder.getOrDefault(top.sourcePackId(), -1)));
                }
            });
            entries.sort(Comparator.comparingInt(Entry::pack).thenComparing(entry -> entry.file().toString()));

            Map<ResourceLocation, Value> result = new HashMap<>();
            for (Entry entry : entries) {
                boolean builtin = OWN_PACK_ID.equals(entry.resource().sourcePackId());
                try (Reader reader = entry.resource().openAsReader()) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    read(root.getAsJsonObject("factors"), true, builtin, entry.file(), result);
                    read(root.getAsJsonObject("costs"), false, builtin, entry.file(), result);
                } catch (Exception ex) {
                    RunelessRPG.LOGGER.error("Failed to read spell costs from {} ({})", entry.file(), entry.resource().sourcePackId(), ex);
                }
            }
            return result;
        }

        private static void read(JsonObject values, boolean factor, boolean builtin, ResourceLocation file, Map<ResourceLocation, Value> result) {
            if (values == null) {
                return;
            }
            for (Map.Entry<String, JsonElement> value : values.entrySet()) {
                ResourceLocation spellId = ResourceLocation.tryParse(value.getKey());
                double amount = value.getValue().isJsonPrimitive() && value.getValue().getAsJsonPrimitive().isNumber()
                        ? value.getValue().getAsDouble() : Double.NaN;
                if (spellId != null && amount >= 0.0) {
                    result.put(spellId, new Value(amount, factor, builtin));
                } else {
                    RunelessRPG.LOGGER.warn("Ignoring invalid spell cost {} '{}' in {}", factor ? "factor" : "entry", value.getKey(), file);
                }
            }
        }

        @Override
        protected void apply(Map<ResourceLocation, Value> values, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, Value> known = new HashMap<>();
            Optional<Registry<Spell>> registry = registryAccess.registry(SpellRegistry.KEY);
            // Entries for spells that are not installed are skipped silently (packs list spells of optional mods)
            values.forEach((id, value) -> {
                if (registry.isEmpty() || registry.get().containsKey(id)) {
                    known.put(id, value);
                }
            });
            serverValues = Map.copyOf(known);
        }
    }
}
