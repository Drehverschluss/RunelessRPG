package com.drehverschluss.runelessrpg.orb;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-mob mana orb values from data files {@code data/<ns>/runelessrpg/mana_orbs/*.json}:
 * <pre>{@code
 * {
 *   "mobs": {
 *     "minecraft:creeper": {"factor": 2.0},
 *     "minecraft:piglin":  {"factor": 0.5, "count": 1},
 *     "#c:mini_bosses":    {"baseChance": 25, "count": 3}
 *   }
 * }
 * }</pre>
 * <ul>
 *   <li>{@code factor} (default 1, 0 or more): multiplies the kill chance; 0 = no orbs from this mob.</li>
 *   <li>{@code count} (default 1, 1–64): orbs per successful kill.</li>
 *   <li>{@code baseChance} (optional, 0–100): replaces {@code killBaseChancePercent} for this mob, so the mob can drop
 *       orbs without Mana Harvest; the potion bonus is added on top.</li>
 *   <li>A mob with an entry can drop orbs even if its category is not in {@code killMobCategories}.</li>
 *   <li>Precedence: an entity id beats a tag; between tags the entry from the later file wins, inside one file the first
 *       matching tag. {@code #runelessrpg:no_mana_orbs} and the boss rules beat every entry.</li>
 *   <li>Several files with the <b>same path</b>: only the one from the highest pack counts (like spell_costs). Different
 *       files are merged in pack order, then by file name; for the same id the later entry wins.</li>
 * </ul>
 */
public final class ManaOrbData {
    private static final String DIRECTORY = "runelessrpg/mana_orbs";

    /** One entry; {@code baseChance} is null when the global base chance applies. */
    public record Entry(double factor, int count, @Nullable Double baseChance) {}

    private record TagRule(TagKey<EntityType<?>> tag, Entry entry) {}

    private record Data(Map<ResourceLocation, Entry> byId, List<TagRule> tags) {}

    private static volatile Data data = new Data(Map.of(), List.of());

    private ManaOrbData() {}

    /** Entry for this mob type: its id first, then the tags (later files first); null without an entry. */
    @Nullable
    public static Entry find(EntityType<?> type) {
        Data current = data;
        Entry byId = current.byId().get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        if (byId != null) {
            return byId;
        }
        for (TagRule rule : current.tags()) {
            if (type.is(rule.tag())) {
                return rule.entry();
            }
        }
        return null;
    }

    public static final class Loader extends SimplePreparableReloadListener<Data> {
        @Override
        protected Data prepare(ResourceManager manager, ProfilerFiller profiler) {
            Map<String, Integer> packOrder = new HashMap<>();
            List<String> packs = manager.listPacks().map(pack -> pack.packId()).toList();
            for (int i = 0; i < packs.size(); i++) {
                packOrder.put(packs.get(i), i);
            }
            record File(ResourceLocation id, Resource resource, int pack) {}
            List<File> files = new ArrayList<>();
            manager.listResourceStacks(DIRECTORY, id -> id.getPath().endsWith(".json")).forEach((id, stack) -> {
                // Only the top file of a path counts
                if (!stack.isEmpty()) {
                    Resource top = stack.get(stack.size() - 1);
                    files.add(new File(id, top, packOrder.getOrDefault(top.sourcePackId(), -1)));
                }
            });
            files.sort(Comparator.comparingInt(File::pack).thenComparing(file -> file.id().toString()));

            Map<ResourceLocation, Entry> byId = new HashMap<>();
            List<List<TagRule>> tagsPerFile = new ArrayList<>();
            for (File file : files) {
                List<TagRule> fileTags = new ArrayList<>();
                try (Reader reader = file.resource().openAsReader()) {
                    JsonObject mobs = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("mobs");
                    if (mobs != null) {
                        for (Map.Entry<String, JsonElement> mob : mobs.entrySet()) {
                            read(mob.getKey(), mob.getValue(), file.id(), byId, fileTags);
                        }
                    }
                } catch (Exception ex) {
                    RunelessRPG.LOGGER.error("Failed to read mana orb data from {} ({})", file.id(), file.resource().sourcePackId(), ex);
                }
                tagsPerFile.add(fileTags);
            }
            // Later files first, inside a file in the listed order
            List<TagRule> tags = new ArrayList<>();
            for (int i = tagsPerFile.size() - 1; i >= 0; i--) {
                tags.addAll(tagsPerFile.get(i));
            }
            return new Data(Map.copyOf(byId), List.copyOf(tags));
        }

        private static void read(String key, JsonElement value, ResourceLocation file, Map<ResourceLocation, Entry> byId, List<TagRule> tags) {
            boolean tag = key.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? key.substring(1) : key);
            if (id == null || !value.isJsonObject()) {
                RunelessRPG.LOGGER.warn("Ignoring invalid mana orb entry '{}' in {}", key, file);
                return;
            }
            JsonObject object = value.getAsJsonObject();
            double factor = number(object, "factor", 1.0);
            double count = number(object, "count", 1.0);
            Double baseChance = object.has("baseChance") ? number(object, "baseChance", Double.NaN) : null;
            if (!(factor >= 0.0) || !(count >= 1.0 && count <= 64.0) || (baseChance != null && !(baseChance >= 0.0 && baseChance <= 100.0))) {
                RunelessRPG.LOGGER.warn("Ignoring mana orb entry '{}' in {}: factor >= 0, count 1-64, baseChance 0-100", key, file);
                return;
            }
            Entry entry = new Entry(factor, (int) Math.round(count), baseChance);
            if (tag) {
                tags.add(new TagRule(TagKey.create(Registries.ENTITY_TYPE, id), entry));
            } else {
                byId.put(id, entry);
            }
        }

        private static double number(JsonObject object, String name, double fallback) {
            JsonElement element = object.get(name);
            if (element == null) {
                return fallback;
            }
            return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsDouble() : Double.NaN;
        }

        @Override
        protected void apply(Data loaded, ResourceManager manager, ProfilerFiller profiler) {
            data = loaded;
        }
    }
}
