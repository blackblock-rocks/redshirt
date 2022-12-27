package rocks.blackblock.redshirt.polymc;

import io.github.theepicblock.polymc.impl.poly.wizard.AbstractVirtualEntity;
import net.minecraft.entity.EntityType;

public class VRedshirtEntity extends AbstractVirtualEntity {

    private EntityType<?> entity_type = null;

    public VRedshirtEntity(EntityType<?> entity_type) {
        this.entity_type = entity_type;
    }

    @Override
    public EntityType<?> getEntityType() {
        return this.entity_type;
    }
}
