package com.drehverschluss.runelessrpg.orb;

import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A mana orb ({@code runelessrpg:mana_orb}). Moves like a vanilla experience orb (gravity, floats up in water, bounces
 * in lava) but never flies to players. Nearby orbs merge into one with a higher {@link #getCount() count}; a touching
 * player takes one orb after the other ({@link ManaOrbs#tryPickUp}).
 * <p>
 * Orbs cannot be destroyed by damage (explosions, fire, lava, attacks); they disappear after
 * {@code orbLifetimeSeconds}, below the world or when mana orbs are turned off.
 */
public class ManaOrb extends Entity {
    private static final EntityDataAccessor<Integer> DATA_COUNT = SynchedEntityData.defineId(ManaOrb.class, EntityDataSerializers.INT);
    private static final double MERGE_DISTANCE = 0.5;

    private int age;

    public ManaOrb(EntityType<? extends ManaOrb> type, Level level) {
        super(type, level);
    }

    /** New orb(s) at a position; {@code scatter} = small random push like experience orbs from a kill. */
    public ManaOrb(Level level, Vec3 pos, int count, boolean scatter) {
        this(ManaOrbs.MANA_ORB.get(), level);
        setPos(pos.x, pos.y, pos.z);
        setYRot((float) (random.nextDouble() * 360.0));
        if (scatter) {
            setDeltaMovement((random.nextDouble() * 0.2 - 0.1) * 2.0, random.nextDouble() * 0.2 * 2.0, (random.nextDouble() * 0.2 - 0.1) * 2.0);
        }
        setCount(count);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COUNT, 1);
    }

    /** Number of orbs merged into this entity (the renderer draws more orbs bigger). */
    public int getCount() {
        return entityData.get(DATA_COUNT);
    }

    void setCount(int count) {
        entityData.set(DATA_COUNT, Math.max(1, count));
    }

    void addCount(int count) {
        setCount(getCount() + count);
        age = 0;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        super.tick();
        xo = getX();
        yo = getY();
        zo = getZ();
        if (isEyeInFluid(FluidTags.WATER)) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x * 0.99F, Math.min(motion.y + 5.0E-4F, 0.06F), motion.z * 0.99F);
        } else {
            applyGravity();
        }
        if (level().getFluidState(blockPosition()).is(FluidTags.LAVA)) {
            // Thrown up like experience orbs, but never burns
            setDeltaMovement((random.nextFloat() - random.nextFloat()) * 0.2F, 0.2F, (random.nextFloat() - random.nextFloat()) * 0.2F);
        }
        if (!level().noCollision(getBoundingBox())) {
            moveTowardsClosestSpace(getX(), (getBoundingBox().minY + getBoundingBox().maxY) / 2.0, getZ());
        }
        if (!level().isClientSide) {
            if (!RunelessServerConfig.MANA_ORBS_ENABLED.get()) {
                discard();
                return;
            }
            if (tickCount % 20 == 1) {
                mergeNearby();
            }
        }
        move(MoverType.SELF, getDeltaMovement());
        float friction = 0.98F;
        if (onGround()) {
            BlockPos below = getBlockPosBelowThatAffectsMyMovement();
            friction = level().getBlockState(below).getFriction(level(), below, this) * 0.98F;
        }
        setDeltaMovement(getDeltaMovement().multiply(friction, 0.98, friction));
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(1.0, -0.9, 1.0));
        }
        age++;
        if (!level().isClientSide && age >= RunelessServerConfig.ORB_LIFETIME_SECONDS.get() * 20) {
            discard();
        }
    }

    @Override
    public BlockPos getBlockPosBelowThatAffectsMyMovement() {
        return getOnPos(0.999999F);
    }

    // All orbs are worth the same, so any orb within the merge distance joins (no vanilla merge groups): fewer entities
    private void mergeNearby() {
        for (ManaOrb other : level().getEntitiesOfClass(ManaOrb.class, getBoundingBox().inflate(MERGE_DISTANCE),
                orb -> orb != this && !orb.isRemoved())) {
            if (isRemoved()) {
                return;
            }
            setCount(getCount() + other.getCount());
            age = Math.min(age, other.age);
            other.discard();
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer && ManaOrbs.tryPickUp(serverPlayer, this)) {
            int left = getCount() - 1;
            if (left <= 0) {
                discard();
            } else {
                setCount(left);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("Age", (short) age);
        tag.putInt("Count", getCount());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getShort("Age");
        setCount(Math.max(tag.getInt("Count"), 1));
    }
}
