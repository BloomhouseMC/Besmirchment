package de.aelpecyem.besmirchment.client;

import de.aelpecyem.besmirchment.client.packet.FamiliarAbilityPacket;
import de.aelpecyem.besmirchment.client.packet.WerepyreJumpPacket;
import de.aelpecyem.besmirchment.client.renderer.*;
import de.aelpecyem.besmirchment.common.Besmirchment;
import de.aelpecyem.besmirchment.common.item.WitchyDyeItem;
import de.aelpecyem.besmirchment.common.packet.LichRevivePacket;
import de.aelpecyem.besmirchment.common.packet.SparklePacket;
import de.aelpecyem.besmirchment.common.registry.BSMBlockEntityTypes;
import de.aelpecyem.besmirchment.common.registry.BSMEntityTypes;
import de.aelpecyem.besmirchment.common.registry.BSMObjects;
import de.aelpecyem.besmirchment.common.registry.BSMTransformations;
import de.aelpecyem.besmirchment.common.transformation.WerepyreAccessor;
import moriyashiine.bewitchment.common.item.TaglockItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

@Environment(EnvType.CLIENT)
public class BesmirchmentClient implements ClientModInitializer {
    public static final KeyBinding FAMILIAR_ABILITY = new KeyBinding("key." + Besmirchment.MODID +".familiar_ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "itemGroup.besmirchment.group");
    public int abilityCooldown = 20;
    public static int fogTicks = 0;
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), BSMObjects.PHYLACTERY);

        EntityRendererRegistry.INSTANCE.register(BSMEntityTypes.FINAL_BROOM, (dispatcher, context) -> new FinalBroomEntityRenderer(dispatcher));
        EntityRendererRegistry.INSTANCE.register(BSMEntityTypes.WITCHY_DYE, (dispatcher, context) -> new FlyingItemEntityRenderer<>(dispatcher, context.getItemRenderer()));
        EntityRendererRegistry.INSTANCE.register(BSMEntityTypes.WEREPYRE, (dispatcher, context) -> new WerepyreEntityRenderer(dispatcher));
        EntityRendererRegistry.INSTANCE.register(BSMEntityTypes.BEELZEBUB, (dispatcher, context) -> new BeelzebubEntityRenderer(dispatcher));
        EntityRendererRegistry.INSTANCE.register(BSMEntityTypes.INFECTIOUS_SPIT, (dispatcher, context) -> new InfectiousSpitEntityRenderer(dispatcher));

        BlockEntityRendererRegistry.INSTANCE.register(BSMBlockEntityTypes.PHYLACTERY, PhylacteryBlockEntityRenderer::new);
        FabricModelPredicateProviderRegistry.register(BSMObjects.DEMONIC_DEED, Besmirchment.id("variant"), (stack, world, entity) -> TaglockItem.hasTaglock(stack) ? 1 : 0);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 1 ? BSMObjects.WITCHY_DYE.getColor(stack) == WitchyDyeItem.FUNNI_NUMBER ? BesmirchmentClient.HSBtoRGB(
                ((MinecraftClient.getInstance().world.getTime() + MinecraftClient.getInstance().getTickDelta()) % 100) / 100F,
                1, 1) : BSMObjects.WITCHY_DYE.getColor(stack) : 0xFFFFFF, BSMObjects.WITCHY_DYE);

        KeyBindingRegistryImpl.registerKeyBinding(FAMILIAR_ABILITY);
        ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
            if (minecraftClient.player != null){
                if (BSMTransformations.isLich(minecraftClient.player, true)){
                    if (shouldObscureVision(minecraftClient.player)){
                        fogTicks = 10;
                    }
                    if (minecraftClient.options.keySprint.isPressed()){
                        minecraftClient.player.setSprinting(true);
                    }
                    minecraftClient.player.setVelocity(minecraftClient.player.getRotationVector().multiply(minecraftClient.player.isSprinting() ? 0.8F : 0.5F));
                }
                if (minecraftClient.player.input != null && minecraftClient.player.input.jumping && BSMTransformations.isWerepyre(minecraftClient.player, false) && !minecraftClient.player.isOnGround() && ((WerepyreAccessor) minecraftClient.player).getLastJumpTicks() > 5) {
                    minecraftClient.player.jump();
                    WerepyreJumpPacket.send();
                }
            }
            if (FAMILIAR_ABILITY.wasPressed() && abilityCooldown <= 0){
                FamiliarAbilityPacket.send();
                abilityCooldown = 20;
            }else if (abilityCooldown > 0){
                abilityCooldown--;
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(SparklePacket.ID, SparklePacket::handle);
        ClientPlayNetworking.registerGlobalReceiver(LichRevivePacket.ID, LichRevivePacket::handle);
    }

    public static int getFogTicks(){
        return fogTicks;
    }

    public static boolean shouldObscureVision(PlayerEntity player){
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for(int i = 0; i < 8; ++i) {
            double x = player.getX() + (double)(((float)(i % 2) - 0.5F) * player.getWidth() * 0.8F);
            double y = player.getEyeY() + (double)(((float)((i >> 1) % 2) - 0.5F) * 0.1F);
            double z = player.getZ() + (double)(((float)((i >> 2) % 2) - 0.5F) * player.getWidth() * 0.8F);
            mutable.set(x, y, z);
            BlockState blockState = player.world.getBlockState(mutable);
            if (blockState.getRenderType() != BlockRenderType.INVISIBLE && blockState.shouldBlockVision(player.world, mutable)) {
                return true;
            }
        }
        return false;
    }

    public static int HSBtoRGB(float hue, float saturation, float brightness){
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
    }
}
