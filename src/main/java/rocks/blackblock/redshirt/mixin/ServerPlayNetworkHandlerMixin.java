package rocks.blackblock.redshirt.mixin;

import com.mojang.authlib.GameProfile;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocks.blackblock.redshirt.mixin.accessors.PlayerListS2CPacketAccessor;
import rocks.blackblock.redshirt.npc.RedshirtEntity;

import java.util.Arrays;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow public ServerPlayerEntity player;

    @Shadow public abstract void sendPacket(Packet<?> packet);

    @Unique
    private boolean redshirt$skipCheck;

    @Inject(
            method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V"
            ),
            cancellable = true
    )
    private void changeEntityType(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> listener, CallbackInfo ci) {

        ServerWorld world = player.getWorld();

        if (packet instanceof PlayerSpawnS2CPacket spawn_packet && !this.redshirt$skipCheck) {
            Entity entity = world.getEntity(spawn_packet.getPlayerUuid());

            if (!(entity instanceof RedshirtEntity npc)) {
                return;
            }

            GameProfile profile = npc.getGameProfile();
            PlayerListS2CPacket player_add_packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER);

            ((PlayerListS2CPacketAccessor) player_add_packet).setEntries(
                    Arrays.asList(new PlayerListS2CPacket.Entry(profile, 0, GameMode.SURVIVAL, npc.getName(), null))
            );
            this.sendPacket(player_add_packet);

            // Player had to be added to the tab list,
            // otherwise it doesn't show
            this.redshirt$skipCheck = true;
            this.sendPacket(spawn_packet);
            this.redshirt$skipCheck = false;

            ci.cancel();
        }



    }

}
