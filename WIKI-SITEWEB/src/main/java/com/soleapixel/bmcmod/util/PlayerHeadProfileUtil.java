package com.soleapixel.bmcmod.util;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import com.soleapixel.bmcmod.BmcMod;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Résout une tête de joueur avec skin Mojang (cache serveur, session service, ou APIs HTTP si pas de serveur).
 */
public final class PlayerHeadProfileUtil {
    private static final HttpClient MOJANG_HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private PlayerHeadProfileUtil() {
    }

    public static ItemStack createPlayerHead(String playerName, @Nullable MinecraftServer server) {
        if (server == null) {
            server = ServerLifecycleHooks.getCurrentServer();
        }
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        GameProfile profile = resolveGameProfile(server, playerName.trim());
        stack.set(DataComponents.PROFILE, new ResolvableProfile(profile));
        return stack;
    }

    public static GameProfile resolveGameProfile(@Nullable MinecraftServer server, String name) {
        if (name.isEmpty()) {
            return new GameProfile(UUIDUtil.createOfflinePlayerUUID("Player"), "Player");
        }

        if (server != null) {
            ServerPlayer online = findOnlineCaseInsensitive(server, name);
            if (online != null) {
                return online.getGameProfile();
            }

            UUID uuid = null;
            String resolvedName = name;
            if (server.getProfileCache() != null) {
                Optional<GameProfile> cached = server.getProfileCache().get(name);
                if (cached.isPresent()) {
                    GameProfile g = cached.get();
                    if (g.getId() != null) {
                        uuid = g.getId();
                        if (g.getName() != null) {
                            resolvedName = g.getName();
                        }
                    }
                }
            }

            if (uuid == null) {
                Optional<MojangLookup> moj = fetchUuidFromMojangApi(name);
                if (moj.isPresent()) {
                    uuid = moj.get().uuid();
                    resolvedName = moj.get().canonicalName();
                }
            }

            if (uuid != null) {
                try {
                    ProfileResult result = server.getSessionService().fetchProfile(uuid, false);
                    if (result != null) {
                        GameProfile filled = result.profile();
                        if (filled != null) {
                            return filled;
                        }
                    }
                } catch (Exception e) {
                    BmcMod.LOGGER.debug("PlayerHeadProfileUtil: fetchProfile failed for {}: {}", name, e.toString());
                }
                return new GameProfile(uuid, resolvedName);
            }

            return new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
        }

        // Client multijoueur (pas de MinecraftServer) : UUID + textures via APIs publiques Mojang.
        Optional<MojangLookup> moj = fetchUuidFromMojangApi(name);
        if (moj.isEmpty()) {
            return new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
        }
        UUID uuid = moj.get().uuid();
        String resolvedName = moj.get().canonicalName();
        Optional<GameProfile> withTextures = fetchProfileFromSessionServer(uuid, resolvedName);
        return withTextures.orElseGet(() -> new GameProfile(uuid, resolvedName));
    }

    private static Optional<GameProfile> fetchProfileFromSessionServer(UUID uuid, String fallbackName) {
        try {
            String undashed = uuid.toString().replace("-", "").toLowerCase(Locale.ROOT);
            String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + undashed + "?unsigned=false";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> resp = MOJANG_HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200 || resp.body() == null || resp.body().isEmpty()) {
                return Optional.empty();
            }
            JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
            String pname = root.has("name") ? root.get("name").getAsString() : fallbackName;
            GameProfile profile = new GameProfile(uuid, pname);
            if (root.has("properties") && root.get("properties").isJsonArray()) {
                JsonArray arr = root.getAsJsonArray("properties");
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject prop = el.getAsJsonObject();
                    if (!prop.has("name") || !prop.has("value")) {
                        continue;
                    }
                    String n = prop.get("name").getAsString();
                    String v = prop.get("value").getAsString();
                    String sig = prop.has("signature") && !prop.get("signature").isJsonNull()
                            ? prop.get("signature").getAsString()
                            : null;
                    profile.getProperties().put(n, new Property(n, v, sig));
                }
            }
            return Optional.of(profile);
        } catch (Exception e) {
            BmcMod.LOGGER.debug("PlayerHeadProfileUtil: session server lookup failed: {}", e.toString());
            return Optional.empty();
        }
    }

    private static ServerPlayer findOnlineCaseInsensitive(MinecraftServer server, String name) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getGameProfile().getName() != null && p.getGameProfile().getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    private static Optional<MojangLookup> fetchUuidFromMojangApi(String name) {
        try {
            String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
            String url = "https://api.mojang.com/users/profiles/minecraft/" + encoded;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> resp = MOJANG_HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200 || resp.body() == null || resp.body().isEmpty()) {
                return Optional.empty();
            }
            JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (!root.has("id") || !root.has("name")) {
                return Optional.empty();
            }
            String undashed = root.get("id").getAsString();
            String canonical = root.get("name").getAsString();
            UUID uuid = uuidFromUndashedHex(undashed);
            return Optional.of(new MojangLookup(uuid, canonical));
        } catch (Exception e) {
            BmcMod.LOGGER.debug("PlayerHeadProfileUtil: Mojang API lookup failed for {}: {}", name, e.toString());
            return Optional.empty();
        }
    }

    private static UUID uuidFromUndashedHex(String undashed) {
        String s = undashed.toLowerCase(Locale.ROOT);
        if (s.length() != 32) {
            throw new IllegalArgumentException("Invalid UUID string: " + undashed);
        }
        return UUID.fromString(s.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5"));
    }

    private record MojangLookup(UUID uuid, String canonicalName) {
    }
}
