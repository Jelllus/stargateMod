package com.stargate.mod.stargate.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.stargate.mod.StargateMod;
import com.stargate.mod.stargate.address.StargateAddress;
import com.stargate.mod.stargate.address.StargateSymbol;
import com.stargate.mod.stargate.connection.ConnectionManager;
import com.stargate.mod.stargate.connection.DialResult;
import com.stargate.mod.stargate.network.StargateNetwork;
import com.stargate.mod.stargate.network.StargateRegistry;
import com.stargate.mod.stargate.network.StargateRegistry.GateRecord;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Operator commands to exercise gates before they exist as blocks. Gates are named by their address.
 *
 * <pre>
 * /stargate create                     register a gate at your position
 * /stargate list
 * /stargate status &lt;gate&gt;
 * /stargate dial &lt;gate&gt; &lt;address&gt;       automatic dial, e.g. /stargate dial 1-2-3-4-5-6 7-8-9-10-11-12
 * /stargate encode &lt;gate&gt; &lt;symbol&gt;       manual dial, one symbol (0 = point of origin)
 * /stargate abort &lt;gate&gt;
 * /stargate close &lt;gate&gt;
 * /stargate remove &lt;gate&gt;
 * </pre>
 *
 * <p><i>FR —</i> Commandes d'opérateur pour tester les portes avant qu'elles existent en bloc. Les portes
 * sont désignées par leur adresse.
 *
 * <pre>
 * /stargate create                     enregistre une porte à votre position
 * /stargate list                       liste les portes et leur état
 * /stargate status &lt;porte&gt;             état détaillé d'une porte
 * /stargate dial &lt;porte&gt; &lt;adresse&gt;     numérotation automatique
 * /stargate encode &lt;porte&gt; &lt;symbole&gt;   numérotation manuelle, un symbole (0 = point d'origine)
 * /stargate abort &lt;porte&gt;              annule une numérotation
 * /stargate close &lt;porte&gt;              ferme le trou de ver sortant
 * /stargate remove &lt;porte&gt;             retire une porte
 * </pre>
 */
@EventBusSubscriber(modid = StargateMod.MODID)
public final class StargateCommand {
    private static final SimpleCommandExceptionType BAD_ADDRESS =
            new SimpleCommandExceptionType(Component.literal("Invalid address (expected e.g. 1-2-3-4-5-6)"));
    private static final SimpleCommandExceptionType NO_GATE =
            new SimpleCommandExceptionType(Component.literal("No gate has that address"));

    private StargateCommand() {
    }

    /** Hooks the command into NeoForge. / <i>FR —</i> Branche la commande sur NeoForge. */
    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    /**
     * Registers {@code /stargate} and its sub-commands; requires gamemaster permission.
     *
     * <p><i>FR —</i> Enregistre {@code /stargate} et ses sous-commandes ; nécessite la permission
     * d'opérateur (gamemaster).
     *
     * @param dispatcher the server command dispatcher / le répartiteur de commandes du serveur
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stargate")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("create").executes(StargateCommand::create))
                .then(Commands.literal("list").executes(StargateCommand::list))
                .then(Commands.literal("status")
                        .then(Commands.argument("gate", StringArgumentType.word()).executes(StargateCommand::status)))
                .then(Commands.literal("dial")
                        .then(Commands.argument("gate", StringArgumentType.word())
                                .then(Commands.argument("address", StringArgumentType.word()).executes(StargateCommand::dial))))
                .then(Commands.literal("encode")
                        .then(Commands.argument("gate", StringArgumentType.word())
                                .then(Commands.argument("symbol", IntegerArgumentType.integer(0, StargateSymbol.COUNT - 1))
                                        .executes(StargateCommand::encode))))
                .then(Commands.literal("abort")
                        .then(Commands.argument("gate", StringArgumentType.word()).executes(StargateCommand::abort)))
                .then(Commands.literal("close")
                        .then(Commands.argument("gate", StringArgumentType.word()).executes(StargateCommand::close)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("gate", StringArgumentType.word()).executes(StargateCommand::remove))));
    }

    private static int create(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        GlobalPos pos = GlobalPos.of(src.getLevel().dimension(), BlockPos.containing(src.getPosition()));
        GateRecord gate = StargateNetwork.registerGate(src.getServer(), pos);
        src.sendSuccess(() -> Component.literal("Gate " + gate.address() + " at " + describe(gate.location())), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        var gates = StargateNetwork.registry(src.getServer()).all();
        if (gates.isEmpty()) {
            src.sendSuccess(() -> Component.literal("No gates registered"), false);
            return 0;
        }
        ConnectionManager cm = StargateNetwork.connections();
        for (GateRecord g : gates) {
            src.sendSuccess(() -> Component.literal(g.address() + "  " + describe(g.location()) + "  " + cm.getState(g.id())), false);
        }
        return gates.size();
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        GateRecord gate = gate(ctx);
        ConnectionManager cm = StargateNetwork.connections();
        StargateRegistry registry = StargateNetwork.registry(src.getServer());
        StringBuilder sb = new StringBuilder()
                .append(gate.address()).append(": ").append(cm.getState(gate.id()))
                .append(", chevrons ").append(cm.getEncoded(gate.id()));
        cm.getConnection(gate.id()).ifPresent(c -> {
            boolean outgoing = c.isSource(gate.id());
            String other = registry.get(c.other(gate.id())).map(r -> r.address().toString()).orElse("?");
            sb.append(outgoing ? ", outgoing to " : ", incoming from ").append(other);
        });
        cm.getRemainingOpenTicks(gate.id()).ifPresent(t -> sb.append(", closes in ").append(t / 20).append("s"));
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int dial(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        GateRecord gate = gate(ctx);
        StargateAddress target = StargateAddress.parse(StringArgumentType.getString(ctx, "address"))
                .orElseThrow(BAD_ADDRESS::create);
        return report(ctx.getSource(), StargateNetwork.dial(gate.id(), target));
    }

    private static int encode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        GateRecord gate = gate(ctx);
        StargateSymbol symbol = StargateSymbol.of(IntegerArgumentType.getInteger(ctx, "symbol"));
        return report(ctx.getSource(), StargateNetwork.connections().encode(gate.id(), symbol));
    }

    private static int abort(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        boolean ok = StargateNetwork.connections().abortDial(gate(ctx).id());
        return feedback(ctx.getSource(), ok, "Dial aborted", "Gate is not dialing");
    }

    private static int close(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        boolean ok = StargateNetwork.connections().disconnect(gate(ctx).id());
        return feedback(ctx.getSource(), ok, "Wormhole closing", "Gate has no outgoing wormhole");
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        GateRecord gate = gate(ctx);
        StargateNetwork.removeGate(ctx.getSource().getServer(), gate.id());
        ctx.getSource().sendSuccess(() -> Component.literal("Gate " + gate.address() + " removed"), true);
        return 1;
    }

    /** Resolves the {@code gate} argument (an address) to a registered gate. / <i>FR —</i> Résout l'argument {@code gate} (une adresse) en porte enregistrée. */
    private static GateRecord gate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        StargateAddress address = StargateAddress.parse(StringArgumentType.getString(ctx, "gate"))
                .orElseThrow(BAD_ADDRESS::create);
        return StargateNetwork.registry(ctx.getSource().getServer()).findByAddress(address)
                .orElseThrow(NO_GATE::create);
    }

    private static int report(CommandSourceStack src, DialResult result) {
        return feedback(src, result.isSuccess(), result.name(), result.name());
    }

    private static int feedback(CommandSourceStack src, boolean ok, String success, String failure) {
        if (ok) {
            src.sendSuccess(() -> Component.literal(success), false);
            return 1;
        }
        src.sendFailure(Component.literal(failure));
        return 0;
    }

    private static String describe(GlobalPos pos) {
        return pos.dimension().identifier() + " " + pos.pos().toShortString();
    }
}
