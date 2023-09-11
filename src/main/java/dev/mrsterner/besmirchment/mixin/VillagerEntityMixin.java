package dev.mrsterner.besmirchment.mixin;

import dev.mrsterner.besmirchment.common.BSMConfig;
import dev.mrsterner.besmirchment.common.Besmirchment;
import dev.mrsterner.besmirchment.common.entity.WerepyreEntity;
import dev.mrsterner.besmirchment.common.entity.interfaces.VillagerWerepyreAccessor;
import dev.mrsterner.besmirchment.common.registry.BSMEntityTypes;
import dev.mrsterner.besmirchment.common.registry.BSMStatusEffects;
import moriyashiine.bewitchment.api.BewitchmentAPI;
import moriyashiine.bewitchment.api.component.CursesComponent;
import moriyashiine.bewitchment.client.network.packet.SpawnSmokeParticlesPacket;
import moriyashiine.bewitchment.common.registry.BWComponents;
import moriyashiine.bewitchment.common.registry.BWSoundEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityInteraction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.village.VillageGossipType;
import net.minecraft.village.VillagerGossips;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity implements VillagerWerepyreAccessor {
    @Shadow @Final private VillagerGossips gossip;
    @Unique private NbtCompound storedWerepyre;
    @Unique private int despawnTimer = 2400;

    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void setStoredWerepyre(NbtCompound storedWerepyre) {
        this.storedWerepyre = storedWerepyre;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo callbackInfo) {
        if (!world.isClient && storedWerepyre != null) {
            if (!this.hasCustomName() && despawnTimer > 0) {
                despawnTimer--;
                if (despawnTimer == 0) {
                    remove(RemovalReason.DISCARDED);
                }
            }
            if (age % 20 == 0 && world.isNight() && BewitchmentAPI.getMoonPhase(world) == 0 && (this.hasCustomName() || this.world.isSkyVisible(this.getBlockPos()))) {
                WerepyreEntity entity = BSMEntityTypes.WEREPYRE.create(world);
                if (entity != null) {
                    PlayerLookup.tracking(this).forEach(player -> SpawnSmokeParticlesPacket.send(player, this));
                    world.playSound(null, getX(), getY(), getZ(), BWSoundEvents.ENTITY_GENERIC_TRANSFORM, getSoundCategory(), getSoundVolume(), getSoundPitch());
                    entity.readNbt(storedWerepyre);
                    entity.updatePositionAndAngles(getX(), getY(), getZ(), random.nextFloat() * 360, 0);
                    entity.setHealth(entity.getMaxHealth() * (getHealth() / getMaxHealth()));
                    entity.setFireTicks(getFireTicks());
                    entity.clearStatusEffects();
                    getStatusEffects().forEach(entity::addStatusEffect);
                    BWComponents.CURSES_COMPONENT.get(entity).getCurses().clear();
                    BWComponents.CURSES_COMPONENT.get(this).getCurses().forEach((BWComponents.CURSES_COMPONENT.get(entity))::addCurse);
                    if (!entity.hasCustomName() && despawnTimer >= 0) {
                        despawnTimer = 2400;
                    }
                    entity.storedVillager = writeNbt(new NbtCompound());
                    world.spawnEntity(entity);
                    remove(RemovalReason.DISCARDED);
                }
            }
        }
    }

    @Inject(method = "interactMob", at = @At("HEAD"))
    private void interactMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> callbackInfo) {
        despawnTimer = -1;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readCustomDataFromTag(NbtCompound tag, CallbackInfo callbackInfo) {
        if (tag.contains("BSMStoredWerepyre")) {
            storedWerepyre = tag.getCompound("BSMStoredWerepyre");
        }
        if (tag.contains("BSMDespawnTimer")) {
            despawnTimer = tag.getInt("BSMDespawnTimer");
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeCustomDataToTag(NbtCompound tag, CallbackInfo callbackInfo) {
        if (storedWerepyre != null) {
            tag.put("BSMStoredWerepyre", storedWerepyre);
        }
        tag.putInt("BSMDespawnTimer", despawnTimer);
    }

    @Inject(method = "isReadyToBreed", at = @At("HEAD"), cancellable = true)
    private void isWillingToBreed(CallbackInfoReturnable<Boolean> cir){
        if (hasStatusEffect(BSMStatusEffects.LOVE) && getBreedingAge() > -1){
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onInteractionWith", at = @At("HEAD"), cancellable = true)
    private void onInteraction(EntityInteraction interaction, Entity entity, CallbackInfo ci){
        if (entity instanceof PlayerEntity && BewitchmentAPI.getFamiliar((PlayerEntity) entity) == EntityType.VILLAGER){
            if (interaction == EntityInteraction.ZOMBIE_VILLAGER_CURED) {
                this.gossip.startGossip(entity.getUuid(), VillageGossipType.MAJOR_POSITIVE, 40);
                this.gossip.startGossip(entity.getUuid(), VillageGossipType.MINOR_POSITIVE, 50);
            } else if (interaction == EntityInteraction.TRADE) {
                this.gossip.startGossip(entity.getUuid(), VillageGossipType.TRADING, 5);
            } else if (interaction == EntityInteraction.VILLAGER_HURT) {
                this.gossip.startGossip(entity.getUuid(), VillageGossipType.MINOR_NEGATIVE, 10);
            } else if (interaction == EntityInteraction.VILLAGER_KILLED) {
                this.gossip.startGossip(entity.getUuid(), VillageGossipType.MAJOR_NEGATIVE, 5);
            }
            ci.cancel();
        }
    }
    @Inject(method = "getReputation", at = @At("RETURN"), cancellable = true)
    private void getReputation(PlayerEntity player, CallbackInfoReturnable<Integer> cir){
        if (cir.getReturnValue() < BSMConfig.villagerFamiliarReputationBase && BewitchmentAPI.getFamiliar(player) == EntityType.VILLAGER){
            cir.setReturnValue(BSMConfig.villagerFamiliarReputationBase);
        }
    }
}
