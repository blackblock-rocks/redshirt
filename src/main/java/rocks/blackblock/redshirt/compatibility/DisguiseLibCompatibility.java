package rocks.blackblock.redshirt.compatibility;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import rocks.blackblock.redshirt.npc.RedshirtEntity;
import xyz.nucleoid.disguiselib.api.EntityDisguise;

/**
 * Just DisguiseLib methods in their own
 * class, in order to make
 * the mod run without the lib as well.
 *
 * @author Samo_lego
 */
public class DisguiseLibCompatibility {

    public static void disguiseAs(RedshirtEntity redshirt, Entity entity) {
        ((EntityDisguise) redshirt).disguiseAs(entity);
    }

    public static void setGameProfile(RedshirtEntity redshirt, GameProfile gameProfile) {
        ((EntityDisguise) redshirt).setGameProfile(gameProfile);
    }

    public static void clearDisguise(RedshirtEntity redshirt) {
        ((EntityDisguise) redshirt).removeDisguise();
    }

    public static boolean isDisguised(Entity entity) {
        return ((EntityDisguise) entity).isDisguised();
    }
}
