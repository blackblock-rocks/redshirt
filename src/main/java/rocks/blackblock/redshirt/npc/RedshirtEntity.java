package rocks.blackblock.redshirt.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import rocks.blackblock.redshirt.Redshirt;
import rocks.blackblock.redshirt.compatibility.DisguiseLibCompatibility;
import rocks.blackblock.redshirt.entity.FakePlayer;
import rocks.blackblock.redshirt.helper.SkinHelper;
import rocks.blackblock.redshirt.mixin.accessors.EntityTrackerEntryAccessor;
import rocks.blackblock.redshirt.mixin.accessors.PlayerSpawnS2CPacketAccessor;
import rocks.blackblock.redshirt.mixin.accessors.TACSAccessor;

import java.util.NoSuchElementException;

/**
 * The actual NPC
 *
 * @author  Jelle De Loecker   <jelle@elevenways.be>
 * @since   0.1.0
 * @version 0.1.0
 */
public class RedshirtEntity extends PathAwareEntity implements CrossbowUser, RangedAttackMob {

    // The main entity type
    public static EntityType<RedshirtEntity> REDSHIRT_TYPE;

    // Fake GameProfile for this NPC
    private GameProfile game_profile;

    // The server this entity is on
    private final MinecraftServer server;

    // The fake player we'll be using to represent this NPC
    private PlayerEntity fake_player = null;

    // The current skin source
    private String skin_source = null;

    // The current skin as nbt data
    private NbtCompound skin_data = null;

    /**
     * Goals
     * Public so they can be accessed from professions.
     */
    public final LookAtEntityGoal look_at_player_goal = new LookAtEntityGoal(this, PlayerEntity.class, 8.0F);
    public final LookAroundGoal look_around_goal = new LookAroundGoal(this);
    public final WanderAroundGoal wander_around_goal = new WanderAroundGoal(this, 1.0D, 30);

    // Temp
    private boolean DISGUISELIB_LOADED = true;

    public RedshirtEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);

        this.game_profile = new GameProfile(this.getUuid(), this.getName().getString());

        // Let DisguiseLib know that about this entity
        DisguiseLibCompatibility.setGameProfile(this, this.game_profile);

        // Make these persistent by default
        this.setPersistent();

        this.goalSelector.add(8, look_at_player_goal);

        this.setCustomNameVisible(true);
        this.setCustomName(this.getName());

        // Get the actual server instance
        this.server = world.getServer();

        // Create the fake player instance
        this.initFakePlayer();

        // Register this Redshirt
        Redshirt.REDSHIRTS.add(this);
    }

    /**
     * Create the fake player
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     */
    protected void initFakePlayer() {
        this.fake_player = new FakePlayer(this.world, this.getBlockPos(), this.headYaw, this.getNewGameProfile());
        //this.fake_player.getDataTracker().set(getPLAYER_MODE_CUSTOMISATION(), (byte) 0x7f);
    }

    /**
     * Get a new game profile for this player
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     */
    protected GameProfile getNewGameProfile() {
        return new GameProfile(this.uuid, null);
    }

    /**
     * Get the current game profile for this player
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     */
    public GameProfile getGameProfile() {
        return this.game_profile;
    }

    /**
     * Get the fake player
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     */
    public PlayerEntity getFakePlayer() {
        return this.fake_player;
    }

    /**
     * Set the name of this NPC
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     *
     * @param name new name to be set.
     */
    @Override
    public void setCustomName(Text name) {
        super.setCustomName(name);
        String profile_name = "Redshirt";

        if (name != null) {
            profile_name = name.getString();
            if (name.getString().length() > 16) {
                // Minecraft kicks you if player has name longer than 16 chars in GameProfile
                profile_name = name.getString().substring(0, 16);
            }
        }

        NbtCompound skin = null;

        if (this.game_profile != null) {
            skin = this.getSkinNbt(this.game_profile);
        }

        this.game_profile = new GameProfile(this.getUuid(), profile_name);

        if (skin != null) {
            this.setSkinFromNbt(skin);
            this.sendProfileUpdates();
        }
    }

    /**
     * Write the entity data to NBT
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.1.0
     */
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        if (this.skin_source != null) {
            nbt.putString("skin_source", this.skin_source);

            if (this.skin_data != null) {
                nbt.put("skin_data", this.skin_data);
            }
        }
    }

    /**
     * Read the entity from the NBT
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.1.0
     */
    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        if (nbt.contains("skin_source")) {

            if (nbt.contains("skin_data")) {
                this.setSkinFromNbt(nbt.getCompound("skin_data"));
            }

            if (this.skin_data != null) {
                this.skin_source = nbt.getString("skin_source");
            } else {
                this.setSkin(nbt.getString("skin_source"));
            }
        }
    }

    /**
     * Reload the skin
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     */
    public void reloadSkin() {
        String source = this.skin_source;
        this.skin_source = null;
        this.setSkin(source);
    }

    /**
     * Sets the skin
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     *
     * @param   skin_source   The source of the skin
     */
    public void setSkin(String skin_source) {
        this.setSkin(skin_source, false);
    }

    /**
     * Sets the skin
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     *
     * @param   skin_source   The source of the skin
     * @param   use_slim      If the slim (alex) model should be used
     */
    public void setSkin(String skin_source, Boolean use_slim) {

        if (skin_source == null) {
            return;
        }

        // Don't set the same skin again
        if (this.skin_source != null && this.skin_source.equals(skin_source)) {
            return;
        }

        this.skin_source = skin_source;

        SkinHelper.getSkin(skin_source, use_slim, result -> {

            if (result == null) {
                Redshirt.LOGGER.warn("Failed to get skin reply from " + skin_source);
                return;
            }

            String value = result.getValue();
            String signature = result.getSignature();

            if (value.isBlank() || signature.isBlank()) {
                Redshirt.LOGGER.warn("Failed to get valid skin from " + skin_source);
                return;
            }

            NbtCompound skin_nbt = new NbtCompound();
            skin_nbt.putString("value", value);
            skin_nbt.putString("signature", signature);

            this.setSkinFromNbt(skin_nbt);
            this.sendProfileUpdates();
        });
    }

    /**
     * Writes skin to NBT
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     *
     * @param profile game profile containing skin
     *
     * @return compound tag with skin values
     */
    public NbtCompound getSkinNbt(GameProfile profile) {
        NbtCompound skin_nbt = new NbtCompound();
        try {
            PropertyMap propertyMap = profile.getProperties();
            Property skin = propertyMap.get("textures").iterator().next();

            skin_nbt.putString("value", skin.getValue());
            skin_nbt.putString("signature", skin.getSignature());
        } catch (NoSuchElementException ignored) { }

        return skin_nbt;
    }

    /**
     * Sets the skin from NBT
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     *
     * @param   skin_nbt   The NBT compound with skin data
     */
    public void setSkinFromNbt(NbtCompound skin_nbt) {
        // Clearing current skin
        try {
            this.skin_data = null;
            PropertyMap map = this.game_profile.getProperties();
            Property skin = map.get("textures").iterator().next();
            map.remove("textures", skin);
        } catch (NoSuchElementException ignored) { }

        // Setting the skin
        try {
            String value = skin_nbt.getString("value");
            String signature = skin_nbt.getString("signature");

            if (value != null && signature != null && !value.isEmpty() && !signature.isEmpty()) {
                PropertyMap propertyMap = this.game_profile.getProperties();
                propertyMap.put("textures", new Property("textures", value, signature));
            }

            this.skin_data = skin_nbt;
        } catch (Error ignored) { }
    }

    /**
     * Updates NPC's {@link GameProfile} for others.
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     */
    public void sendProfileUpdates() {
        if (this.world.isClient()) return;

        if (DISGUISELIB_LOADED) {
            DisguiseLibCompatibility.setGameProfile(this, this.game_profile);
        } else {
            ServerChunkManager chunkManager = (ServerChunkManager) this.world.getChunkManager();
            ThreadedAnvilChunkStorage chunkStorage = chunkManager.threadedAnvilChunkStorage;

            EntityTrackerEntryAccessor trackerEntry = ((TACSAccessor) chunkStorage).getEntityTrackers().get(this.getId());
            if (trackerEntry != null) {
                trackerEntry.getListeners().forEach(tracking -> trackerEntry.getPlayer().startTracking(tracking.getPlayer()));
            }
        }
    }

    /**
     * Get the packet that will add this entity to the client
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     */
    @Override
    public Packet<?> createSpawnPacket() {
        PlayerSpawnS2CPacket spawn_packet = new PlayerSpawnS2CPacket(this.fake_player);
        PlayerSpawnS2CPacketAccessor spawn_accessor = (PlayerSpawnS2CPacketAccessor) spawn_packet;
        spawn_accessor.setId(this.getId());
        spawn_accessor.setUuid(this.getUuid());
        spawn_accessor.setX(this.getX());
        spawn_accessor.setY(this.getY());
        spawn_accessor.setZ(this.getZ());
        spawn_accessor.setYaw((byte)((int)(this.getYaw() * 256.0F / 360.0F)));
        spawn_accessor.setPitch((byte)((int)(this.getPitch() * 256.0F / 360.0F)));

        return spawn_packet;
    }

    /**
     * Use the datatracker of the fake player
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     */
    @Override
    public DataTracker getDataTracker() {
        if (this.fake_player == null) {
            this.initFakePlayer();
        }

        return this.fake_player.getDataTracker();
    }

    /**
     * Handles player interaction
     *
     * @param   player   The player interacting with this NPC
     * @param   pos      The position of the interaction
     * @param   hand     The hand doing the interaction
     */
    @Override
    public ActionResult interactAt(PlayerEntity player, Vec3d pos, Hand hand) {

        return ActionResult.PASS;
    }

    /**
     * Ranged Attack method
     *
     * @param   target
     * @param   pullProgress
     */
    @Override
    public void attack(LivingEntity target, float pullProgress) {

    }

    /**
     * Crossbow charging method
     *
     * @param   charging
     */
    @Override
    public void setCharging(boolean charging) {

    }

    /**
     * Crossbow shoot method method
     *
     * @param   target
     * @param   crossbow
     * @param   projectile
     * @param   multiShotSpray
     */
    @Override
    public void shoot(LivingEntity target, ItemStack crossbow, ProjectileEntity projectile, float multiShotSpray) {

    }

    /**
     * Post crossbow-shoot method
     */
    @Override
    public void postShoot() {

    }

    /**
     * Handle the death of this readshirt
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     *
     * @param   source   The source of the damage that lead to its death
     */
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        this.shouldReallyHaveBeenRemoved();
    }

    /**
     * Remove this entity
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     *
     * @param   reason   The reason why this entity was removed
     */
    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.shouldReallyHaveBeenRemoved();
    }

    /**
     * Remove this entity from the list
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     */
    public void shouldReallyHaveBeenRemoved() {
        Redshirt.REDSHIRTS.remove(this);
    }

    /**
     * Create a new redshirt NPC
     */
    public static RedshirtEntity create(ServerPlayerEntity player, String name) {

        ServerWorld world = player.getWorld();

        // Create the NPC
        RedshirtEntity npc = new RedshirtEntity(REDSHIRT_TYPE, player.getWorld());

        Vec3d pos = player.getPos();

        npc.updatePositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), player.getYaw(), player.getPitch());
        npc.setHeadYaw(player.getHeadYaw());
        npc.setCustomName(Text.literal(name));

        return npc;
    }

    /**
     * Create the default attributes of an NPC
     */
    public static DefaultAttributeContainer.Builder createDefaultAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.25D)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2505D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.8D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0D);
    }
}
