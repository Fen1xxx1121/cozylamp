package com.cozylamp;

import net.fabricmc.api.ModInitializer;

public class CozyLamp implements ModInitializer {
    public static final String MOD_ID = "cozylamp";

    @Override
    public void onInitialize() {
        ModRegistry.init();
    }
}
