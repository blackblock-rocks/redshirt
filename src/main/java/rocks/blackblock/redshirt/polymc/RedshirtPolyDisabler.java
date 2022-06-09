package rocks.blackblock.redshirt.polymc;

import io.github.theepicblock.polymc.api.PolyMcEntrypoint;
import io.github.theepicblock.polymc.api.PolyRegistry;
import net.minecraft.entity.EntityType;
import rocks.blackblock.redshirt.Redshirt;
import rocks.blackblock.redshirt.npc.RedshirtEntity;

public class RedshirtPolyDisabler implements PolyMcEntrypoint {

    @Override
    public void registerPolys(PolyRegistry registry) {

        // Disable the polys
        for (EntityType<? extends RedshirtEntity> type : Redshirt.REDSHIRT_TYPES) {
            registry.registerEntityPoly(type, (info, entity) -> null);
        }
    }
}
