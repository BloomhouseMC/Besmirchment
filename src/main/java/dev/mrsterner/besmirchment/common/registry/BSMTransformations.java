package dev.mrsterner.besmirchment.common.registry;

import dev.mrsterner.besmirchment.common.entity.WerepyreEntity;
import dev.mrsterner.besmirchment.common.transformation.LichTransformation;
import dev.mrsterner.besmirchment.common.transformation.WerepyreTransformation;
import moriyashiine.bewitchment.api.BewitchmentAPI;
import moriyashiine.bewitchment.api.component.TransformationComponent;
import moriyashiine.bewitchment.api.registry.Transformation;
import moriyashiine.bewitchment.common.registry.BWComponents;
import moriyashiine.bewitchment.common.registry.BWRegistries;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class BSMTransformations {
    public static final Transformation WEREPYRE = new WerepyreTransformation();
    public static final Transformation LICH = new LichTransformation();
    public static void init(){
        BSMUtil.register(BWRegistries.TRANSFORMATION, "werepyre", WEREPYRE);
        BSMUtil.register(BWRegistries.TRANSFORMATION, "lich", LICH);
    }

    public static boolean isLich(Entity entity, boolean isGost){
        if(entity instanceof PlayerEntity player){
            return BWComponents.TRANSFORMATION_COMPONENT.get(player).getTransformation() == LICH && (!isGost || BWComponents.TRANSFORMATION_COMPONENT.get((PlayerEntity) entity).isAlternateForm());
        }
        return false;
    }
    public static boolean isWerepyre(Entity entity, boolean includeHumanForm) {
        if (entity instanceof PlayerEntity player && BWComponents.TRANSFORMATION_COMPONENT.get(player).getTransformation() == WEREPYRE) {
            return includeHumanForm || BWComponents.TRANSFORMATION_COMPONENT.get(player).isAlternateForm();
        } else {
            return entity instanceof WerepyreEntity;
        }
    }

    public static boolean hasWerepyrePledge(PlayerEntity player){
        return BewitchmentAPI.isPledged(player, "pledge.besmirchment.beelzebub");
    }
    /*
    public static void handleNourish(PlayerEntity player){
        if (Bewitchment.isNourishLoaded) {
            NourishComponent nourishComponent = NourishMain.NOURISH.get(player);
            for (NourishGroup group : NourishGroups.groups) {
                if (nourishComponent.getValue(group) != group.getDefaultValue()) {
                    nourishComponent.setValue(group, group.getDefaultValue());
                }
            }
        }
    }

     */
}
