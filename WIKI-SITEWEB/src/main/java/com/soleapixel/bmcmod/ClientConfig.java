package com.soleapixel.bmcmod;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Options purement client (fichier {@code bmcmod-client.toml}). */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue CROSSHAIR_RED_ON_ENTITY = BUILDER
            .comment("When enabled, the crosshair turns red while you look at a mob or another player.")
            .define("crosshairRedOnEntity", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
