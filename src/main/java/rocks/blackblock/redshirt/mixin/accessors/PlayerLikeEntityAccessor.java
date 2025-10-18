package rocks.blackblock.redshirt.mixin.accessors;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(PlayerLikeEntity.class)
public interface PlayerLikeEntityAccessor {

    @Accessor("STANDING_DIMENSIONS")
    static EntityDimensions getSTANDING_DIMENSIONS() {
        throw new AssertionError();
    }

    @Accessor("POSE_DIMENSIONS")
    static Map<EntityPose, EntityDimensions> getPOSE_DIMENSIONS() {
        throw new AssertionError();
    }

    @Accessor("PLAYER_MODE_CUSTOMIZATION_ID")
    static TrackedData<Byte> getPLAYER_MODE_CUSTOMIZATION_ID() {
        throw new AssertionError();
    }

    @Accessor("MAIN_ARM_ID")
    static TrackedData<Byte> getMAIN_ARM() {
        throw new AssertionError();
    }

}
