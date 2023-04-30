package rocks.blackblock.redshirt.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rocks.blackblock.core.BlackBlockCore;

public class FakePlayer extends ServerPlayerEntity {

    public FakePlayer(MinecraftServer server, ServerWorld world, GameProfile profile) {
        super(server, world, profile);
    }

    public FakePlayer(ServerWorld world, GameProfile profile) {
        this(BlackBlockCore.getServer(), world, profile);
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }
}
