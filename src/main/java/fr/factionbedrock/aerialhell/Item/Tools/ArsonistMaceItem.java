package fr.factionbedrock.aerialhell.Item.Tools;

import fr.factionbedrock.aerialhell.Item.AerialHellItem;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

//same smash attack as vanilla MaceItem (fall based damage, knockback around target, no fall damage after hit)
//it is an AerialHellItem (and not a MaceItem) to be able to use item abilities (see AerialHellItemAbilities.ARSONIST_MACE)
public class ArsonistMaceItem extends AerialHellItem
{
    private static final float SMASH_ATTACK_HEAVY_THRESHOLD = 5.0F;
    private static final float SMASH_ATTACK_KNOCKBACK_RADIUS = 3.5F;
    private static final float SMASH_ATTACK_KNOCKBACK_POWER = 0.7F;

    public ArsonistMaceItem(AerialHellItem.Properties properties) {super(properties);}

    @Override public void hurtEnemy(ItemStack itemStack, LivingEntity target, LivingEntity attacker)
    {
        if (MaceItem.canSmashAttack(attacker) && attacker.level() instanceof ServerLevel level)
        {
            attacker.setDeltaMovement(attacker.getDeltaMovement().with(Direction.Axis.Y, 0.01F));
            attacker.setIgnoreFallDamageFromCurrentImpulse(true, this.calculateImpactPosition(attacker));
            if (attacker instanceof ServerPlayer player) {player.connection.send(new ClientboundSetEntityMotionPacket(player));}

            if (target.onGround())
            {
                if (attacker instanceof ServerPlayer player) {player.setSpawnExtraParticlesOnFall(true);}
                SoundEvent sound = attacker.fallDistance > SMASH_ATTACK_HEAVY_THRESHOLD ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
                level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), sound, attacker.getSoundSource(), 1.0F, 1.0F);
            }
            else {level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.MACE_SMASH_AIR, attacker.getSoundSource(), 1.0F, 1.0F);}

            knockback(level, attacker, target);
        }
        super.hurtEnemy(itemStack, target, attacker);
    }

    private Vec3 calculateImpactPosition(LivingEntity attacker)
    {
        return attacker.isIgnoringFallDamageFromCurrentImpulse() && attacker.currentImpulseImpactPos != null && attacker.currentImpulseImpactPos.y <= attacker.position().y ? attacker.currentImpulseImpactPos : attacker.position();
    }

    @Override public void postHurtEnemy(ItemStack itemStack, LivingEntity target, LivingEntity attacker)
    {
        if (MaceItem.canSmashAttack(attacker)) {attacker.resetFallDistance();}
        super.postHurtEnemy(itemStack, target, attacker);
    }

    @Override public float getAttackDamageBonus(Entity victim, float ignoredDamage, DamageSource damageSource)
    {
        if (!(damageSource.getDirectEntity() instanceof LivingEntity attacker) || !MaceItem.canSmashAttack(attacker)) {return 0.0F;}

        double fallDistance = attacker.fallDistance;
        double damage;
        if (fallDistance <= 3.0D) {damage = 4.0D * fallDistance;}
        else if (fallDistance <= 8.0D) {damage = 12.0D + 2.0D * (fallDistance - 3.0D);}
        else {damage = 22.0D + fallDistance - 8.0D;}

        return attacker.level() instanceof ServerLevel level ? (float) (damage + EnchantmentHelper.modifyFallBasedDamage(level, attacker.getWeaponItem(), victim, damageSource, 0.0F) * fallDistance) : (float) damage;
    }

    @Nullable @Override public DamageSource getItemDamageSource(LivingEntity attacker)
    {
        return MaceItem.canSmashAttack(attacker) ? attacker.damageSources().mace(attacker) : super.getItemDamageSource(attacker);
    }

    private static void knockback(ServerLevel level, Entity attacker, Entity target)
    {
        level.levelEvent(2013, target.getOnPos(), 750);
        level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(SMASH_ATTACK_KNOCKBACK_RADIUS), knockbackPredicate(attacker, target)).forEach(nearby ->
        {
            Vec3 direction = nearby.position().subtract(target.position());
            double knockbackPower = getKnockbackPower(attacker, nearby, direction);
            if (knockbackPower > 0.0D)
            {
                Vec3 knockbackVector = direction.normalize().scale(knockbackPower);
                nearby.push(knockbackVector.x, SMASH_ATTACK_KNOCKBACK_POWER, knockbackVector.z);
                if (nearby instanceof ServerPlayer otherPlayer) {otherPlayer.connection.send(new ClientboundSetEntityMotionPacket(otherPlayer));}
            }
        });
    }

    private static Predicate<LivingEntity> knockbackPredicate(Entity attacker, Entity target)
    {
        return nearby ->
        {
            boolean notSpectator = !nearby.isSpectator();
            boolean notAttackerOrTarget = nearby != attacker && nearby != target;
            boolean notAlliedToAttacker = !attacker.isAlliedTo(nearby);
            boolean notTamedByAttacker = !(nearby instanceof TamableAnimal animal && attacker instanceof LivingEntity livingAttacker && animal.isTame() && animal.isOwnedBy(livingAttacker));
            boolean notMarkerArmorStand = !(nearby instanceof ArmorStand armorStand && armorStand.isMarker());
            boolean withinRange = target.distanceToSqr(nearby) <= SMASH_ATTACK_KNOCKBACK_RADIUS * SMASH_ATTACK_KNOCKBACK_RADIUS;
            boolean notFlyingInCreative = !(nearby instanceof Player player && player.isCreative() && player.getAbilities().flying);
            return notSpectator && notAttackerOrTarget && notAlliedToAttacker && notTamedByAttacker && notMarkerArmorStand && withinRange && notFlyingInCreative;
        };
    }

    private static double getKnockbackPower(Entity attacker, LivingEntity nearby, Vec3 direction)
    {
        return (SMASH_ATTACK_KNOCKBACK_RADIUS - direction.length()) * SMASH_ATTACK_KNOCKBACK_POWER * (attacker.fallDistance > SMASH_ATTACK_HEAVY_THRESHOLD ? 2 : 1) * (1.0D - nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    }
}
