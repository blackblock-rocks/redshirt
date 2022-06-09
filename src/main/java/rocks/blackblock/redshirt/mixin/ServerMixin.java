package rocks.blackblock.redshirt.mixin;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocks.blackblock.redshirt.Redshirt;

@Mixin(MinecraftDedicatedServer.class)
public class ServerMixin {

    @Inject(at = @At("TAIL"), method = "setupServer")
    private void getServer(CallbackInfoReturnable<Boolean> cir) {
        Redshirt.SERVER = (MinecraftDedicatedServer) (Object) this;
    }
}
