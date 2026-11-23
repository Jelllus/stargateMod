package com.stargate.mod.replicator;

import com.stargate.mod.StargateMod;
import com.stargate.mod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * Replicator (SG-1): a small hostile machine made of metal blocks. It climbs walls like a spider, hunts
 * players and iron golems in swarms, and <b>replicates</b>: it seeks iron items lying on the ground
 * ({@link #FOOD} tag), eats them, and once it has absorbed {@link #REPLICATION_COST} of them assembles a new
 * replicator next to itself — as long as fewer than {@link #MAX_NEARBY} replicators are already around.
 *
 * <p><i>FR —</i> Réplicateur (SG-1) : petite machine hostile faite de blocs de métal. Il grimpe aux murs
 * comme une araignée, chasse les joueurs et les golems de fer en essaim, et <b>se réplique</b> : il cherche
 * les objets en fer posés au sol (tag {@link #FOOD}), les dévore, et une fois {@link #REPLICATION_COST}
 * objets absorbés assemble un nouveau réplicateur à côté de lui — tant qu'il y en a moins de
 * {@link #MAX_NEARBY} aux alentours.
 */
public class ReplicatorEntity extends Monster {
    /** Items a replicator eats to replicate. / <i>FR —</i> Objets dont se nourrit un réplicateur pour se répliquer. */
    public static final TagKey<Item> FOOD =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(StargateMod.MODID, "replicator_food"));

    /** Items to eat for one new replicator. / <i>FR —</i> Objets à dévorer pour un nouveau réplicateur. */
    public static final int REPLICATION_COST = 4;
    /** Replication stops when this many replicators are within {@link #CROWD_RADIUS}. / <i>FR —</i> La réplication s'arrête à partir de ce nombre de réplicateurs dans {@link #CROWD_RADIUS}. */
    public static final int MAX_NEARBY = 12;
    /** Radius of the crowd check, in blocks. / <i>FR —</i> Rayon du test de population, en blocs. */
    public static final double CROWD_RADIUS = 24.0;
    /** Ticks between two bites. / <i>FR —</i> Ticks entre deux bouchées. */
    private static final int EAT_COOLDOWN = 10;

    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID =
            SynchedEntityData.defineId(ReplicatorEntity.class, EntityDataSerializers.BYTE);
    private static final byte FLAG_CLIMBING = 1;

    /** Items eaten since the last replication. / <i>FR —</i> Objets dévorés depuis la dernière réplication. */
    private int material;
    private int eatCooldown;

    /**
     * Creates a replicator; called through the entity type.
     *
     * <p><i>FR —</i> Crée un réplicateur ; appelé via le type d'entité.
     *
     * @param type  the entity type / le type d'entité
     * @param level the level / le niveau
     */
    public ReplicatorEntity(EntityType<? extends ReplicatorEntity> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    /**
     * Base attributes: fragile but fast, armoured, hard to push.
     *
     * <p><i>FR —</i> Attributs de base : fragile mais rapide, blindé, difficile à repousser.
     *
     * @return the attribute builder / le constructeur d'attributs
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.35F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(4, new SeekMetalGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // Hit one and the whole swarm answers. / Frapper l'un d'eux, c'est attirer tout l'essaim.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_FLAGS_ID, (byte) 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.setClimbing(this.horizontalCollision);
            if (this.eatCooldown > 0) {
                this.eatCooldown--;
            }
        }
    }

    // ---- Wall climbing / Escalade ------------------------------------------------------------------

    @Override
    public boolean onClimbable() {
        return this.isClimbing();
    }

    /**
     * Whether the replicator is currently climbing a wall (synced to clients).
     *
     * <p><i>FR —</i> Indique si le réplicateur escalade un mur (synchronisé avec les clients).
     *
     * @return {@code true} while climbing / {@code true} pendant l'escalade
     */
    public boolean isClimbing() {
        return (this.entityData.get(DATA_FLAGS_ID) & FLAG_CLIMBING) != 0;
    }

    private void setClimbing(boolean climbing) {
        byte flags = this.entityData.get(DATA_FLAGS_ID);
        this.entityData.set(DATA_FLAGS_ID, climbing ? (byte) (flags | FLAG_CLIMBING) : (byte) (flags & ~FLAG_CLIMBING));
    }

    @Override
    public void makeStuckInBlock(BlockState state, net.minecraft.world.phys.Vec3 speedMultiplier) {
        if (!state.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(state, speedMultiplier);
        }
    }

    // ---- Replication / Réplication -----------------------------------------------------------------

    /**
     * Replicators always look for loot: that is how they find metal.
     *
     * <p><i>FR —</i> Les réplicateurs ramassent toujours : c'est ainsi qu'ils trouvent le métal.
     */
    @Override
    public boolean canPickUpLoot() {
        return true;
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack itemStack) {
        return itemStack.is(FOOD) && this.eatCooldown == 0 && this.canGrow(level);
    }

    /**
     * Eats one item from the stack instead of equipping it; replicates once enough metal is absorbed.
     *
     * <p><i>FR —</i> Dévore un objet de la pile au lieu de s'en équiper ; se réplique une fois assez de
     * métal absorbé.
     */
    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity entity) {
        ItemStack stack = entity.getItem();
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.getItem()),
                this.getX(), this.getY() + 0.2, this.getZ(), 8, 0.15, 0.1, 0.15, 0.05);
        this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 0.6F, 1.6F + this.random.nextFloat() * 0.3F);
        this.onItemPickup(entity);
        this.take(entity, 1);
        stack.shrink(1);
        if (stack.isEmpty()) {
            entity.discard();
        }
        this.eatCooldown = EAT_COOLDOWN;
        if (++this.material >= REPLICATION_COST) {
            this.material -= REPLICATION_COST;
            this.replicate(level);
        }
    }

    /** Room left for another replicator nearby. / <i>FR —</i> Reste-t-il de la place pour un réplicateur de plus. */
    private boolean canGrow(ServerLevel level) {
        List<ReplicatorEntity> crowd = level.getEntitiesOfClass(ReplicatorEntity.class,
                this.getBoundingBox().inflate(CROWD_RADIUS));
        return crowd.size() < MAX_NEARBY;
    }

    /**
     * Assembles a new replicator next to this one.
     *
     * <p><i>FR —</i> Assemble un nouveau réplicateur à côté de celui-ci.
     *
     * @param level the server level / le niveau serveur
     */
    private void replicate(ServerLevel level) {
        BlockPos pos = this.blockPosition();
        ReplicatorEntity child = ModEntities.REPLICATOR.get().spawn(level, pos, EntitySpawnReason.BREEDING);
        if (child == null) {
            return;
        }
        child.snapTo(this.getX(), this.getY(), this.getZ(), this.random.nextFloat() * 360F, 0F);
        if (this.getTarget() != null) {
            child.setTarget(this.getTarget());
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()),
                this.getX(), this.getY() + 0.2, this.getZ(), 30, 0.3, 0.15, 0.3, 0.1);
        this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 0.8F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("ReplicatorMaterial", this.material);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.material = input.getIntOr("ReplicatorMaterial", 0);
    }

    // ---- Sounds: high-pitched metal / Sons : métal aigu -------------------------------------------

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.8F);
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public float getVoicePitch() {
        return 1.7F + (this.random.nextFloat() - 0.5F) * 0.2F;
    }

    // ---- AI ----------------------------------------------------------------------------------------

    /**
     * Walks to the nearest edible metal item on the ground when there is nothing to fight. The actual
     * eating is done by the vanilla pick-up loop ({@link #pickUpItem}).
     *
     * <p><i>FR —</i> Marche vers le plus proche objet en métal comestible au sol quand il n'y a rien à
     * combattre. Le repas lui-même passe par la boucle de ramassage vanilla ({@link #pickUpItem}).
     */
    static class SeekMetalGoal extends Goal {
        private static final double RANGE = 16.0;
        private final ReplicatorEntity mob;
        private @Nullable ItemEntity food;
        private int repath;

        SeekMetalGoal(ReplicatorEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.mob.getTarget() != null || this.mob.getRandom().nextInt(10) != 0
                    || !(this.mob.level() instanceof ServerLevel level) || !this.mob.canGrow(level)) {
                return false;
            }
            this.food = this.findFood();
            return this.food != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.food != null && this.food.isAlive() && this.mob.getTarget() == null
                    && this.mob.distanceToSqr(this.food) < RANGE * RANGE;
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public void stop() {
            this.food = null;
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.food != null && --this.repath <= 0) {
                this.repath = 10;
                this.mob.getNavigation().moveTo(this.food, 1.1);
            }
        }

        private @Nullable ItemEntity findFood() {
            ItemEntity best = null;
            double bestDist = Double.MAX_VALUE;
            for (ItemEntity item : this.mob.level().getEntitiesOfClass(ItemEntity.class,
                    this.mob.getBoundingBox().inflate(RANGE, RANGE / 2, RANGE),
                    e -> e.isAlive() && e.getItem().is(FOOD))) {
                double d = this.mob.distanceToSqr(item);
                if (d < bestDist) {
                    bestDist = d;
                    best = item;
                }
            }
            return best;
        }
    }
}
