package de.aelpecyem.besmirchment.client;

import de.aelpecyem.besmirchment.client.packet.FamiliarAbilityPacket;
import de.aelpecyem.besmirchment.client.renderer.FinalBroomEntityRenderer;
import de.aelpecyem.besmirchment.common.Besmirchment;
import de.aelpecyem.besmirchment.common.registry.BSMEntityTypes;
import de.aelpecyem.besmirchment.common.registry.BSMObjects;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class BesmirchmentClient implements ClientModInitializer {
    public static final KeyBinding FAMILIAR_ABILITY = new KeyBinding("key." + Besmirchment.MODID +".familiar_ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "itemGroup.besmirchment.grouo");
    public int abilityCooldown = 20;
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(BSMEntityTypes.FINAL_BROOM, (dispatcher, context) -> new FinalBroomEntityRenderer(dispatcher));
        EntityRendererRegistry.INSTANCE.register(BSMEntityTypes.WITCHY_DYE, (dispatcher, context) -> new FlyingItemEntityRenderer<>(dispatcher, context.getItemRenderer()));
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 1 ? BSMObjects.WITCHY_DYE.getColor(stack): 0xFFFFFF, BSMObjects.WITCHY_DYE);

        KeyBindingRegistryImpl.registerKeyBinding(FAMILIAR_ABILITY);

        ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
            if (FAMILIAR_ABILITY.wasPressed() && abilityCooldown <= 0){
                FamiliarAbilityPacket.send();
                abilityCooldown = 20;
            }else if (abilityCooldown > 0){
                abilityCooldown--;
            }
        });
    }
}
