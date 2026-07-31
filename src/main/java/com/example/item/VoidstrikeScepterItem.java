package com.example.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VoidstrikeScepterItem extends Item {

    private static final double BOLT_RANGE = 24.0;
    private static final double CHAIN_JUMP_RADIUS = 6.0;
    private static final int MAX_CHAIN_JUMPS = 3;
    private static final float BASE_DAMAGE = 7.0f;
    private static final float CHAIN_DAMAGE_FALLOFF = 0.7f;
    private static final int WITHER_DURATION = 60;
    private static final int PRIMARY_COOLDOWN_TICKS = 20;

    private static final double NOVA_RADIUS = 6.0;
    private static final float NOVA_DAMAGE = 5.0f;
    private static final double NOVA_KNOCKBACK = 1.1;
    private static final int NOVA_GLOW_DURATION = 100;
    private static final int NOVA_LEVITATION_DURATION = 15;
    private static final int NOVA_COOLDOWN_TICKS = 100;

    public VoidstrikeScepterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        boolean nova = player.isShiftKeyDown();

        if (!world.isClientSide()) {
            if (nova) {
                castNova(world, player);
            } else {
                castChainBolt(world, player);
            }
        }

        player.getCooldowns().addCooldown(stack, nova ? NOVA_COOLDOWN_TICKS : PRIMARY_COOLDOWN_TICKS);

        world.playSound(player, player.blockPosition(),
                nova ? SoundEvents.BEACON_ACTIVATE : SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS, 1.0f, nova ? 0.7f : 1.3f);

        return InteractionResult.SUCCESS;
    }

    private void castChainBolt(Level world, Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(BOLT_RANGE));

        ClipContext blockCtx = new ClipContext(
                start, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        );
        HitResult blockHit = world.clip(blockCtx);
        double maxDist = blockHit.getLocation().distanceTo(start);

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(BOLT_RANGE)).inflate(1.0);
        List<Entity> candidates = world.getEntities(player, searchBox,
                e -> e instanceof LivingEntity && e.isAlive());

        LivingEntity firstHit = null;
        Vec3 firstHitPoint = null;
        double closestDist = maxDist;

        for (Entity entity : candidates) {
            AABB box = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = box.clip(start, end);
            if (hit.isPresent()) {
                double dist = start.distanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    firstHit = (LivingEntity) entity;
                    firstHitPoint = hit.get();
                }
            }
        }

        if (world instanceof ServerLevel serverWorld) {
            Vec3 beamEnd = firstHit != null ? firstHitPoint : start.add(look.scale(closestDist));
            drawBolt(serverWorld, start, beamEnd);
        }

        if (firstHit == null) {
            return;
        }

        Set<Entity> struck = new HashSet<>();
        struck.add(player);
        struck.add(firstHit);

        List<LivingEntity> chainOrder = new ArrayList<>();
        chainOrder.add(firstHit);

        Vec3 jumpOrigin = firstHitPoint;
        LivingEntity previous = firstHit;

        for (int jump = 0; jump < MAX_CHAIN_JUMPS; jump++) {
            AABB jumpBox = new AABB(jumpOrigin, jumpOrigin).inflate(CHAIN_JUMP_RADIUS);
            List<Entity> nearby = world.getEntities(previous, jumpBox,
                    e -> e instanceof LivingEntity && e.isAlive() && !struck.contains(e));

            LivingEntity next = null;
            double nextDist = Double.MAX_VALUE;
            for (Entity entity : nearby) {
                double dist = entity.position().distanceTo(jumpOrigin);
                if (dist < nextDist) {
                    nextDist = dist;
                    next = (LivingEntity) entity;
                }
            }

            if (next == null) {
                break;
            }

            Vec3 nextPoint = next.position().add(0, next.getBbHeight() * 0.5, 0);
            if (world instanceof ServerLevel serverWorld) {
                drawBolt(serverWorld, jumpOrigin, nextPoint);
            }

            chainOrder.add(next);
            struck.add(next);
            jumpOrigin = nextPoint;
            previous = next;
        }

        float damage = BASE_DAMAGE;
        for (LivingEntity target : chainOrder) {
            target.hurt(world.damageSources().indirectMagic(player, player), damage);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION, 0));
            damage *= CHAIN_DAMAGE_FALLOFF;
        }
    }

    private void drawBolt(ServerLevel world, Vec3 from, Vec3 to) {
        double length = from.distanceTo(to);
        double step = 0.4;
        Vec3 dir = to.subtract(from);
        int steps = Math.max(1, (int) (length / step));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.add(dir.scale((double) i / steps));
            world.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        world.sendParticles(ParticleTypes.WITCH, to.x, to.y, to.z, 6, 0.2, 0.2, 0.2, 0.01);
    }

    private void castNova(Level world, Player player) {
        Vec3 center = player.position();
        AABB box = new AABB(center, center).inflate(NOVA_RADIUS);
        List<Entity> nearby = world.getEntities(player, box,
                e -> e instanceof LivingEntity && e.isAlive());

        for (Entity entity : nearby) {
            LivingEntity living = (LivingEntity) entity;
            double dist = living.position().distanceTo(center);
            if (dist > NOVA_RADIUS) {
                continue;
            }

            living.hurt(world.damageSources().indirectMagic(player, player), NOVA_DAMAGE);

            double dx = living.getX() - center.x;
            double dz = living.getZ() - center.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 0.001) {
                dx = 1.0;
                dz = 0.0;
                len = 1.0;
            }
            Vec3 push = new Vec3(dx / len * NOVA_KNOCKBACK, 0.35, dz / len * NOVA_KNOCKBACK);
            living.setDeltaMovement(living.getDeltaMovement().add(push));

            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, NOVA_GLOW_DURATION, 0));
            living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, NOVA_LEVITATION_DURATION, 0));
        }

        if (world instanceof ServerLevel serverWorld) {
            for (int ring = 1; ring <= 5; ring++) {
                double radius = ring * (NOVA_RADIUS / 5.0);
                int points = 8 + ring * 4;
                for (int p = 0; p < points; p++) {
                    double angle = (2 * Math.PI * p) / points;
                    double x = center.x + radius * Math.cos(angle);
                    double z = center.z + radius * Math.sin(angle);
                    serverWorld.sendParticles(ParticleTypes.REVERSE_PORTAL, x, center.y + 0.1, z, 1, 0, 0.02, 0, 0.0);
                }
            }
            serverWorld.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 1.0, center.z, 1, 0, 0, 0, 0);
        }
    }
}
