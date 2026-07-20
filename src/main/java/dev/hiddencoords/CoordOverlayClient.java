package dev.hiddencoords;

import dev.hiddencoords.ui.CoordWindow;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class CoordOverlayClient implements ClientModInitializer {
    private static final long UPDATE_INTERVAL_NANOS = 200_000_000L;
    private final CoordWindow window = new CoordWindow();
    private long nextUpdate;
    private boolean wasInWorld;
    private KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(createToggleKey());
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> window.shutdown());
    }

    private void onEndTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) window.toggle();
        if (client.player == null || client.world == null) {

            if (wasInWorld) window.hideForNoWorld();
            wasInWorld = false;
            return;
        }
        wasInWorld = true;
        long now = System.nanoTime();
        if (now < nextUpdate) return;
        nextUpdate = now + UPDATE_INTERVAL_NANOS;

        BlockPos pos = client.player.getBlockPos();
        RegistryEntry<Biome> biome = client.world.getBiome(pos);

        Identifier biomeId = biome.getKey().map(key -> key.getValue()).orElse(null);

        String biomeName = biomeId == null ? "Unknown biome" : translatedBiomeName(biomeId);
        Identifier dimensionId = client.world.getRegistryKey().getValue();
        window.update(new CoordinateSnapshot(pos.getX(), pos.getY(), pos.getZ(), dimensionName(dimensionId), biomeName));
    }

    private static String dimensionName(Identifier id) {
        return switch (id.toString()) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> prettyName(id.getPath());
        };
    }

    private static KeyBinding createToggleKey() {
        try {
            return KeyBinding.class
                    .getConstructor(String.class, InputUtil.Type.class, int.class, String.class)
                    .newInstance("key.hidden_coords.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6,
                            "key.category.hidden_coords");
        } catch (ReflectiveOperationException oldConstructorMissing) {
            try {
                Class<?> categoryClass = findCategoryClass();
                Object category = createCategory(categoryClass);
                return (KeyBinding) KeyBinding.class
                        .getConstructor(String.class, InputUtil.Type.class, int.class, categoryClass)
                        .newInstance("key.hidden_coords.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, category);
            } catch (ReflectiveOperationException newConstructorMissing) {
                throw new IllegalStateException("Could not create Hidden Coords keybinding", newConstructorMissing);
            }
        }
    }

    private static Class<?> findCategoryClass() throws ClassNotFoundException {

        return KeyBinding.class.getClassLoader().loadClass("net.minecraft.class_304$class_11900");
    }

    private static Object createCategory(Class<?> categoryClass) throws ReflectiveOperationException {
        for (Method method : categoryClass.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == categoryClass
                    && method.getParameterCount() == 1 && method.getParameterTypes()[0] == Identifier.class) {
                return method.invoke(null, Identifier.of("hidden_coords", "main"));
            }
        }
        throw new NoSuchMethodException("Identifier-to-Category factory");
    }

    private static String translatedBiomeName(Identifier biomeId) {
        String translationKey = "biome." + biomeId.getNamespace() + "." + biomeId.getPath().replace('/', '.');
        String name = Text.translatable(translationKey).getString();
        return name.equals(translationKey) ? prettyName(biomeId.getPath()) : name;
    }

    private static String prettyName(String path) {
        String[] words = path.replace('-', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
