package com.drehverschluss.runelessrpg.client;

import com.drehverschluss.runelessrpg.client.config.RunelessClientConfig;
import com.drehverschluss.runelessrpg.orb.ManaOrb;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws a mana orb with the vanilla experience orb sprite (referenced from the vanilla resources, not copied), tinted
 * blue and shimmering; more merged orbs use a bigger sprite (up to 5). Lit a bit brighter than its surroundings, like
 * experience orbs. Full orbs pulse slowly in size and brightness ({@code fullOrbEffect}); their sparkles come from
 * {@link ManaOrbSparkle}.
 */
public class ManaOrbRenderer extends EntityRenderer<ManaOrb> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/experience_orb.png");
    private static final RenderType RENDER_TYPE = RenderType.itemEntityTranslucentCull(TEXTURE);
    /** Sprite indices for 1–5 orbs (sprites 0, 1, 2, 5, 8 are 8, 10, 12, 14, 16 pixels wide). */
    private static final int[] ICONS = {0, 1, 2, 5, 8};
    private static final float SCALE = 0.3F;
    /** Pulse of full orbs: one cycle in 2 s, up to 8 % bigger and up to 30 % closer to white. */
    private static final float PULSE_TICKS = 40.0F;
    private static final float PULSE_SIZE = 0.08F;
    private static final float PULSE_BRIGHTNESS = 0.3F;

    public ManaOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.15F;
        shadowStrength = 0.75F;
    }

    @Override
    protected int getBlockLightLevel(ManaOrb entity, BlockPos pos) {
        return Mth.clamp(super.getBlockLightLevel(entity, pos) + 7, 0, 15);
    }

    @Override
    public void render(ManaOrb entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        int icon = icon(entity.getCount());
        float u0 = (icon % 4 * 16) / 64.0F;
        float u1 = (icon % 4 * 16 + 16) / 64.0F;
        float v0 = (icon / 4 * 16) / 64.0F;
        float v1 = (icon / 4 * 16 + 16) / 64.0F;
        float time = (entity.tickCount + partialTicks) / 2.0F;
        int red = (int) (20 + (Mth.sin(time) + 1.0F) * 0.5F * 50);
        int green = (int) (100 + (Mth.sin(time + (float) (Math.PI * 4.0 / 3.0)) + 1.0F) * 0.5F * 60);
        int blue = 255;
        float scale = SCALE;
        if (ManaOrbSparkle.isFull(entity) && RunelessClientConfig.FULL_ORB_EFFECT.get().pulse()) {
            // Slow pulse of full orbs: 0..1 every PULSE_TICKS, each orb in its own phase
            float pulse = (Mth.sin((entity.tickCount + partialTicks + entity.getId() * 7) * Mth.TWO_PI / PULSE_TICKS) + 1.0F) * 0.5F;
            scale *= 1.0F + PULSE_SIZE * pulse;
            float white = PULSE_BRIGHTNESS * pulse;
            red = (int) Mth.lerp(white, red, 255);
            green = (int) Mth.lerp(white, green, 255);
        }
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.scale(scale, scale, scale);
        VertexConsumer consumer = buffer.getBuffer(RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -0.5F, -0.25F, red, green, blue, u0, v1, packedLight);
        vertex(consumer, pose, 0.5F, -0.25F, red, green, blue, u1, v1, packedLight);
        vertex(consumer, pose, 0.5F, 0.75F, red, green, blue, u1, v0, packedLight);
        vertex(consumer, pose, -0.5F, 0.75F, red, green, blue, u0, v0, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * Sprite of the experience orb sheet: one size step per orb up to 5 (8, 10, 12, 14, 16 pixels wide), bigger counts
     * keep the size of 5.
     */
    private static int icon(int count) {
        return ICONS[Mth.clamp(count, 1, ICONS.length) - 1];
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, int red, int green, int blue, float u, float v, int light) {
        consumer.addVertex(pose, x, y, 0.0F)
                .setColor(red, green, blue, 128)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ManaOrb entity) {
        return TEXTURE;
    }
}
