package rocks.blackblock.redshirt.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import rocks.blackblock.core.utils.BBLog;
import rocks.blackblock.redshirt.Redshirt;
import rocks.blackblock.redshirt.helper.SkinHelper;
import rocks.blackblock.redshirt.mixin.accessors.EntityTrackerEntryAccessor;
import rocks.blackblock.redshirt.mixin.accessors.TACSAccessor;
import rocks.blackblock.redshirt.polymc.RedshirtWizard;

import java.util.NoSuchElementException;

/**
 * The actual NPC
 *
 * @author  Jelle De Loecker   <jelle@elevenways.be>
 * @since   0.1.0
 * @version 0.1.0
 */
public class RedshirtEntity extends PathAwareEntity implements CrossbowUser, RangedAttackMob {

    private static final BBLog.Categorised LOGGER = BBLog.getCategorised("redshirt");

    // The main entity type
    public static EntityType<RedshirtEntity> REDSHIRT_TYPE;

    // The server this entity is on
    private final MinecraftServer server;

    // The current skin source
    private String skin_source = null;

    // The current skin value
    private String skin_value = null;

    // The current skin signature
    private String skin_signature = null;

    // Does this entity come from NBT?
    protected boolean from_nbt = false;

    // Wizards
    private RedshirtWizard<? extends RedshirtEntity> wizard = null;

    public RedshirtEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);

        // Get the actual server instance
        this.server = world.getServer();

        // Register this Redshirt
        Redshirt.REDSHIRTS.add(this);
    }

    /**
     * Attach a wizard
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.4.0
     */
    public void setWizard(RedshirtWizard<? extends RedshirtEntity> wizard) {

        this.wizard = wizard;

        if (wizard == null || this.skin_value == null || this.skin_signature == null) {
            return;
        }

        this.wizard.setSkin(this.skin_value, this.skin_signature);
    }

    /**
     * Make sure to update the wizard once this entity gets a new name
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.1.0
     */
    @Override
    public void setCustomName(Text name) {
        super.setCustomName(name);

        if (this.wizard != null) {
            this.wizard.setName(name);
        }

        if (this.isCustomNameVisible()) {

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

            if (this.skin_value != null && this.skin_signature != null) {
                NbtCompound skin_data = new NbtCompound();
                skin_data.putString("value", this.skin_value);
                skin_data.putString("signature", this.skin_signature);
                nbt.put("skin_data", skin_data);
            }
        }
    }

    /**
     * Indicate this entity comes from NBT data
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.1.0
     */
    @Override
    public void readNbt(NbtCompound nbt) {
        this.from_nbt = true;
        super.readNbt(nbt);
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

            this.skin_source = nbt.getString("skin_source");

            if (this.skin_value == null) {
                this.setSkin(this.skin_source);
            }
        }
    }

    @Override
    public float getPathfindingFavor(BlockPos pos, WorldView world) {
        return -world.getPhototaxisFavor(pos);
    }

    /**
     * Set an attribute
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.4.0
     */
    public void setAttribute(EntityAttribute attribute_type, double new_value) {

        EntityAttributeInstance attribute = this.getAttributeInstance(attribute_type);

        if (attribute == null) {
            return;
        }

        attribute.setBaseValue(new_value);
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

        if (skin_source == null || skin_source.isBlank()) {
            if (LOGGER.isEnabled()) {
                LOGGER.log("RedshirtEntity", this, "has no skin source");
            }
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

            if (LOGGER.isEnabled()) {
                BBLog.log("Setting skin for " + this.getEntityName() + " to " + value);
            }

            NbtCompound skin_nbt = new NbtCompound();
            skin_nbt.putString("value", value);
            skin_nbt.putString("signature", signature);

            this.setSkinFromNbt(skin_nbt);
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

        this.skin_value = null;
        this.skin_signature = null;

        // Setting the skin
        try {
            String value = skin_nbt.getString("value");
            String signature = skin_nbt.getString("signature");

            if (value != null && !value.isBlank()) {
                this.skin_value = value;
            }

            if (signature != null && !signature.isBlank()) {
                this.skin_signature = signature;
            }

            if (this.wizard != null && this.skin_value != null && this.skin_signature != null) {
                this.wizard.setSkin(this.skin_value, this.skin_signature);
            }

        } catch (Error ignored) {
            BBLog.error("Error setting skin from NBT:", ignored);
        }

        this.sendProfileUpdates();
    }

    /**
     * Updates NPC's {@link GameProfile} for others.
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     */
    public void sendProfileUpdates() {

        if (this.getWorld().isClient()) {
            return;
        }

        if (this.wizard != null) {
            this.wizard.markDirty();
        }

        ServerChunkManager chunkManager = (ServerChunkManager) this.getWorld().getChunkManager();
        ThreadedAnvilChunkStorage chunkStorage = chunkManager.threadedAnvilChunkStorage;

        EntityTrackerEntryAccessor trackerEntry = ((TACSAccessor) chunkStorage).getEntityTrackers().get(this.getId());
        if (trackerEntry != null) {
            trackerEntry.getListeners().forEach(tracking -> trackerEntry.getPlayer().startTracking(tracking.getPlayer()));
        }
    }

    /**
     * Use the datatracker of the fake player
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.1.0
     */
    /*@Override
    public DataTracker getDataTracker() {

        if (this.fake_player == null) {
            this.initFakePlayer();
        }

        return this.fake_player.getDataTracker();
    }*/

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
        BBLog.log("Called ranged attack method for", this, "but nothing was implemented");
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

        if (this.wizard != null) {
            this.wizard.scheduleRemovePacket();
        }

        Redshirt.REDSHIRTS.remove(this);
    }

    /**
     * Queue a datatracker update
     *
     * @since   0.5.0
     */
    @Override
    public void setPose(EntityPose pose) {
        super.setPose(pose);

        if (this.wizard != null) {
            this.wizard.scheduleDataTrackerUpdate();
        }
    }

    /**
     * Create a new redshirt NPC
     */
    public static RedshirtEntity create(ServerPlayerEntity player, String name) {

        ServerWorld world = player.getServerWorld();

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
