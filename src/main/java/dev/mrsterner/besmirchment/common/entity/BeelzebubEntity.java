package dev.mrsterner.besmirchment.common.entity;

import com.google.common.collect.Sets;
import dev.mrsterner.besmirchment.common.registry.BSMEntityTypes;
import dev.mrsterner.besmirchment.common.registry.BSMSounds;
import moriyashiine.bewitchment.api.BewitchmentAPI;
import moriyashiine.bewitchment.api.component.TransformationComponent;
import moriyashiine.bewitchment.api.entity.Pledgeable;
import moriyashiine.bewitchment.client.network.packet.SpawnSmokeParticlesPacket;
import moriyashiine.bewitchment.common.entity.living.util.BWHostileEntity;
import moriyashiine.bewitchment.common.misc.BWUtil;
import moriyashiine.bewitchment.common.network.packet.TransformationAbilityPacket;
import moriyashiine.bewitchment.common.registry.*;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BeelzebubEntity extends BWHostileEntity implements Pledgeable {
    private static final StatusEffect[] POSSIBLE_EFFECTS = {
            StatusEffects.INSTANT_DAMAGE,
            StatusEffects.INSTANT_DAMAGE,
            StatusEffects.POISON,
            StatusEffects.POISON,
            StatusEffects.SLOWNESS,
            StatusEffects.SLOWNESS,
            BWStatusEffects.CORROSION,
            StatusEffects.WITHER,
            StatusEffects.WEAKNESS,
            StatusEffects.NAUSEA,
            BWStatusEffects.MORTAL_COIL
    };
    private final Set<UUID> pledgedPlayerUUIDS = new HashSet<>();
    private final ServerBossBar bossBar;
    private int timeSinceLastAttack = 0;

    public BeelzebubEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        bossBar = new ServerBossBar(getDisplayName(), BossBar.Color.RED, BossBar.Style.PROGRESS);
        setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0);
        setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0);
        experiencePoints = 69;
    }
    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 300).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 14).add(EntityAttributes.GENERIC_ARMOR, 10).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
    }

    @Override
    public void tick() {
        super.tick();
        if (!world.isClient) {
            bossBar.setPercent(getHealth() / getMaxHealth());
            LivingEntity target = getTarget();
            int timer = age + getId();
            if (timer % 10 == 0) {
                heal(1);
            }
            if (target != null) {
                timeSinceLastAttack++;
                if (timeSinceLastAttack >= 600) {
                    BWUtil.teleport(this, target.getX(), target.getY(), target.getZ(), true);
                    timeSinceLastAttack = 0;
                }
                lookAtEntity(target, 360, 360);
                if (timer % 80 == 0 && canSee(target)) {
                    spitAt(target);
                }
                if (timer % 600 == 0) {
                    summonMinions(this);
                }
            }
            else {
                if (getY() > -64) {
                    heal(8);
                }
                timeSinceLastAttack = 0;
            }
        }
    }

    private void spitAt(LivingEntity target) {
        InfectiousSpitEntity spit = BSMEntityTypes.INFECTIOUS_SPIT.create(world);
        spit.init(this, target, selectPotionEffects());
        if (!this.isSilent()) {
            this.world.playSound(null, this.getX(), this.getY(), this.getZ(), BSMSounds.ENTITY_GENERIC_SPIT, this.getSoundCategory(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
        }
        this.world.spawnEntity(spit);
        timeSinceLastAttack = 0;
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    private Set<StatusEffectInstance> selectPotionEffects(){
        Map<StatusEffect, Integer> effects = new HashMap<>();
        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            StatusEffect effect = POSSIBLE_EFFECTS[random.nextInt(POSSIBLE_EFFECTS.length)];
            effects.compute(effect, (key, value) -> value != null ? ++value : 1);
        }
        Set<StatusEffectInstance> statusEffects = new HashSet<>();
        for (StatusEffect effect : effects.keySet()) {
            statusEffects.add(new StatusEffectInstance(effect, effect == BWStatusEffects.MORTAL_COIL ? 1200 : 400 / effects.get(effect), effects.get(effect) - 1));
        }
        return statusEffects;
    }
    @Override
    public String getPledgeID() {
        return BSMEntityTypes.BEELZEBUB_PLEDGE;
    }

    @Override
    public Collection<UUID> getPledgedPlayerUUIDs() {
        return pledgedPlayerUUIDS;
    }

    @Override
    public EntityType<?> getMinionType() {
        return random.nextBoolean() ? EntityType.BEE : EntityType.CAVE_SPIDER;
    }

    @Override
    public Collection<StatusEffectInstance> getMinionBuffs() {
        return Sets.newHashSet(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 1), new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 0), new StatusEffectInstance(BWStatusEffects.HARDENING, Integer.MAX_VALUE, 1));
    }

    @Override
    public int getTimeSinceLastAttack() {
        return timeSinceLastAttack;
    }

    @Override
    public void setTimeSinceLastAttack(int timeSinceLastAttack) {
        this.timeSinceLastAttack = timeSinceLastAttack;
    }

    @Override
    public EntityGroup getGroup() {
        return BewitchmentAPI.DEMON;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return BSMSounds.BEELZEBUB_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return BSMSounds.BEELZEBUB_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return BSMSounds.BEELZEBUB_DEATH;
    }


    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (isAlive() && getTarget() == null && BewitchmentAPI.isWerewolf(player, true)) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() == BWObjects.GARLIC_BREAD) {
                boolean client = world.isClient;
                if (!client) {
                    if (!player.isCreative()) {
                        stack.decrement(1);
                    }
                    PlayerLookup.tracking(player).forEach(foundPlayer -> SpawnSmokeParticlesPacket.send(foundPlayer, player));
                    SpawnSmokeParticlesPacket.send(player, player);
                    world.playSound(null, getBlockPos(), BWSoundEvents.ENTITY_GENERIC_PLING, player.getSoundCategory(), 1, 1);
                    if (BWComponents.TRANSFORMATION_COMPONENT.get(player).isAlternateForm()) {
                        TransformationAbilityPacket.useAbility(player, true);
                    }
                    BWComponents.TRANSFORMATION_COMPONENT.get(player).getTransformation().onRemoved(player);
                    BWComponents.TRANSFORMATION_COMPONENT.get(player).setTransformation(BWTransformations.HUMAN);
                    BWComponents.TRANSFORMATION_COMPONENT.get(player).getTransformation().onAdded(player);
                }
                return ActionResult.success(client);
            }
        }
        return super.interactMob(player, hand);
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return false;
    }


    @Override
    public boolean canHaveStatusEffect(StatusEffectInstance effect) {
        return (effect.getEffectType()).getCategory() == StatusEffectCategory.BENEFICIAL;
    }



    @Override
    public boolean isAffectedBySplashPotions() {
        return false;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean flag = super.tryAttack(target);
        if (flag && target instanceof LivingEntity) {
            ((LivingEntity) target).addStatusEffect(new StatusEffectInstance(BWStatusEffects.MORTAL_COIL, 1200));
            ((LivingEntity) target).addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 80, 0));
            target.addVelocity(0, 0.2, 0);
            swingHand(Hand.MAIN_HAND);
        }
        return flag;
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (world.isDay()) {
            target = null;
        }
        super.setTarget(target);
    }

    @Override
    public void setCustomName(@Nullable Text name) {
        super.setCustomName(name);
        bossBar.setName(getDisplayName());
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
        if (hasCustomName()) {
            bossBar.setName(getDisplayName());
        }
        timeSinceLastAttack = tag.getInt("TimeSinceLastAttack");
        fromNbtPledgeable(tag);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        tag.putInt("TimeSinceLastAttack", timeSinceLastAttack);
        toNbtPledgeable(tag);
    }

    @Override
    protected boolean hasShiny() {
        return false;
    }

    @Override
    public int getVariants() {
        return 0;
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1, true));
        goalSelector.add(2, new WanderAroundFarGoal(this, 1));
        goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8));
        goalSelector.add(3, new LookAroundGoal(this));
        targetSelector.add(0, new RevengeGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, LivingEntity.class, 10, true, false, entity -> entity.getGroup() != BewitchmentAPI.DEMON && BWUtil.getArmorPieces(entity, stack -> stack.getItem() instanceof ArmorItem && ((ArmorItem) stack.getItem()).getMaterial() == BWMaterials.BESMIRCHED_ARMOR) < 3 && !(entity instanceof PlayerEntity && BewitchmentAPI.isPledged((PlayerEntity) entity, getPledgeID()))));
    }
}
