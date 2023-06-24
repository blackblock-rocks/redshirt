package rocks.blackblock.redshirt.mixin.accessors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.data.DataTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(DataTracker.class)
public interface DataTrackerAccessor {

    @Accessor("entries")
    public Int2ObjectMap<DataTracker.Entry<?>> getEntries();

}
