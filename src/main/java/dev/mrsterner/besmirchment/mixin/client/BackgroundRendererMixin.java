package dev.mrsterner.besmirchment.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.mrsterner.besmirchment.client.BesmirchmentClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void renderFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci){
        if (BesmirchmentClient.getFogTicks() > 0){
            RenderSystem.setShaderFogStart(0);
            RenderSystem.setShaderFogEnd(4);
            ci.cancel();
            BesmirchmentClient.fogTicks--;
        }
    }
}
