package dev.mrsterner.besmirchment.common.entity;

import dev.mrsterner.besmirchment.common.entity.interfaces.VillagerWerepyreAccessor;
import moriyashiine.bewitchment.api.BewitchmentAPI;
import moriyashiine.bewitchment.client.network.packet.SpawnSmokeParticlesPacket;
import moriyashiine.bewitchment.common.entity.living.util.BWHostileEntity;
import moriyashiine.bewitchment.common.registry.BWComponents;
import moriyashiine.bewitchment.common.registry.BWSoundEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class WerepyreEntity extends BWHostileEntity {
    public static final TrackedData<Integer> JUMP_TICKS = DataTracker.registerData(WerepyreEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public NbtCompound storedVillager;

    @Environment(EnvType.CLIENT)
    public float jumpBeginProgress = 0;

    public WerepyreEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public void tick() {
        super.tick();
        if (!world.isClient && world.isDay() && !world.isRaining() && world.isSkyVisible(getBlockPos())) {
            setOnFireFor(8);
        }
        if (storedVillager != null && age % 20 == 0 && (world.isDay() || BewitchmentAPI.getMoonPhase(world) != 0)) {
            VillagerEntity entity = EntityType.VILLAGER.create(world);
            if (entity instanceof VillagerWerepyreAccessor) {
                PlayerLookup.tracking(this).forEach(player -> SpawnSmokeParticlesPacket.send(player, this));
                world.playSound(null, getX(), getY(), getZ(), BWSoundEvents.ENTITY_GENERIC_TRANSFORM, getSoundCategory(), getSoundVolume(), getSoundPitch());
                entity.readNbt(storedVillager);
                entity.updatePositionAndAngles(getX(), getY(), getZ(), random.nextFloat() * 360, 0);
                entity.setHealth(entity.getMaxHealth() * (getHealth() / getMaxHealth()));
                entity.setFireTicks(getFireTicks());
                entity.clearStatusEffects();
                getStatusEffects().forEach(entity::addStatusEffect);
                BWComponents.CURSES_COMPONENT.get(this).getCurses().clear();
                BWComponents.CURSES_COMPONENT.get(this).getCurses().forEach(BWComponents.CURSES_COMPONENT.get(this)::addCurse);
                ((VillagerWerepyreAccessor) entity).setStoredWerepyre(writeNbt(new NbtCompound()));
                world.spawnEntity(entity);
                remove(RemovalReason.DISCARDED);
            }
        }
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (getLastJumpTime() < 200) {
            setLastJumpTime(getLastJumpTime() + 1);
        }
        if (world.isClient) {
            if (getLastJumpTime() > 20) {
                if (jumpBeginProgress > 0) {
                    jumpBeginProgress -= 0.1;
                }
            } else if (jumpBeginProgress < 1) {
                jumpBeginProgress += 0.1;
            }
        }
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return super.handleFallDamage(Math.max(fallDistance - 12, 0), damageMultiplier, damageSource);
    }


    protected boolean hasShiny() {
        return true;
    }

    public int getVariants() {
        return getVariantsStatic();
    }

    public EntityGroup getGroup() {
        return BewitchmentAPI.DEMON;
    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        return BWSoundEvents.ENTITY_WEREWOLF_AMBIENT;
    }

    protected SoundEvent getHurtSound(DamageSource source) {
        return BWSoundEvents.ENTITY_WEREWOLF_HURT;
    }

    protected SoundEvent getDeathSound() {
        return BWSoundEvents.ENTITY_WEREWOLF_DEATH;
    }

    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityTag) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityTag);
        if (this.dataTracker.get(VARIANT) != 0) {
            RegistryEntry<Biome> biome = world.getBiome(getBlockPos());

            if (biome.isIn(ConventionalBiomeTags.FOREST)) {
                dataTracker.set(VARIANT, random.nextBoolean() ? 2 : 3);
            } else if (biome.isIn(ConventionalBiomeTags.TAIGA)) {
                dataTracker.set(VARIANT, random.nextBoolean() ? 1 : 4);
            }
            if (biome.isIn(ConventionalBiomeTags.ICY)) {
                dataTracker.set(VARIANT, random.nextBoolean() ? 3 : 4);
            } else {
                dataTracker.set(VARIANT, this.random.nextInt(this.getVariants() - 1) + 1);
            }
        }

        if (spawnReason == SpawnReason.NATURAL || spawnReason == SpawnReason.SPAWN_EGG) {
            storedVillager = EntityType.VILLAGER.create((World) world).writeNbt(new NbtCompound());
        }
        return data;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(JUMP_TICKS, 40);
    }

    public int getLastJumpTime() {
        return dataTracker.get(JUMP_TICKS);
    }

    public void setLastJumpTime(int ticks) {
        dataTracker.set(JUMP_TICKS, ticks);
    }

    public void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
        setLastJumpTime(tag.getInt("LastJumpTicks"));
        if (tag.contains("StoredVillager")) {
            storedVillager = tag.getCompound("StoredVillager");
        }
    }


    public void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        tag.putInt("LastJumpTicks", getLastJumpTime());
        if (storedVillager != null) {
            tag.put("StoredVillager", storedVillager);
        }
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new PounceAtTargetGoal());
        goalSelector.add(2, new MeleeAttackGoal(this, 1, true));
        goalSelector.add(3, new WanderAroundFarGoal(this, 1));
        goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8));
        goalSelector.add(5, new LookAroundGoal(this));
        targetSelector.add(0, new RevengeGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, LivingEntity.class, 10, true, false, entity -> entity instanceof PlayerEntity || entity instanceof SheepEntity || entity instanceof MerchantEntity || entity.getGroup() == EntityGroup.ILLAGER));
    }

    public static int getVariantsStatic() {
        return 5;
    }

    public boolean canJump() {
        return getLastJumpTime() > 5;
    }

    public static boolean isValidMoonPhase(WorldAccess world) {
        int phase = BewitchmentAPI.getMoonPhase(world);
        return phase == 0;
    }

    public static boolean canSpawn(EntityType<WerepyreEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        return world.getDifficulty() != Difficulty.PEACEFUL && world.getLevelProperties().getTime() > 24000L && isValidMoonPhase(world) && HostileEntity.canSpawnInDark(type, world, spawnReason, pos, random);
    }

    public class PounceAtTargetGoal extends Goal {
        private LivingEntity target;

        public PounceAtTargetGoal() {
            this.setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
        }

        public boolean canStart() {
            this.target = getTarget();
            if (this.target == null) {
                return false;
            } else {
                double d = distanceTo(this.target);
                if (d >= 3 && d <= 14.0D) {
                    if (!canJump()) {
                        return false;
                    } else {
                        return getRandom().nextInt(5) == 0;
                    }
                } else {
                    return false;
                }
            }
        }

        public boolean shouldContinue() {
            return !canJump();
        }

        public void start() {
            setLastJumpTime(0);
            lookAtEntity(this.target, 60, 60);
            Vec3d vec3d = getVelocity();
            Vec3d vec3d2 = new Vec3d(this.target.getX() - getX(), this.target.getY() + 1 - getY(), this.target.getZ() - getZ());
            if (vec3d2.lengthSquared() > 1.0E-7D) {
                vec3d2 = vec3d2.normalize().multiply(0.8D).add(vec3d.multiply(0.2D));
            }
            setVelocity(vec3d2.x, vec3d2.y, vec3d2.z);
        }
    }


}
