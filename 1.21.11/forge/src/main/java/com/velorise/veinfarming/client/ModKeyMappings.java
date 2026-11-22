package com.velorise.veinfarming.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("vein_farming", "general")
    );

    public static final KeyMapping KEY_ACTIVATE = new KeyMapping(
            "key.vein_farming.activate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            CATEGORY
    );

    public static final KeyMapping KEY_TOGGLE_SMART_PLANT = new KeyMapping(
            "key.vein_farming.toggle_smart_plant",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    private static boolean smartPlantEnabled = true;

    public static boolean isSmartPlantEnabled() {
        return smartPlantEnabled;
    }

    public static void setSmartPlantEnabled(boolean enabled) {
        smartPlantEnabled = enabled;
    }

    private static boolean isKeyPhysicallyDown(Minecraft mc, KeyMapping mapping) {
        if (mapping == null || mapping.isUnbound()) {
            return false;
        }
        InputConstants.Key key = mapping.getKey();
        long window = mc.getWindow().handle();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return mapping.isDown();
    }

    public static boolean isKeyActive() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return false;
        }
        if (!KEY_ACTIVATE.isUnbound()) {
            return isKeyPhysicallyDown(mc, KEY_ACTIVATE);
        }
        return isKeyPhysicallyDown(mc, mc.options.keyShift);
    }

    private ModKeyMappings() {
    }
}
