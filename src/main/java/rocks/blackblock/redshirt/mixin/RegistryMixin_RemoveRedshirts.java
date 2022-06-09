package rocks.blackblock.redshirt.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import rocks.blackblock.redshirt.Redshirt;

import java.util.Map;

@Mixin(RegistrySyncManager.class)
public class RegistryMixin_RemoveRedshirts {

    private static final Identifier ENTITY_TYPE = new Identifier("minecraft", "entity_type");

    /**
     * Removes all redshirt tags from registry sync, as we do not need it on client.
     * Prevents client from being kicked if using FAPI.
     * Prevents PolyMC from providing a poly too
     */
    @Inject(
            method = "createAndPopulateRegistryMap",
            at = @At("TAIL"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            remap = false
    )
    private static void removeRedshirtsFromSync(boolean isClientSync, @Nullable Map<Identifier, Object2IntMap<Identifier>> activeMap, CallbackInfoReturnable<@Nullable Map<Identifier, Object2IntMap<Identifier>>> cir, Map<Identifier, Object2IntMap<Identifier>> map) {
        if (isClientSync) {
            var entity_map = map.get(ENTITY_TYPE);

            for (Identifier id : Redshirt.REDSHIRT_TYPE_IDENTIFIERS) {
                entity_map.removeInt(id);
            }
        }
    }

}
