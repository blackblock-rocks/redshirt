package rocks.blackblock.redshirt.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import rocks.blackblock.redshirt.npc.RedshirtEntity;

import static net.minecraft.server.command.CommandManager.literal;

public class Commands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(Commands::addCommands);
    }

    private static void addCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {

        LiteralArgumentBuilder<ServerCommandSource> redshirt = literal("redshirt").requires(source -> source.hasPermissionLevel(2));
        LiteralArgumentBuilder<ServerCommandSource> create = literal("create");
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

        create.then(name);
        redshirt.then(create);
        dispatcher.register(redshirt);
    }

}
