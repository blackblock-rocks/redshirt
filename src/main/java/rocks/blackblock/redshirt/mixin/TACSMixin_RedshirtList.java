package rocks.blackblock.redshirt.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.redshirt.Redshirt;
import rocks.blackblock.redshirt.npc.RedshirtEntity;

@Mixin(ServerChunkLoadingManager.class)
public class TACSMixin_RedshirtList {

    @Inject(method = "loadEntity", at = @At("TAIL"))
    protected void onLoadEntity(Entity entity, CallbackInfo ci) {
        if (entity instanceof RedshirtEntity redshirt) {
            Redshirt.REDSHIRTS.add(redshirt);
        }
    }

    @Inject(method = "unloadEntity", at = @At("TAIL"))
    protected void onUnloadEntity(Entity entity, CallbackInfo ci) {
        if (entity instanceof RedshirtEntity) {
            Redshirt.REDSHIRTS.remove(entity);
        }
    }
}
