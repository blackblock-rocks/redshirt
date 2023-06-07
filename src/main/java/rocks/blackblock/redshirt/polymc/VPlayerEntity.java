package rocks.blackblock.redshirt.polymc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import io.github.theepicblock.polymc.api.wizard.PacketConsumer;
import io.github.theepicblock.polymc.impl.poly.wizard.AbstractVirtualEntity;
import io.github.theepicblock.polymc.impl.poly.wizard.EntityUtil;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import rocks.blackblock.core.BlackBlockCore;
import rocks.blackblock.core.utils.BBLog;
import rocks.blackblock.redshirt.entity.FakePlayer;
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
public class VPlayerEntity extends AbstractVirtualEntity {

    public static final String EMPTY_PLAYER_NAME = "\u0020";
    public static final Text EMPTY_PLAYER_NAME_TEXT = Text.of(EMPTY_PLAYER_NAME);
    public static TeamS2CPacket team_packet = null;

    @NotNull
    protected Text name = EMPTY_PLAYER_NAME_TEXT;

    protected GameProfile profile = null;
    protected String skin_value = null;
    protected String skin_signature = null;
    protected boolean is_dirty = false;
    protected boolean needs_remove = false;
    protected LivingEntity entity = null;
    protected boolean has_spawned_once = false;

    /**
     * Construct a new virtual player with automatically generated ids
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public VPlayerEntity() {
        this(MathHelper.randomUuid());
    }

    /**
     * Construct a new virtual player with a specific uuid
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public VPlayerEntity(UUID uuid) {
        this(uuid, EntityUtil.getNewEntityId());
    }

    /**
     * Construct a new virtual player with given ids
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public VPlayerEntity(UUID uuid, int id) {
        super(uuid, id);
        this.createNewGameProfile();
    }

    /**
     * Construct a new virtual player with given entity
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public VPlayerEntity(LivingEntity entity) {
        super(entity.getUuid(), entity.getId());
        this.createNewGameProfile();
        this.setEntity(entity);

        if (entity.hasCustomName()) {
            this.setName(entity.getName());
        }
    }

    /**
     * Get the fake player instance
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public FakePlayer getFakePlayer() {
        FakePlayer player = new FakePlayer(BlackBlockCore.getServer().getOverworld(), this.profile);
        return player;
    }

    /**
     * Get the packet that will add a team that will make VPlayerEntities without a name
     * have no black bar above their head
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
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
     * Attach the real entity
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public void setEntity(LivingEntity entity) {
        this.entity = entity;
    }

    /**
     * Mark this entity as being dirt
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public void setDirty(boolean is_dirty) {
        this.is_dirty = is_dirty;
    }

    /**
     * Is this entity dirty?
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public boolean isDirty() {
        return this.is_dirty;
    }

    /**
     * Get a new game profile for this player
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
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

        this.updateProfileSkin();

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

        if (content instanceof LiteralTextContent literal) {
            string_content = literal.string();

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

        BBLog.attention("Setting skin of VPlayerEntity " + this.getNameString());

        this.skin_value = value;
        this.skin_signature = signature;

        this.updateProfileSkin();
    }

    /**
     * Update the skin of the game profile
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    private void updateProfileSkin() {
        PropertyMap profile_properties = this.profile.getProperties();

        String value = this.skin_value;
        String signature = this.skin_signature;

        if (value != null & signature != null && !value.isEmpty() && !signature.isEmpty()) {
            profile_properties.put("textures", new Property("textures", value, signature));
        } else {
            profile_properties.removeAll("textures");
        }

        if (this.has_spawned_once) {
            this.setDirty(true);
        }
    }

    /**
     * Return the entity type
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    @Override
    public EntityType<?> getEntityType() {
        return EntityType.PLAYER;
    }

    /**
     * Spawn this virtual player in the given player's client
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    @Override
    public void spawn(PacketConsumer player, Vec3d pos) {
        this.spawn(player, pos, 0, 0, 0, Vec3d.ZERO);
    }

    /**
     * Spawn this virtual player in the given player's client
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    @Override
    public void spawn(PacketConsumer players, Vec3d pos, float pitch, float yaw, int entityData, Vec3d velocity) {

        TeamS2CPacket packet = this.getEmptyTeamNamePacket();
        players.sendPacket(packet);

        this.has_spawned_once = true;
        this.sendTablistAddPacket(players);

        players.sendPacket(this.createSpawnPacket(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch));

        // Make sure all the skin layers are rendered
        this.sendTrackedDataUpdate(players, PlayerEntityAccessor.getPLAYER_MODEL_PARTS(), (byte) 0x7f);

        this.sendEquipmentPacket(players);
    }

    /**
     * Send a TrackedData update
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public <T> void sendTrackedDataUpdate(PacketConsumer players, TrackedData<T> data, T value) {
        EntityTrackerUpdateS2CPacket packet = EntityUtil.createDataTrackerUpdate(this.getId(), data, value);
        players.sendPacket(packet);
    }

    /**
     * Remove this entity for the given players
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    @Override
    public void remove(PacketConsumer players) {
        super.remove(players);
        this.sendTablistRemovePacket(players);
    }

    /**
     * Create a spawn packet for this player
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.4.0
     */
    protected Packet<?> createSpawnPacket(double x, double y, double z, float yaw, float pitch) {

        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(this.id);

        // Use random uuid? MathHelper.randomUuid()
        buffer.writeUuid(this.uuid);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeByte((byte)((int)(yaw * 256.0F / 360.0F)));
        buffer.writeByte((byte)((int)(pitch * 256.0F / 360.0F)));

        buffer.resetReaderIndex();

        return new PlayerSpawnS2CPacket(buffer);
    }

    /**
     * Create a packet with equipment information
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
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
     * @author  Jelle De Loecker   <jelle@elevenways.be>
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
     * Send a tablist remove packet
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.4.0
     */
    protected void sendTablistRemovePacket(PacketConsumer players) {
        PlayerRemoveS2CPacket player_remove_packet = new PlayerRemoveS2CPacket(List.of(this.uuid));
        players.sendPacket(player_remove_packet);
    }

    /**
     * Send a tablist add packet
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.4.0
     */
    protected void sendTablistAddPacket(PacketConsumer players) {

        FakePlayer player = this.getFakePlayer();

        // Send the player info (adds it to the tablist. Should no longer be needed in 1.19.3)
        PlayerListS2CPacket player_add_packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player);
        ((PlayerListS2CPacketAccessor) player_add_packet).setEntries(
                List.of(new PlayerListS2CPacket.Entry(this.uuid, this.profile, false, 0, GameMode.SURVIVAL, this.name, null))
        );
        players.sendPacket(player_add_packet);

        // Send the player info (adds it to the tablist. Should no longer be needed in 1.19.3)
        PlayerListS2CPacket player_update_packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, player);
        ((PlayerListS2CPacketAccessor) player_update_packet).setEntries(
                List.of(new PlayerListS2CPacket.Entry(this.uuid, this.profile, false, 0, GameMode.SURVIVAL, this.name, null))
        );
        players.sendPacket(player_update_packet);
    }

    /**
     * Update this entity's skin
     *
     * @author  Jelle De Loecker   <jelle@elevenways.be>
     * @since   0.4.0
     */
    protected void sendProfileUpdate(PacketConsumer players) {

        boolean remove = false;

        if (this.entity != null && !this.entity.isAlive()) {
            remove = true;
        }

        // Actually remove this entity for all the real players
        this.remove(players);

        this.sendTablistAddPacket(players);

        if (!remove) {
            BlackBlockCore.onTickTimeout(() -> {
                this.spawn(players, this.entity.getPos());
            }, 4);
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
        return "VPlayerEntity{'" + this.name + "',uuid=" + this.uuid + ",id=" + this.id + "}";
    }
}
