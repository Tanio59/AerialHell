package fr.factionbedrock.aerialhell.Entity.Passive;

import fr.factionbedrock.aerialhell.Entity.AI.GlideGoal;
import fr.factionbedrock.aerialhell.Entity.AerialHellAnimalEntity;
import fr.factionbedrock.aerialhell.Registry.Entities.AerialHellEntities;
import fr.factionbedrock.aerialhell.Registry.AerialHellSoundEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class GlidingTurtleEntity extends AerialHellAnimalEntity implements PlayerRideableJumping
{
    public static final EntityDataAccessor<Boolean> GLIDING = SynchedEntityData.<Boolean>defineId(GlidingTurtleEntity.class, EntityDataSerializers.BOOLEAN);
    private int ateTimer;

    //ridden jump
    private static final double RIDDEN_JUMP_MIN_POWER = 0.5D, RIDDEN_JUMP_MAX_POWER = 1.4D, RIDDEN_JUMP_FORWARD_POWER = 0.6D;
    private static final int RIDDEN_JUMP_COOLDOWN = 20;
    private float playerJumpPendingScale;
    private int ridingJumpCooldown;

    //ridden glide
    private static final double GLIDE_GRAVITY = 0.01D;
    private static final double GLIDE_CRUISE_SPEED = 0.9D, GLIDE_IDLE_SPEED = 0.4D, GLIDE_DIVE_SPEED_BONUS = 0.9D, GLIDE_CLIMB_SPEED_MALUS = 0.5D; //target speeds, actual speed is ~60% of target because of air friction
    private static final double GLIDE_BASE_MAX_FALL_SPEED = 0.08D, GLIDE_DIVE_MAX_FALL_SPEED_BONUS = 0.4D, GLIDE_MIN_MAX_FALL_SPEED = 0.04D;
    private static final double GLIDE_STEERING = 0.15D, GLIDE_LIFT_MIN_SPEED = 0.35D, GLIDE_LIFT_FACTOR = 0.25D;

    public GlidingTurtleEntity(EntityType<? extends GlidingTurtleEntity> type, Level worldIn) {super(type, worldIn);}

    public GlidingTurtleEntity(Level worldIn) {this(AerialHellEntities.GLIDING_TURTLE.get(), worldIn);}

    public InteractionResult mobInteract(Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (this.isFood(stack)) {this.ateTimer = 12000; return super.mobInteract(player, hand);}

        if (this.isSaddled() && !this.isVehicle() && !player.isSecondaryUseActive())
        {
            if (!this.level().isClientSide()) {player.startRiding(this);}
            return InteractionResult.SUCCESS;
        }

        InteractionResult result = super.mobInteract(player, hand);
        if (!result.consumesAction() && this.isEquippableInSlot(stack, EquipmentSlot.SADDLE)) {return stack.interactLivingEntity(player, this, hand);}
        return result;
    }

    @Override public void tick()
    {
        if (this.ateTimer > 0) {this.ateTimer--;}
        else if (!this.isAteTimerInBounds()) {this.ateTimer = 0;}
        if (this.ridingJumpCooldown > 0) {this.ridingJumpCooldown--;}
        super.tick();
        //when ridden, GlideGoal is disabled : wings are spread as long as the turtle is in the air
        if (!this.level().isClientSide() && this.getControllingPassenger() instanceof Player) {this.setGliding(!this.onGround() && !this.isInWater());}
    }

    private boolean isAteTimerInBounds() {return this.ateTimer >=0 && this.ateTimer <= 6000;}

    @Override protected void registerGoals()
    {
        super.registerGoals();
        this.goalSelector.addGoal(0, new GlideGoal(this));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        super.defineSynchedData(builder);
        builder.define(GLIDING, false);
    }

    public static AttributeSupplier.Builder registerAttributes()
    {
        return AerialHellAnimalEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.TEMPT_RANGE, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26);
    }

    @Nullable @Override public AgeableMob getBreedOffspring(ServerLevel world, AgeableMob mob)
    {
        return AerialHellEntities.GLIDING_TURTLE.get().create(world, EntitySpawnReason.BREEDING);
    }

    public boolean isGliding() {return !this.entityData.get(GLIDING);}
    public void setGliding(boolean flag) {this.entityData.set(GLIDING, !flag);}

    @Override public boolean causeFallDamage(double distance, float damageMultiplier, DamageSource source) {return false;}

    /* ---------------- Riding ---------------- */

    @Override public boolean canUseSlot(EquipmentSlot slot) {return slot != EquipmentSlot.SADDLE ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby();}
    @Override protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {return slot == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(slot);}

    @Nullable @Override public LivingEntity getControllingPassenger()
    {
        return this.isSaddled() && this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
    }

    @Override protected void tickRidden(Player controller, Vec3 riddenInput)
    {
        super.tickRidden(controller, riddenInput);
        this.setRot(controller.getYRot(), controller.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();

        if (this.isLocalInstanceAuthoritative())
        {
            if (this.onGround())
            {
                if (this.playerJumpPendingScale > 0.0F) {this.executeRidersJump(controller, this.playerJumpPendingScale);}
                this.playerJumpPendingScale = 0.0F;
            }
            else if (this.isRiddenGliding()) {this.applyRiddenGlideMotion(controller);}
        }
    }

    @Override protected Vec3 getRiddenInput(Player controller, Vec3 selfInput)
    {
        if (this.isRiddenGliding()) {return Vec3.ZERO;} //glide motion is fully handled by applyRiddenGlideMotion
        float forward = controller.zza;
        if (forward <= 0.0F) {forward *= 0.25F;}
        return new Vec3(controller.xxa * 0.5F, 0.0D, forward);
    }

    @Override protected float getRiddenSpeed(Player controller) {return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);}

    @Override protected double getEffectiveGravity()
    {
        return this.isRiddenGliding() ? Math.min(super.getEffectiveGravity(), GLIDE_GRAVITY) : super.getEffectiveGravity();
    }

    private boolean isRiddenGliding()
    {
        return this.getControllingPassenger() instanceof Player && !this.onGround() && !this.isInWater() && this.getDeltaMovement().y <= 0.0D;
    }

    //rider looks down = dive (faster, steeper), rider looks up = flare (slower, can convert speed into a bit of height)
    private void applyRiddenGlideMotion(Player controller)
    {
        float pitch = Mth.clamp(controller.getXRot(), -45.0F, 60.0F);
        double dive = Math.max(0.0F, pitch) / 60.0D;
        double climb = Math.max(0.0F, -pitch) / 45.0D;

        Vec3 motion = this.getDeltaMovement();
        Vec3 heading = Vec3.directionFromRotation(0.0F, this.getYRot());
        double horizontalSpeed = motion.horizontalDistance();

        double targetSpeed = (controller.zza > 0.0F ? GLIDE_CRUISE_SPEED : GLIDE_IDLE_SPEED) + dive * GLIDE_DIVE_SPEED_BONUS - climb * GLIDE_CLIMB_SPEED_MALUS;
        targetSpeed = Math.max(0.1D, targetSpeed);
        double motionX = Mth.lerp(GLIDE_STEERING, motion.x, heading.x * targetSpeed);
        double motionZ = Mth.lerp(GLIDE_STEERING, motion.z, heading.z * targetSpeed);

        double lift = horizontalSpeed > GLIDE_LIFT_MIN_SPEED ? (horizontalSpeed - GLIDE_LIFT_MIN_SPEED) * GLIDE_LIFT_FACTOR * climb : 0.0D;
        double maxFallSpeed = Math.max(GLIDE_MIN_MAX_FALL_SPEED, GLIDE_BASE_MAX_FALL_SPEED + dive * GLIDE_DIVE_MAX_FALL_SPEED_BONUS - climb * GLIDE_MIN_MAX_FALL_SPEED);
        double motionY = Math.max(motion.y + lift, -maxFallSpeed);

        this.setDeltaMovement(motionX, motionY, motionZ);
        this.resetFallDistance();
    }

    private void executeRidersJump(Player controller, float scale)
    {
        Vec3 heading = Vec3.directionFromRotation(0.0F, this.getYRot());
        double forwardPower = RIDDEN_JUMP_FORWARD_POWER * scale * (controller.zza > 0.0F ? 1.0D : 0.3D);
        double upPower = Mth.lerp(scale, RIDDEN_JUMP_MIN_POWER, RIDDEN_JUMP_MAX_POWER);
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x + heading.x * forwardPower, upPower, motion.z + heading.z * forwardPower);
        this.ridingJumpCooldown = RIDDEN_JUMP_COOLDOWN;
        this.needsSync = true;
        net.neoforged.neoforge.common.CommonHooks.onLivingJump(this);
    }

    @Override public void onPlayerJump(int jumpAmount)
    {
        if (this.isSaddled() && this.onGround() && this.ridingJumpCooldown <= 0)
        {
            this.playerJumpPendingScale = this.getPlayerJumpPendingScale(Math.max(0, jumpAmount));
        }
    }

    @Override public boolean canJump() {return this.isSaddled();}
    @Override public void handleStartJump(int jumpScale) {if (this.onGround()) {this.playSound(SoundEvents.PHANTOM_FLAP, 0.8F, 0.8F + this.random.nextFloat() * 0.3F);}}
    @Override public void handleStopJump() {}
    @Override public int getJumpCooldown() {return this.ridingJumpCooldown;}

    /* ---------------- Sounds & save data ---------------- */

    @Override public int getAmbientSoundInterval() {return 640;}
    @Override protected float getSoundVolume() {return 0.7F;}
    @Override protected SoundEvent getAmbientSound() {return AerialHellSoundEvents.ENTITY_GLIDING_TURTLE_AMBIENT.get();}
    @Override protected SoundEvent getHurtSound(DamageSource damageSourceIn) {return AerialHellSoundEvents.ENTITY_GLIDING_TURTLE_HURT.get();}
    @Override protected SoundEvent getDeathSound() {return AerialHellSoundEvents.ENTITY_GLIDING_TURTLE_DEATH.get();}
    @Override public void playAmbientSound()
    {
        SoundEvent ambientSound = this.getAmbientSound();
        if (ambientSound != null) {this.playSound(ambientSound, this.ateTimer <= 0 ? this.getSoundVolume() : 0.0F, this.getVoicePitch());}
    }

    @Override public void addAdditionalSaveData(ValueOutput valueOutput)
    {
        super.addAdditionalSaveData(valueOutput);
        valueOutput.putBoolean("Glide", this.isGliding());
        valueOutput.putInt("AteTimer", this.ateTimer);
    }

    @Override public void readAdditionalSaveData(ValueInput valueInput)
    {
        super.readAdditionalSaveData(valueInput);
        this.setGliding(valueInput.getBooleanOr("Glide", false));
        if (valueInput.getInt("AteTimer").isPresent()) {this.ateTimer = valueInput.getInt("AteTimer").get();}

    }
}
