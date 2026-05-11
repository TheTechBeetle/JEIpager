package com.example.examplemod;

import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("hello")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    ctx.getSource().sendSuccess(() -> Component.literal("hello " + name), false);
                                    return 1;
                                })
                        )
        );

        dispatcher.register(
                Commands.literal("multiply")
                        .then(Commands.argument("a", FloatArgumentType.floatArg())
                        .then(Commands.argument("b", FloatArgumentType.floatArg())
                        .executes(ctx -> {
                            Float a = FloatArgumentType.getFloat(ctx, "a");
                            Float b = FloatArgumentType.getFloat(ctx, "b");
                            ctx.getSource().sendSuccess(() -> Component.literal(String.format("%f x %f = %f", a, b, a * b)), false);
                            return 1;
                        })))
        );
        dispatcher.register(
                Commands.literal("exportjson")
                        .executes(ctx -> {
                            MinecraftServer server = ctx.getSource().getServer();

                            Path output = server.getFile("all-recipes.json").toAbsolutePath();

                            JsonArray array = new JsonArray();
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();

                            RecipeManager manager = server.getRecipeManager();

                            for (RecipeHolder<?> holder : manager.getRecipes()) {
                                Recipe<?> recipe = holder.value();
                                RecipeSerializer<?> serializer = recipe.getSerializer();

                                JsonObject json = new JsonObject();
                                json.addProperty("id", holder.id().toString());
                                json.addProperty("type", recipe.getType().toString());

                                // ⭐ 1.21: MapCodec -> Codec -> encodeStart(JsonOps,…)
                                @SuppressWarnings("unchecked")
                                MapCodec<Recipe<?>> mapCodec = (MapCodec<Recipe<?>>) serializer.codec();
                                Codec<Recipe<?>> codec = mapCodec.codec();

                                JsonElement data = codec
                                        .encodeStart(JsonOps.INSTANCE, recipe)
                                        .getOrThrow();

                                json.add("data", data);
                                array.add(json);
                            }

                            try (Writer writer = Files.newBufferedWriter(output)) {
                                gson.toJson(array, writer);
                            } catch (IOException e) {
                                ctx.getSource().sendFailure(Component.literal("Failed: " + e.getMessage()));
                                return 0;
                            }

                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Exported " + array.size() + " recipes to " + output),
                                    false
                            );

                            return 1;
                        })


        );
    }
}
