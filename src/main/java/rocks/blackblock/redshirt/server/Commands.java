package rocks.blackblock.redshirt.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import rocks.blackblock.redshirt.Redshirt;
import rocks.blackblock.redshirt.npc.RedshirtEntity;

import static net.minecraft.server.command.CommandManager.literal;

public class Commands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(Commands::addCommands);
    }

    private static void addCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {

        LiteralArgumentBuilder<ServerCommandSource> redshirt = literal("redshirt").requires(source -> source.hasPermissionLevel(2));
        LiteralArgumentBuilder<ServerCommandSource> create = literal("create");
        LiteralArgumentBuilder<ServerCommandSource> list = literal("list");
        var name = CommandManager.argument("name", StringArgumentType.greedyString());

        name.executes(context -> {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayer();

            String npc_name = StringArgumentType.getString(context, "name");

            try {
                RedshirtEntity npc = RedshirtEntity.create(player, npc_name);
                player.getWorld().spawnEntity(npc);

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }

            return 1;
        });

        list.executes(context -> {

            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayer();

            for (RedshirtEntity entity : Redshirt.REDSHIRTS) {
                BlockPos pos = entity.getBlockPos();
                String entity_name = entity.getEntityName();

                source.sendFeedback(() -> Text.literal(" - " + entity_name + ": " + pos), false);

            }

            return 1;

        });

        create.then(name);
        redshirt.then(create);
        redshirt.then(list);
        dispatcher.register(redshirt);
    }

}
