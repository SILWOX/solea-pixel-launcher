package com.soleapixel.bmcmod.item.builderwand;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.item.builderwand.BuilderWandTier.WandModeSpec;

public final class BuilderWandPlacement {
    private static final int MAX_BLOCKS_PER_USE = 512;

    private BuilderWandPlacement() {
    }

    /**
     * @param viewLook direction de regard du joueur (ex. {@code player.getLookAngle()}), utilisée pour orienter
     *                 plans / murs / croix sur la face (choix nord-sud vs est-ouest sur le dessus d’un bloc, etc.).
     * @param placementTurn quart de tour (0–3) dans le plan de la face : fait tourner l’axe “largeur” sans bouger la tête.
     */
    public static List<BlockPos> computePositions(BlockPos clicked, Direction face, WandModeSpec spec, Vec3 viewLook, int placementTurn) {
        Vec3 look = twistLookInFacePlane(viewLook, face, placementTurn);
        BlockPos start = clicked.relative(face);
        List<BlockPos> out = new ArrayList<>();
        switch (spec.kind()) {
            case LINE -> {
                for (int i = 0; i < spec.a(); i++) {
                    if (!add(out, start.relative(face, i))) {
                        return out;
                    }
                }
            }
            case PLANE -> {
                PlanarAxes ax = planarAxes(face, look);
                fillPlane(out, start, ax.u(), ax.v(), spec.a(), spec.b(), false);
            }
            case HOLLOW_PLANE -> {
                PlanarAxes ax = planarAxes(face, look);
                fillPlane(out, start, ax.u(), ax.v(), spec.a(), spec.b(), true);
            }
            case CROSS -> {
                PlanarAxes ax = planarAxes(face, look);
                int mi = spec.a() / 2;
                int mj = spec.b() / 2;
                for (int i = 0; i < spec.a(); i++) {
                    for (int j = 0; j < spec.b(); j++) {
                        if (i == mi || j == mj) {
                            if (!add(out, start.relative(ax.u(), i).relative(ax.v(), j))) {
                                return out;
                            }
                        }
                    }
                }
            }
            case CORNER -> {
                PlanarAxes ax = planarAxes(face, look);
                for (int i = 0; i < spec.a(); i++) {
                    if (!add(out, start.relative(ax.u(), i))) {
                        return out;
                    }
                }
                for (int j = 1; j < spec.b(); j++) {
                    if (!add(out, start.relative(ax.v(), j))) {
                        return out;
                    }
                }
            }
            case DIAGONAL -> {
                PlanarAxes ax = planarAxes(face, look);
                for (int k = 0; k < spec.a(); k++) {
                    if (!add(out, start.relative(ax.u(), k).relative(ax.v(), k))) {
                        return out;
                    }
                }
            }
            case WALL -> {
                Direction u = wallWidthAxis(face, look);
                fillWall(out, start, u, spec.a(), spec.b(), false);
            }
            case HOLLOW_WALL -> {
                Direction u = wallWidthAxis(face, look);
                fillWall(out, start, u, spec.a(), spec.b(), true);
            }
        }
        return out;
    }

    public static List<BlockPos> computePositions(BlockPos clicked, Direction face, WandModeSpec spec, Vec3 viewLook) {
        return computePositions(clicked, face, spec, viewLook, 0);
    }

    /**
     * Fait tourner le vecteur de référence de {@code k} × 90° autour de la normale de la face (repère main droite),
     * pour que les modes sensibles au regard tournent “sur eux-mêmes” sans changer la vue du joueur.
     */
    private static Vec3 twistLookInFacePlane(Vec3 viewLook, Direction face, int placementTurn) {
        int k = Math.floorMod(placementTurn, 4);
        if (k == 0) {
            return viewLook;
        }
        Vec3 n = dirVec(face);
        double nLen = n.length();
        if (nLen < 1.0E-8) {
            return viewLook;
        }
        n = n.scale(1.0D / nLen);
        Vec3 v = viewLook.lengthSqr() > 1.0E-8 ? viewLook.normalize() : new Vec3(0.0D, -1.0D, 0.01D);
        Vec3 proj = v.subtract(n.scale(v.dot(n)));
        if (proj.lengthSqr() < 1.0E-6) {
            proj = dirVec(tangentU(face));
        } else {
            proj = proj.normalize();
        }
        Vec3 rotated = proj;
        for (int i = 0; i < k; i++) {
            rotated = n.cross(rotated);
            double rl = rotated.length();
            if (rl < 1.0E-8) {
                return viewLook;
            }
            rotated = rotated.scale(1.0D / rl);
        }
        return rotated;
    }

    private record PlanarAxes(Direction u, Direction v) {
    }

    private static Vec3 dirVec(Direction d) {
        return new Vec3(d.getStepX(), d.getStepY(), d.getStepZ());
    }

    private static boolean perpendicular(Direction a, Direction b) {
        return a.getStepX() * b.getStepX() + a.getStepY() * b.getStepY() + a.getStepZ() * b.getStepZ() == 0;
    }

    /**
     * Axe de largeur du mur : horizontal dans le plan de la face (pas l’axe Y), choisi selon le regard.
     * Hauteur du mur = toujours {@link Direction#UP}.
     */
    private static Direction wallWidthAxis(Direction face, Vec3 viewLook) {
        return pickPrimaryInPlane(face, viewLook, d -> perpendicular(face, d) && d.getAxis() != Direction.Axis.Y);
    }

    /**
     * Deux directions orthogonales dans le plan de la face, avec {@code u × v} aligné sur la normale {@code face}
     * (repère main droite cohérent pour remplir le rectangle).
     */
    private static PlanarAxes planarAxes(Direction face, Vec3 viewLook) {
        Direction u = pickPrimaryInPlane(face, viewLook, d -> perpendicular(face, d));
        Direction v = companionV(face, u);
        return new PlanarAxes(u, v);
    }

    private static Direction pickPrimaryInPlane(Direction face, Vec3 viewLook, java.util.function.Predicate<Direction> candidateFilter) {
        Vec3 n = dirVec(face);
        Vec3 v = viewLook.lengthSqr() > 1.0E-8 ? viewLook.normalize() : new Vec3(0.0D, -1.0D, 0.0D);
        Vec3 proj = v.subtract(n.scale(v.dot(n)));
        if (proj.lengthSqr() < 1.0E-6) {
            return tangentU(face);
        }
        proj = proj.normalize();
        Direction best = tangentU(face);
        double bestDot = -Double.MAX_VALUE;
        for (Direction d : Direction.values()) {
            if (!candidateFilter.test(d)) {
                continue;
            }
            if (!perpendicular(face, d)) {
                continue;
            }
            Vec3 dv = dirVec(d);
            double dot = proj.dot(dv);
            if (dot > bestDot) {
                bestDot = dot;
                best = d;
            }
        }
        return best;
    }

    /** Choisit {@code v} dans le plan de la face tel que {@code u × v} pointe comme la normale {@code face}. */
    private static Direction companionV(Direction face, Direction u) {
        Vec3 n = dirVec(face);
        for (Direction v : Direction.values()) {
            if (!perpendicular(face, v) || !perpendicular(u, v)) {
                continue;
            }
            Vec3 cr = dirVec(u).cross(dirVec(v));
            if (cr.lengthSqr() < 1.0E-8) {
                continue;
            }
            if (cr.dot(n) > 0.0D) {
                return v;
            }
        }
        return tangentV(face);
    }

    private static void fillPlane(List<BlockPos> out, BlockPos start, Direction u, Direction v, int a, int b, boolean hollowOnly) {
        boolean fillAll = hollowOnly && (a <= 2 || b <= 2);
        for (int i = 0; i < a; i++) {
            for (int j = 0; j < b; j++) {
                if (hollowOnly && !fillAll && i != 0 && i != a - 1 && j != 0 && j != b - 1) {
                    continue;
                }
                if (!add(out, start.relative(u, i).relative(v, j))) {
                    return;
                }
            }
        }
    }

    private static void fillWall(List<BlockPos> out, BlockPos start, Direction u, int a, int b, boolean hollowOnly) {
        Direction up = Direction.UP;
        boolean fillAll = hollowOnly && (a <= 2 || b <= 2);
        for (int i = 0; i < a; i++) {
            for (int j = 0; j < b; j++) {
                if (hollowOnly && !fillAll && i != 0 && i != a - 1 && j != 0 && j != b - 1) {
                    continue;
                }
                if (!add(out, start.relative(u, i).relative(up, j))) {
                    return;
                }
            }
        }
    }

    private static boolean add(List<BlockPos> out, BlockPos pos) {
        if (out.size() >= MAX_BLOCKS_PER_USE) {
            return false;
        }
        out.add(pos);
        return true;
    }

    private static Direction tangentU(Direction face) {
        if (face.getAxis() == Direction.Axis.Y) {
            return Direction.NORTH;
        }
        return face.getCounterClockWise(Direction.Axis.Y);
    }

    private static Direction tangentV(Direction face) {
        if (face.getAxis() == Direction.Axis.Y) {
            return Direction.EAST;
        }
        return Direction.UP;
    }

    /**
     * @return nombre de blocs effectivement posés
     */
    public static int placeAll(ServerPlayer player, ServerLevel level, List<BlockPos> positions, BlockState toPlace, ItemStack wand, EquipmentSlot wandSlot) {
        Block block = toPlace.getBlock();
        if (block.asItem().getDefaultInstance().isEmpty()) {
            return 0;
        }
        int placed = 0;
        for (BlockPos pos : positions) {
            if (!level.isInWorldBounds(pos)) {
                continue;
            }
            if (!player.mayInteract(level, pos)) {
                continue;
            }
            BlockState existing = level.getBlockState(pos);
            if (!existing.canBeReplaced()) {
                continue;
            }
            if (!toPlace.canSurvive(level, pos)) {
                continue;
            }
            int need = 1;
            if (!consumeBlocks(player, block, need)) {
                break;
            }
            if (!level.setBlock(pos, toPlace, Block.UPDATE_ALL_IMMEDIATE)) {
                player.getInventory().add(new ItemStack(block));
                break;
            }
            toPlace.getBlock().setPlacedBy(level, pos, toPlace, player, new ItemStack(block.asItem()));
            SoundType st = toPlace.getSoundType(level, pos, player);
            level.playSound(null, pos, st.getPlaceSound(), SoundSource.BLOCKS, (st.getVolume() + 1.0F) / 2.0F, st.getPitch() * 0.8F);
            player.awardStat(Stats.ITEM_USED.get(block.asItem()));
            placed++;
            if (!player.getAbilities().instabuild) {
                wand.hurtAndBreak(1, player, wandSlot);
                if (wand.isEmpty() || wand.getDamageValue() >= wand.getMaxDamage()) {
                    break;
                }
            }
        }
        return placed;
    }

    private static boolean consumeBlocks(ServerPlayer player, Block block, int count) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        ItemStack need = new ItemStack(block.asItem());
        if (need.isEmpty()) {
            return false;
        }
        int remaining = count;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack s = player.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameComponents(s, need)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
        return remaining == 0;
    }
}
