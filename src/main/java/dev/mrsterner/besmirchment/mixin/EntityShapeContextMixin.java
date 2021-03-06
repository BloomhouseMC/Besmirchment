package dev.mrsterner.besmirchment.mixin;

import dev.mrsterner.besmirchment.common.block.NoClipContext;
import dev.mrsterner.besmirchment.common.registry.BSMTransformations;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityShapeContext.class)
public class EntityShapeContextMixin implements NoClipContext {
    @Unique private boolean noClipping;

    @Inject(at = @At("RETURN"), method = "<init>(Lnet/minecraft/entity/Entity;)V")
    private void setEntityField(Entity entity, CallbackInfo ci) {
        this.noClipping = BSMTransformations.isLich(entity, true);
    }

    @Override
    public boolean bsm_isNoClipping() {
        return this.noClipping;
    }
}
