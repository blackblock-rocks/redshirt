package rocks.blackblock.redshirt.mixin.accessors;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {

    @Accessor("STANDING_DIMENSIONS")
    static EntityDimensions getSTANDING_DIMENSIONS() {
        throw new AssertionError();
    }

    @Accessor("POSE_DIMENSIONS")
    static Map<EntityPose, EntityDimensions> getPOSE_DIMENSIONS() {
        throw new AssertionError();
    }

    @Accessor("ABSORPTION_AMOUNT")
    static TrackedData<Float> getABSORPTION_AMOUNT() {
        throw new AssertionError();
    }

    @Accessor("SCORE")
    static TrackedData<Integer> getSCORE() {
        throw new AssertionError();
    }

    @Accessor("PLAYER_MODEL_PARTS")
    static TrackedData<Byte> getPLAYER_MODEL_PARTS() {
        throw new AssertionError();
    }

    @Accessor("MAIN_ARM")
    static TrackedData<Byte> getMAIN_ARM() {
        throw new AssertionError();
    }

    @Accessor("LEFT_SHOULDER_ENTITY")
    static TrackedData<NbtCompound> getLEFT_SHOULDER_ENTITY() {
        throw new AssertionError();
    }

    @Accessor("RIGHT_SHOULDER_ENTITY")
    static TrackedData<NbtCompound> getRIGHT_SHOULDER_ENTITY() {
        throw new AssertionError();
    }
}
