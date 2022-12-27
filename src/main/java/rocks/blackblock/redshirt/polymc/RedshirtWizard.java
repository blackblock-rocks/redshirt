package rocks.blackblock.redshirt.polymc;

import io.github.theepicblock.polymc.api.wizard.PacketConsumer;
import io.github.theepicblock.polymc.api.wizard.UpdateInfo;
import io.github.theepicblock.polymc.api.wizard.WizardInfo;
import io.github.theepicblock.polymc.impl.poly.entity.EntityWizard;
import net.minecraft.text.Text;
import rocks.blackblock.core.BlackBlockCore;
import rocks.blackblock.core.utils.BBLog;
import rocks.blackblock.redshirt.npc.RedshirtEntity;

public class RedshirtWizard<T extends RedshirtEntity> extends EntityWizard<T> {

    private final VPlayerEntity virtual_player;

    public RedshirtWizard(WizardInfo info, T entity) {
        super(info, entity);
        this.virtual_player = new VPlayerEntity(entity);
        entity.setWizard(this);
    }

    @Override
    public void addPlayer(PacketConsumer packetConsumer) {
        this.virtual_player.spawn(packetConsumer, this.getEntity().getPos());
    }

    @Override
    public void removePlayer(PacketConsumer packetConsumer) {
        this.virtual_player.remove(packetConsumer);
    }

    @Override
    public void onMove(PacketConsumer players) {
        var entity = this.getEntity();
        this.virtual_player.move(players, this.getPosition(), entity.getYaw(), entity.getPitch(), entity.isOnGround());
        this.virtual_player.sendVelocity(players, entity.getVelocity());
    }

    /**
     * Set the skin to use
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public void setSkin(String value, String signature) {
        this.virtual_player.setSkin(value, signature);
    }

    /**
     * Set the new name of the virtual player
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public void setName(Text name) {
        this.virtual_player.setName(name);
        this.markDirty();
    }

    /**
     * Request updates
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public void markDirty() {
        this.virtual_player.setDirty(true);
    }

    /**
     * Schedule a remove packet
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    public void scheduleRemovePacket() {
        BlackBlockCore.onTickTimeout(() -> {
            this.virtual_player.setDirty(true);
        }, 20);
    }

    /**
     * Check for updates
     *
     * @author   Jelle De Loecker   <jelle@elevenways.be>
     * @since    0.4.0
     */
    @Override
    public void update(PacketConsumer players, UpdateInfo info) {
        if (this.virtual_player.isDirty()) {
            this.virtual_player.sendProfileUpdate(players);
            this.virtual_player.setDirty(false);
        }
    }
}
