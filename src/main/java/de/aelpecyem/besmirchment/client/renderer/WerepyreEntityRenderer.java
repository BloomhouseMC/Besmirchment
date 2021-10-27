package de.aelpecyem.besmirchment.client.renderer;

import de.aelpecyem.besmirchment.client.BesmirchmentClient;
import de.aelpecyem.besmirchment.client.model.WerepyreEntityModel;
import de.aelpecyem.besmirchment.common.Besmirchment;
import de.aelpecyem.besmirchment.common.entity.interfaces.DyeableEntity;
import de.aelpecyem.besmirchment.common.entity.WerepyreEntity;
import de.aelpecyem.besmirchment.common.item.WitchyDyeItem;
import moriyashiine.bewitchment.common.entity.living.util.BWHostileEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class WerepyreEntityRenderer extends MobEntityRenderer<WerepyreEntity, WerepyreEntityModel<WerepyreEntity>> {
    private static Identifier[] TEXTURES;
    private static final Identifier UNTINTED_TEXTURE = Besmirchment.id("textures/entity/werepyre/untinted.png");

    public WerepyreEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new WerepyreEntityModel<>(context.getPart(BesmirchmentClient.WEREPYRE_MODEL_LAYER)), 1f);
        this.addFeature(new HeldItemFeatureRenderer<>(this));
        this.addFeature(new DyedWerepyreFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(WerepyreEntity entity) {
        if (((DyeableEntity) entity).getColor() >= 0 || ((DyeableEntity) entity).getColor() == WitchyDyeItem.FUNNI_NUMBER){
            return UNTINTED_TEXTURE;
        }
        if (TEXTURES == null) {
            int variants = entity.getVariants();
            TEXTURES = new Identifier[variants];
            for (int i = 0; i < variants; ++i) {
                TEXTURES[i] = Besmirchment.id("textures/entity/werepyre/" + i + ".png");
            }
        }
         return TEXTURES[entity.getDataTracker().get(BWHostileEntity.VARIANT)];
    }
}