package rocks.blackblock.redshirt.polymc;

import io.github.theepicblock.polymc.api.wizard.PacketConsumer;
import io.github.theepicblock.polymc.api.wizard.UpdateInfo;
import io.github.theepicblock.polymc.api.wizard.WizardInfo;
import io.github.theepicblock.polymc.impl.poly.entity.EntityWizard;
import net.minecraft.text.Text;
import rocks.blackblock.core.BlackBlockCore;
import rocks.blackblock.redshirt.npc.RedshirtEntity;

/**
 * The wizard for handling Redshirt entities
 *
 * @author   Jelle De Loecker   <jelle@elevenways.be>
 * @since    0.1.0
 */
public class RedshirtWizard<T extends RedshirtEntity> extends EntityWizard<T> {

    // The actual virtual player entity
    private final VPlayerEntity virtual_player;

    /**
     * Initialize the wizard
     *
     * @since    0.1.0
     */
    public RedshirtWizard(WizardInfo info, T entity) {
        super(info, entity);

        // Create our virtual player
        this.virtual_player = new VPlayerEntity(entity);

        // And inform the entity this is now the wizard
        entity.setWizard(this);
    }

    /**
     * Schedule a datatracker update
     *
     * @since    0.5.0
     */
    public void scheduleDataTrackerUpdate() {
        this.virtual_player.scheduleDataTrackerUpdate();
    }

    /**
     * Add the given player to this wizard:
     * send them the entity spawn packet
     *
     * @since    0.1.0
     */
    @Override
    public void addPlayer(PacketConsumer packetConsumer) {
        this.virtual_player.addConsumers(packetConsumer, this.getEntity().getPos());
    }

    /**
     * Remove this player from the given consumers
     *
     * @since    0.1.0
     */
    @Override
    public void removePlayer(PacketConsumer packetConsumer) {
        this.virtual_player.remove(packetConsumer);
    }

    /**
     * Send a teleport packet to the given consumers
     *
     * @since    0.1.0
     */
    @Override
    public void onMove(PacketConsumer players) {
        var entity = this.getEntity();
        this.virtual_player.move(players, this.getPosition(), entity.getYaw(), entity.getPitch(), entity.isOnGround());
        this.virtual_player.sendVelocity(players, entity.getVelocity());
    }

    /**
     * Set the skin to use
     *
     * @since    0.4.0
     */
    public void setSkin(String value, String signature) {
        this.virtual_player.setSkin(value, signature);
    }

    /**
     * Set the new name of the virtual player
     *
     * @since    0.4.0
     */
    public void setName(Text name) {
        this.virtual_player.setName(name);
    }

    /**
     * Request updates
     *
     * @since    0.4.0
     */
    public void markDirty() {
        this.virtual_player.makeDirty();
    }

    /**
     * Schedule a remove packet
     *
     * @since    0.4.0
     */
    public void scheduleRemovePacket() {
        BlackBlockCore.onTickTimeout(this.virtual_player::makeDirty, 20);
    }

    /**
     * Check for updates
     *
     * @since    0.4.0
     */
    @Override
    public void update(PacketConsumer players, UpdateInfo info) {
        this.virtual_player.update(players, info);
    }
}
