package rocks.blackblock.redshirt.mixin.accessors;

import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.PlayerAssociatedNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(targets="net.minecraft.server.world.ServerChunkLoadingManager$EntityTracker")
public interface EntityTrackerEntryAccessor {
    @Accessor("entry")
    EntityTrackerEntry getPlayer();
    @Accessor("listeners")
    Set<PlayerAssociatedNetworkHandler> getListeners();
}
