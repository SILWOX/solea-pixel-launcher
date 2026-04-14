/**
 * Discord Application ID (Rich Presence). Numeric snowflake from https://discord.com/developers/applications
 *
 * Add Rich Presence art assets with keys: `logo`, `solea_pack_palamod`, `solea_pack_wither` (see modpacks.ts).
 *
 * Resolution order (first non-empty wins):
 * 1. Environment variable `SOLEA_DISCORD_CLIENT_ID`
 * 2. File `discord-rpc-client-id.txt` in the launcher userData folder (single line, digits only)
 * 3. This constant
 *
 * Ne pas mettre le token du bot ici : la Rich Presence n’en a pas besoin et le token ne doit jamais être versionné.
 */
export const DISCORD_RPC_APPLICATION_ID = '1152936997443354674'
