package rocks.blackblock.redshirt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.blackblock.core.config.Config;
import rocks.blackblock.redshirt.config.RedshirtConfig;
import rocks.blackblock.redshirt.npc.RedshirtEntity;
import rocks.blackblock.redshirt.server.Commands;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class Redshirt implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("redshirt");
	public static final Identifier NPC_ID = new Identifier("redshirt", "npc");
	public static final List<Identifier> REDSHIRT_TYPE_IDENTIFIERS = new ArrayList<>();
	public static final List<EntityType<? extends RedshirtEntity>> REDSHIRT_TYPES = new ArrayList<>();
	public static MinecraftDedicatedServer SERVER = null;

	public static final RedshirtConfig CONFIG = Config.getOrCreateConfig("redshirt", RedshirtConfig::new);

	/**
	 * A list of all loaded RedshirtEntities
	 *
	 * @since   0.1.0
	 */
	public static final LinkedHashSet<RedshirtEntity> REDSHIRTS = new LinkedHashSet<>();

	/**
	 * Initialize the Redshirt mod
	 *
	 * @author  Jelle De Loecker   <jelle@elevenways.be>
	 * @since   0.1.0
	 */
	@Override
	public void onInitialize() {

		// Register the basic Redshirt NPC type
		RedshirtEntity.REDSHIRT_TYPE = registerType(NPC_ID, RedshirtEntity::new, RedshirtEntity.createDefaultAttributes());

		// Register all commands
		Commands.registerCommands();
	}

	/**
	 * Register a new Redshirt Entity type
	 *
	 * @author  Jelle De Loecker   <jelle@elevenways.be>
	 * @since   0.1.0
	 */
	public static <TRedshirtEntity extends RedshirtEntity> EntityType<TRedshirtEntity> registerType(
			Identifier entity_id,
			EntityType.EntityFactory<TRedshirtEntity> entity_factory,
			DefaultAttributeContainer.Builder builder
	) {

		EntityType<TRedshirtEntity> NEW_ENTITY_TYPE = Registry.register(
				Registries.ENTITY_TYPE,
				entity_id,
				FabricEntityTypeBuilder
						.create(SpawnGroup.MISC, entity_factory)
						.dimensions(EntityDimensions.fixed(0.6F, 1.8F))
						.build()
		);

		FabricDefaultAttributeRegistry.register(NEW_ENTITY_TYPE, builder);

		REDSHIRT_TYPE_IDENTIFIERS.add(entity_id);
		REDSHIRT_TYPES.add(NEW_ENTITY_TYPE);

		return NEW_ENTITY_TYPE;
	}
}