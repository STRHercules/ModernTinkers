package com.moderntinkers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.moderntinkers.content.tools.StackProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;

/** Renders the synchronized tool stack instead of a generic vanilla arrow. */
public final class StackProjectileRenderer extends EntityRenderer<StackProjectileEntity> {
    private final ItemRenderer itemRenderer;

    public StackProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(StackProjectileEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()) + 225.0F));
        poseStack.translate(0.2D, -0.2D, 0.0D);
        itemRenderer.renderStatic(entity.getDisplayStack(), ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(StackProjectileEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
