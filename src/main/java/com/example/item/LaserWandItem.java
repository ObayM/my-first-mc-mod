package com.example.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class LaserWandItem extends Item {

    private static final double RANGE = 30.0;
    private static final float DAMAGE = 6.0f;

    public LaserWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide()) {
            fireLaser(world, player);
        }
        world.playSound(player, player.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 1.0f, 1.0f);

        return InteractionResult.SUCCESS;
    }

    private void fireLaser(Level world, Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(RANGE));

        ClipContext blockCtx = new ClipContext(
                start, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        );
        HitResult blockHit = world.clip(blockCtx);
        double maxDist = blockHit.getLocation().distanceTo(start);

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0);
        List<Entity> candidates = world.getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e.isAlive());

        LivingEntity hitEntity = null;
        double closestDist = maxDist;

        for (Entity entity : candidates) {
            AABB box = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = box.clip(start, end);
            if (hit.isPresent()) {
                double dist = start.distanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    hitEntity = (LivingEntity) entity;
                }
            }
        }

        if (hitEntity != null) {
            hitEntity.hurt(world.damageSources().playerAttack(player), DAMAGE);
            hitEntity.setDeltaMovement(hitEntity.getDeltaMovement().add(look.scale(0.3)));
        }

        if (world instanceof ServerLevel serverWorld) {
            double step = 0.5;
            for (double travelled = 0; travelled < closestDist; travelled += step) {
                Vec3 point = start.add(look.scale(travelled));
                serverWorld.sendParticles(ParticleTypes.END_ROD,
                        point.x, point.y, point.z,
                        1, 0, 0, 0, 0);
            }
        }
    }
}
