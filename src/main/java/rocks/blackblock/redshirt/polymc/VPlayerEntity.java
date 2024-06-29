package rocks.blackblock.redshirt.polymc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import io.github.theepicblock.polymc.api.wizard.PacketConsumer;
import io.github.theepicblock.polymc.api.wizard.UpdateInfo;
import io.github.theepicblock.polymc.impl.poly.wizard.AbstractVirtualEntity;
import io.github.theepicblock.polymc.impl.poly.wizard.EntityUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.item.ItemStack;
import net.minecraft.network.encryption.PublicPlayerSession;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import rocks.blackblock.bib.util.BibLog;
import rocks.blackblock.bib.util.BibServer;
import rocks.blackblock.redshirt.entity.FakeRedshirtPlayer;
import rocks.blackblock.redshirt.mixin.accessors.PlayerEntityAccessor;
import rocks.blackblock.redshirt.mixin.accessors.PlayerListS2CPacketAccessor;
import rocks.blackblock.screenbuilder.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A virtual player entity for PolyMc wizards
 *
 * @author   Jelle De Loecker   <jelle@elevenways.be>
 * @since    0.4.0
 */
public class VPlayerEntity extends AbstractVirtualEntity implements BibLog.Argable {

    // The BibLog logger (that is optionally enabled)
    private static final BibLog.Categorised LOGGER = BibLog.getCategorised("redshirt", "skinhelper");

    // The "empty" name for the entity
    public static final String EMPTY_PLAYER_NAME = "\u0020";

    // The text representation of the empty name
    public static final Text EMPTY_PLAYER_NAME_TEXT = Text.of(EMPTY_PLAYER_NAME);

    // The team packet to send to the client
    public static TeamS2CPacket team_packet = null;

    // The initial name of the virtual player is an "empty" looking string
    @NotNull
    protected Text name = EMPTY_PLAYER_NAME_TEXT;

    // The game profile
    protected GameProfile profile = null;

    // The skin value & signature
    protected String skin_value = null;
    protected String skin_signature = null;

    // Is this player entity dirty?
    protected boolean is_dirty = false;

    // Does this player entity need to be removed?
    protected boolean needs_remove = false;

    // The real entity
    protected LivingEntity entity = null;

    // Has this player entity been spawned once?
    protected boolean has_spawned_once = false;

    // Has this player info been sent to the client yet?
    protected boolean add_packet_has_been_sent = false;

    // Actions that have to be sent to everyone
    protected List<Packet> queued_packets = new ArrayList<>(32);

    // Does this still require a global spawn packet?
    protected int requires_global_spawn_packet = 0;

    // Does this need a datatracker update?
    protected boolean needs_datatracker_update = false;

    // Schedule a removal
    protected boolean needs_temporary_removal = false;

    // Register count
    protected int register_count = 0;

    // Updates without skin
    protected int updates_without_skin = 0;

    /**
     * Construct a new virtual player with given ids
     *
     * @since    0.4.0
     */
    public VPlayerEntity(UUID uuid, int id) {
        super(uuid, id);
        this.createNewGameProfile();
    }

    /**
     * Construct a new virtual player with given entity
     *
     * @since    0.4.0
     */
    public VPlayerEntity(LivingEntity entity) {
        this(entity.getUuid(), entity.getId());
        this.setEntity(entity);

        if (entity.hasCustomName()) {
            this.setName(entity.getName());
        }
    }

    /**
     * Get the packet that will add a team that will make VPlayerEntities without a name
     * have no black bar above their head
     *
     * @since    0.4.0
     */
    public TeamS2CPacket getEmptyTeamNamePacket() {

        if (team_packet == null) {

            String negative_movement = Font.SPACE.getMovementString(-6);
            MutableText negative_text = Font.SPACE.getText(negative_movement);

            Scoreboard scoreboard = new Scoreboard();
            Team team = new Team(scoreboard, "-");
            team.getPlayerList().add(EMPTY_PLAYER_NAME);
            team.setSuffix(negative_text);
            team_packet = TeamS2CPacket.updateTeam(team, true);
        }

        return team_packet;
    }

    /**
     * Should we send a gloal spawn packet?
     *
     * @since    0.5.0
     */
    public boolean requiresGlobalSpawnPacket() {
        return this.requires_global_spawn_packet > 0;
    }

    /**
     * Increase spawn packet requirement
     *
     * @since    0.5.0
     */
    public void increaseGlobalSpawnPacketRequirement(int amount) {
        this.requires_global_spawn_packet += amount;
    }

    /**
     * Consume a global spawn packet requirement
     *
     * @since    0.5.0
     */
    public boolean consumeGlobalSpawnPacketRequirement() {

        if (this.requires_global_spawn_packet > 0) {
            this.requires_global_spawn_packet--;
            return true;
        }

        return false;
    }

    /**
     * Attach the real entity
     *
     * @since    0.4.0
     */
    public void setEntity(LivingEntity entity) {
        this.entity = entity;
    }

    /**
     * Mark this entity as being dirty
     *
     * @since    0.4.0
     */
    public void setDirty(boolean is_dirty) {
        this.is_dirty = is_dirty;
    }

    /**
     * Make this entity dirty
     *
     * @since    0.4.0
     */
    public void makeDirty() {
        this.setDirty(true);
    }


    /**
     * Is this entity dirty?
     *
     * @since    0.4.0
     */
    public boolean isDirty() {
        return this.is_dirty;
    }

    /**
     * Create a new fake player instance with the current profile info
     *
     * @since    0.4.0
     */
    public FakeRedshirtPlayer getNewFakePlayer() {
        return new FakeRedshirtPlayer(BibServer.getServer().getOverworld(), this.profile);
    }

    /**
     * Create a new game profile for this player
     *
     * @since   0.4.0
     */
    protected GameProfile createNewGameProfile() {

        String name;

        if (this.name == EMPTY_PLAYER_NAME_TEXT) {
            name = "";
        } else {
            name = this.name.getString();
        }

        if (name.length() > 16) {
            name = name.substring(0, 15);
        }

        if (name.isEmpty() || name.isBlank()) {
            name = EMPTY_PLAYER_NAME;
        }

        this.profile = new GameProfile(this.uuid, name);

        this.updateSkinInGameProfile();

        return this.profile;
    }

    /**
     * Safely set this name
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.1
     */
    protected void setSafeName(Text name) {

        if (name == null) {
            name = EMPTY_PLAYER_NAME_TEXT;
        }

        TextContent content = name.getContent();
        String string_content;

        if (content instanceof PlainTextContent plain) {
            string_content = plain.string();

            if (string_content == null) {
                content = null;
            }
        }

        if (content == null) {
            name = EMPTY_PLAYER_NAME_TEXT;
        }

        this.name = name;
    }

    /**
     * Set the name of this entity.
     * Player entities always require a name.
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public void setName(Text name) {
        this.setSafeName(name);
        this.createNewGameProfile();
        this.makeDirty();
    }

    /**
     * Get the name of this entity
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public String getNameString() {

        if (this.name != EMPTY_PLAYER_NAME_TEXT) {
            return this.name.getString();
        }

        return "unknown(" + this + ")";
    }

    /**
     * Set the skin to use
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public void setSkin(String value, String signature) {

        if (LOGGER.isEnabled()) {
            BibLog.attention("Setting skin of VPlayerEntity " + this.getNameString());

            if (value == null) {
                BibLog.log(" -- Skin value is null!!!");
            } else {
                BibLog.log(" -- Value is " + value.length() + " chars long");
            }
        }

        this.skin_value = value;
        this.skin_signature = signature;

        this.updateSkinInGameProfile();
    }

    /**
     * Update the skin of the game profile
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    private void updateSkinInGameProfile() {

        if (LOGGER.isEnabled()) {
            BibLog.log(" -- Updating skin of", this);
        }

        PropertyMap profile_properties = this.profile.getProperties();

        String value = this.skin_value;
        String signature = this.skin_signature;

        if (value != null & signature != null && !value.isEmpty() && !signature.isEmpty()) {
            profile_properties.put("textures", new Property("textures", value, signature));
        } else {
            profile_properties.removeAll("textures");
        }

        if (LOGGER.isEnabled()) {
            LOGGER.log(" -- Updated profile is now", profile_properties);
            LOGGER.log("  -- Has spawned once?", this.has_spawned_once);
            LOGGER.log("  -- Has been sent before?", this.add_packet_has_been_sent);
        }

        if (this.register_count > 0) {
            this.setDirty(true);
        }
    }

    /**
     * Return the entity type
     *
     * @since    0.4.0
     */
    @Override
    public EntityType<?> getEntityType() {
        return EntityType.PLAYER;
    }

    /**
     * Spawn this virtual player in the given player's client
     *
     * @deprecated
     * @since    0.4.0
     */
    @Override
    public void spawn(PacketConsumer player, Vec3d pos) {
        this.sendToConsumers(player, pos);
    }

    /**
     * Spawn this virtual player in the given player's client
     *
     * @deprecated
     * @since    0.4.0
     */
    @Override
    public void spawn(PacketConsumer players, Vec3d pos, float pitch, float yaw, int entityData, Vec3d velocity) {
        this.sendToConsumers(players, pos, pitch, yaw, entityData, velocity);
    }

    /**
     * Send this virtual player to the given consumers
     *
     * @since    0.4.1
     */
    public void sendToConsumers(PacketConsumer player, Vec3d pos) {
        this.sendToConsumers(player, pos, 0, 0, 0, Vec3d.ZERO);
    }

    /**
     * Send this entity to the given consumers for the first time
     *
     * @since    0.4.1
     */
    public void addConsumers(PacketConsumer consumers, Vec3d pos) {
        this.sendToConsumers(consumers, pos, 0, 0, 0, Vec3d.ZERO);
    }

    /**
     * Spawn this virtual player in the given player's client
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public void sendToConsumers(PacketConsumer players, Vec3d pos, float pitch, float yaw, int entityData, Vec3d velocity) {

        TeamS2CPacket packet = this.getEmptyTeamNamePacket();
        players.sendPacket(packet);

        if (!this.registerWithClient(players)) {
            if (LOGGER.isEnabled()) {
                LOGGER.log(" -- Failed to register with client, not sending to consumers");
            }
            return;
        }

        // Send the actual spawn packet
        players.sendPacket(this.createSpawnPacket(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch));

        // Make sure all the skin layers are rendered
        this.sendTrackedDataUpdate(players, PlayerEntityAccessor.getPLAYER_MODEL_PARTS(), (byte) 0x7f);

        // Send the equipment packets too
        this.sendEquipmentPacket(players);

        this.sendDataTrackerUpdate(players);
    }

    /**
     * Send a TrackedData update
     *
     * @since    0.4.0
     */
    public <T> void sendTrackedDataUpdate(PacketConsumer players, TrackedData<T> data, T value) {
        EntityTrackerUpdateS2CPacket packet = EntityUtil.createDataTrackerUpdate(this.getId(), data, value);
        players.sendPacket(packet);
    }

    /**
     * Remove this entity for the given players
     *
     * @since    0.4.0
     */
    @Override
    public void remove(PacketConsumer players) {
        super.remove(players);
        this.sendClientRemovePacket(players);
    }

    /**
     * Create a spawn packet for this player
     *
     * @since   0.4.0
     */
    protected Packet<?> createSpawnPacket(double x, double y, double z, float yaw, float pitch) {

        var result = new EntitySpawnS2CPacket(
                this.id,
                this.uuid,
                x,
                y,
                z,
                pitch,
                yaw,
                EntityType.PLAYER,
                0,
                Vec3d.ZERO,
                this.entity.getHeadYaw()
        );

        return result;
    }

    /**
     * Create a packet with equipment information
     *
     * @since   0.4.0
     */
    protected EntityEquipmentUpdateS2CPacket createEquipmentPacket() {

        ArrayList<Pair<EquipmentSlot, ItemStack>> list = new ArrayList<>();

        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            ItemStack itemStack = this.entity.getEquippedStack(equipmentSlot);
            if (itemStack.isEmpty()) continue;
            list.add(Pair.of(equipmentSlot, itemStack.copy()));
        }

        if (list.isEmpty()) {
            return null;
        }

        return new EntityEquipmentUpdateS2CPacket(this.id, list);
    }

    /**
     * Send an equipment packet
     *
     * @since   0.4.0
     */
    protected void sendEquipmentPacket(PacketConsumer players) {

        EntityEquipmentUpdateS2CPacket packet = this.createEquipmentPacket();

        if (packet == null) {
            return;
        }

        players.sendPacket(packet);
    }

    /**
     * Schedule a temporary removal of this entity
     *
     * @since   0.5.0
     */
    protected void scheduleTemporaryRemoval(PacketConsumer players) {
        this.needs_temporary_removal = true;
        this.sendClientRemovePacket(players);
    }

    /**
     * Send a tablist remove packet
     *
     * @since   0.4.0
     */
    protected void sendClientRemovePacket(PacketConsumer players) {
       this.needs_temporary_removal = false;
       this.sendActualClientRemovePackets(players);
       this.increaseGlobalSpawnPacketRequirement(2);
    }

    /**
     * Send an actual removal packet
     *
     * @since   0.4.0
     */
    protected void sendActualClientRemovePackets(PacketConsumer players) {
        if (LOGGER.isEnabled()) {
            LOGGER.log("  - Removing VPlayerEntity " + this.uuid + " - " + this.id);
        }

        players.sendDeathPacket(this.id);

        // Remove the player from the client's player list
        PlayerRemoveS2CPacket player_remove_packet = new PlayerRemoveS2CPacket(List.of(this.uuid));
        players.sendPacket(player_remove_packet);
    }

    /**
     * Send a client add packet once the server is ready
     *
     * @since   0.4.0
     */
    protected boolean registerWithClient(PacketConsumer players) {

        if (!BibServer.isReady()) {
            this.increaseGlobalSpawnPacketRequirement(1);
            return false;
        }

        if (this.skin_value == null && this.updates_without_skin < 10) {
            this.increaseGlobalSpawnPacketRequirement(1);
            return false;
        }

        PlayerListS2CPacket packet = this.createClientRegisterPacket();
        players.sendPacket(packet);

        return true;
    };

    /**
     * Send a tablist add packet
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.4.0
     */
    protected PlayerListS2CPacket createClientRegisterPacket() {

        this.add_packet_has_been_sent = true;

        FakeRedshirtPlayer fake_player = this.getNewFakePlayer();

        // These players should not be listed (in the tab)
        boolean listed = false;

        // The ping can be zero
        int ping = 0;

        this.register_count++;

        // There will be no chat session
        PublicPlayerSession.Serialized chatSession = null;

        if (LOGGER.isEnabled() && !this.profile.getProperties().containsKey("textures")) {
            BibLog.attention("No textures found for " + this.uuid);
        }

        // Send the player info
        PlayerListS2CPacket player_add_packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, fake_player);
        ((PlayerListS2CPacketAccessor) player_add_packet).setEntries(
                List.of(new PlayerListS2CPacket.Entry(this.uuid, this.profile, listed, ping, GameMode.SURVIVAL, this.name, chatSession))
        );

        return player_add_packet;
    }

    /**
     * Update this entity's skin
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.4.0
     */
    protected void sendProfileUpdateWhenDirty(PacketConsumer players) {

        if (this.requiresGlobalSpawnPacket()) {
            return;
        }

        if (LOGGER.isEnabled()) {
            LOGGER.log("Sending profile updates of", this, "to", players);
        }

        // Actually remove this entity for all the real players
        // We'll send it again on the next update
        this.scheduleTemporaryRemoval(players);

        this.setDirty(false);
    }

    /**
     * Schedule a datatracker update
     *
     * @since    0.5.0
     */
    public void scheduleDataTrackerUpdate() {
        this.needs_datatracker_update = true;
    }

    /**
     * Send a datatracker update
     *
     * @since    0.5.0
     */
    protected void sendDataTrackerUpdate(PacketConsumer players) {

        if (this.entity == null) {
            return;
        }

        if (this.requires_global_spawn_packet > 1) {
            return;
        }

        if (this.needs_datatracker_update) {
            this.needs_datatracker_update = false;
        } else {
            return;
        }

        if (LOGGER.isEnabled()) {
            LOGGER.log("Sending datatracker updates of", this, "to", players);
        }
    }

    /**
     * Handle wizard updates
     *
     * @since   0.4.0
     */
    public void update(PacketConsumer players, UpdateInfo info) {

        if (this.skin_value == null) {
            this.updates_without_skin++;

            if (this.updates_without_skin < 10) {
                return;
            }
        }

        if (this.needs_temporary_removal) {
            this.sendActualClientRemovePackets(players);
            return;
        }

        if (this.consumeGlobalSpawnPacketRequirement()) {
            this.addConsumers(players, this.entity.getPos());
        }

        if (this.needs_datatracker_update) {
            this.sendDataTrackerUpdate(players);
        }

        if (this.isDirty()) {
            this.sendProfileUpdateWhenDirty(players);
        }
    }

    /**
     * Return a string representation of this virtual player
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.4.1
     */
    @Override
    public String toString() {
        return this.toBBLogArg().toString();
    }

    /**
     * Get an Arg representation of this instance
     *
     * @since    1.5.1
     */
    @Override
    public BibLog.Arg toBBLogArg() {
        return BibLog.createArg(this)
            .add("name", this.name)
            .add("uuid", this.uuid)
            .add("id", this.id)
            .add("entity", this.entity)
            .add("profile", this.profile);
    }
}
