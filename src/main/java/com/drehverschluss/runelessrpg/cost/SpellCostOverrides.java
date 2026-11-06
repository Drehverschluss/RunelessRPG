package com.drehverschluss.runelessrpg.cost;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
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
 * Per-spell base costs from datapacks: {@code data/<ns>/runelessrpg/spell_costs/*.json} with
 * {@code {"costs": {"wizards:fireball": 6}}}. Files are applied in datapack order, later packs override earlier ones.
 */
public final class SpellCostOverrides {
    private static final String DIRECTORY = "runelessrpg/spell_costs";

    private static volatile Map<ResourceLocation, Double> serverValues = Map.of();
    private static volatile Map<ResourceLocation, Double> clientValues = Map.of();

    private SpellCostOverrides() {}

    public static Optional<Double> get(boolean clientSide, ResourceLocation spellId) {
        return Optional.ofNullable((clientSide ? clientValues : serverValues).get(spellId));
    }

    public static Map<ResourceLocation, Double> serverValues() {
        return serverValues;
    }

    public static void setClientValues(Map<ResourceLocation, Double> values) {
        clientValues = Map.copyOf(values);
    }

    /** Reload listener for the server data; needs the registry access to validate spell ids. */
    public static final class Loader extends SimplePreparableReloadListener<Map<ResourceLocation, Double>> {
        private final RegistryAccess registryAccess;

        public Loader(RegistryAccess registryAccess) {
            this.registryAccess = registryAccess;
        }

        @Override
        protected Map<ResourceLocation, Double> prepare(ResourceManager manager, ProfilerFiller profiler) {
            Map<String, Integer> packOrder = new HashMap<>();
            List<String> packs = manager.listPacks().map(pack -> pack.packId()).toList();
            for (int i = 0; i < packs.size(); i++) {
                packOrder.put(packs.get(i), i);
            }

            record Entry(ResourceLocation file, Resource resource, int pack) {}
            List<Entry> entries = new ArrayList<>();
            manager.listResourceStacks(DIRECTORY, id -> id.getPath().endsWith(".json")).forEach((file, stack) -> {
                for (Resource resource : stack) {
                    entries.add(new Entry(file, resource, packOrder.getOrDefault(resource.sourcePackId(), -1)));
                }
            });
            entries.sort(Comparator.comparingInt(Entry::pack).thenComparing(entry -> entry.file().toString()));

            Map<ResourceLocation, Double> result = new HashMap<>();
            for (Entry entry : entries) {
                try (Reader reader = entry.resource().openAsReader()) {
                    JsonElement root = JsonParser.parseReader(reader);
                    JsonObject costs = root.getAsJsonObject().getAsJsonObject("costs");
                    if (costs == null) {
                        continue;
                    }
                    for (Map.Entry<String, JsonElement> cost : costs.entrySet()) {
                        ResourceLocation spellId = ResourceLocation.tryParse(cost.getKey());
                        double value = cost.getValue().getAsDouble();
                        if (spellId != null && value >= 0.0) {
                            result.put(spellId, value);
                        } else {
                            RunelessRPG.LOGGER.warn("Ignoring invalid spell cost entry '{}' in {}", cost.getKey(), entry.file());
                        }
                    }
                } catch (Exception ex) {
                    RunelessRPG.LOGGER.error("Failed to read spell costs from {} ({})", entry.file(), entry.resource().sourcePackId(), ex);
                }
            }
            return result;
        }

        @Override
        protected void apply(Map<ResourceLocation, Double> values, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, Double> known = new HashMap<>();
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
