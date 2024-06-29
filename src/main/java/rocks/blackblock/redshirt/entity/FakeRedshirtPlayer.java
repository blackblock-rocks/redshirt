package rocks.blackblock.redshirt.entity;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

public class FakeRedshirtPlayer extends FakePlayer {

    public FakeRedshirtPlayer(MinecraftServer server, ServerWorld world, GameProfile profile) {
        this(world, profile);
    }

    public FakeRedshirtPlayer(ServerWorld world, GameProfile profile) {
        super(world, profile);
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
