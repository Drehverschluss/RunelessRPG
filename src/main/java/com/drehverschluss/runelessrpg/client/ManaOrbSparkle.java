package com.drehverschluss.runelessrpg.client;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.client.config.RunelessClientConfig;
import com.drehverschluss.runelessrpg.cost.CostSettings;
import com.drehverschluss.runelessrpg.orb.ManaOrb;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Golden sparkle that rises slowly from a full mana orb. Client only, not a registered particle type: it uses the
 * vanilla {@code minecraft:glow} sprite of the particle atlas (referenced, not copied), tinted gold like the Mana
 * Harvest potion ({@code 0xE0B040}), always fully lit, fading out at the end.
 */
public class ManaOrbSparkle extends TextureSheetParticle {
    private static final ResourceLocation SPRITE = ResourceLocation.withDefaultNamespace("glow");
    private static final float RED = 0xE0 / 255.0F;
    private static final float GREEN = 0xB0 / 255.0F;
    private static final float BLUE = 0x40 / 255.0F;
    /** Sparkles per tick and full orb at particle setting "All" (about 3 per second). */
    private static final float CHANCE_ALL = 0.15F;
    /** Vanilla drops a third of the particles at "Decreased". */
    private static final float CHANCE_DECREASED = CHANCE_ALL * 2.0F / 3.0F;
    /** Like vanilla particles: none further away than 32 blocks. */
    private static final double MAX_DISTANCE_SQR = 32.0 * 32.0;

    private static boolean failed;

    private final float startSize;

    private ManaOrbSparkle(ClientLevel level, double x, double y, double z, RandomSource random) {
        super(level, x, y, z);
        setSprite(sprite());
        float shade = 0.9F + random.nextFloat() * 0.1F;
        setColor(RED * shade, GREEN * shade, BLUE * shade);
        xd = (random.nextDouble() - 0.5) * 0.006;
        yd = 0.012 + random.nextDouble() * 0.008;
        zd = (random.nextDouble() - 0.5) * 0.006;
        gravity = 0.0F;
        friction = 1.0F;
        hasPhysics = false;
        lifetime = 24 + random.nextInt(12);
        startSize = 0.07F + random.nextFloat() * 0.04F;
        quadSize = startSize;
    }

    /** The particle atlas belongs to the particle engine, which registers it with the texture manager. */
    private static TextureAtlasSprite sprite() {
        return ((TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES)).getSprite(SPRITE);
    }

    @Override
    public void tick() {
        super.tick();
        float progress = (float) age / lifetime;
        // Full for the first 60 % of its life, then fades and shrinks a little
        float fade = progress < 0.6F ? 1.0F : 1.0F - (progress - 0.6F) / 0.4F;
        alpha = Mth.clamp(fade, 0.0F, 1.0F);
        quadSize = startSize * (0.6F + 0.4F * alpha);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    /**
     * Client tick of an orb (set as {@link ManaOrb#setClientTicker}): sparkles of a full orb. A failure (for example a
     * resource pack without the sprite) turns the sparkles off instead of crashing the game.
     */
    public static void tickOrb(ManaOrb orb) {
        if (failed) {
            return;
        }
        try {
            spawnSparkle(orb);
        } catch (RuntimeException e) {
            failed = true;
            RunelessRPG.LOGGER.error("Mana orb sparkles failed, turned off for this session", e);
        }
    }

    private static void spawnSparkle(ManaOrb orb) {
        if (!(orb.level() instanceof ClientLevel level) || !isFull(orb)
                || !RunelessClientConfig.FULL_ORB_EFFECT.get().sparkles()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        float chance = switch (minecraft.options.particles().get()) {
            case ALL -> CHANCE_ALL;
            case DECREASED -> CHANCE_DECREASED;
            case MINIMAL -> 0.0F;
        };
        RandomSource random = orb.getRandom();
        if (chance <= 0.0F || random.nextFloat() >= chance) {
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera.getPosition().distanceToSqr(orb.position()) > MAX_DISTANCE_SQR) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = random.nextDouble() * 0.12;
        Vec3 pos = orb.position();
        minecraft.particleEngine.add(new ManaOrbSparkle(level, pos.x + Math.cos(angle) * radius,
                pos.y + 0.12 + random.nextDouble() * 0.15, pos.z + Math.sin(angle) * radius, random));
    }

    /** Full orb on the client: uses the server limit {@code maxOrbsPerStack} from the cost settings packet. */
    public static boolean isFull(ManaOrb orb) {
        return ManaOrb.isFull(orb.getCount(), CostSettings.client().maxOrbsPerStack());
    }
}
