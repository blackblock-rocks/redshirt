package rocks.blackblock.redshirt.mixin.accessors;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.OptionalInt;

@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {

    @Accessor("ABSORPTION_AMOUNT")
    static TrackedData<Float> getABSORPTION_AMOUNT() {
        throw new AssertionError();
    }

    @Accessor("SCORE")
    static TrackedData<Integer> getSCORE() {
        throw new AssertionError();
    }

    @Accessor("LEFT_SHOULDER_PARROT_VARIANT_ID")
    static TrackedData<OptionalInt> getLEFT_SHOULDER_ENTITY() {
        throw new AssertionError();
    }

    @Accessor("RIGHT_SHOULDER_PARROT_VARIANT_ID")
    static TrackedData<OptionalInt> getRIGHT_SHOULDER_ENTITY() {
        throw new AssertionError();
    }
}
