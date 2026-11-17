package com.drehverschluss.runelessrpg.client;

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
 * blue and pulsing; more merged orbs use a bigger sprite. Lit a bit brighter than its surroundings, like experience orbs.
 */
public class ManaOrbRenderer extends EntityRenderer<ManaOrb> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/experience_orb.png");
    private static final RenderType RENDER_TYPE = RenderType.itemEntityTranslucentCull(TEXTURE);

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
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.3F, 0.3F, 0.3F);
        VertexConsumer consumer = buffer.getBuffer(RENDER_TYPE);
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -0.5F, -0.25F, red, green, blue, u0, v1, packedLight);
        vertex(consumer, pose, 0.5F, -0.25F, red, green, blue, u1, v1, packedLight);
        vertex(consumer, pose, 0.5F, 0.75F, red, green, blue, u1, v0, packedLight);
        vertex(consumer, pose, -0.5F, 0.75F, red, green, blue, u0, v0, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /** Sprite index 0–10 of the experience orb sheet: one size step per doubling of the orb count. */
    private static int icon(int count) {
        return Math.min(10, 31 - Integer.numberOfLeadingZeros(Math.max(1, count)));
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
