package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.soleapixel.bmcmod.item.melody.MelodyHornTune;
import com.soleapixel.bmcmod.item.melody.MelodyHornTuneRegistry;
import com.soleapixel.bmcmod.item.melody.MelodyHornTunes;
import com.soleapixel.bmcmod.item.melody.MelodyNote;
import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.rarity.RarityStickItem;

/**
 * Corne mythique : maintien = boucle de notes type bloc-note ; thème tiré au hasard à chaque nouvelle utilisation.
 */
public final class MelodyHornItem extends RarityStickItem {
    /** Durée « infinie » tant que le joueur maintient (comme l’arc / le bâton d’écho). */
    public static final int USE_DURATION = 72_000;
    /** Ancien espacement des boucles courtes intégrées (les JSON utilisent leur propre timing). */
    public static final int TICKS_PER_NOTE = MelodyHornTunes.BUILTIN_TICKS_PER_STEP;
    /** Volume proche du bloc-note vanilla. */
    private static final float NOTE_VOLUME = 3.0F;

    private static final String TAG_TUNE = "bmcmod:melody_tune";
    /** Position dans la boucle (tick), modulo longueur de la partition. */
    private static final String TAG_TICK = "bmcmod:melody_tick";

    public MelodyHornItem(net.minecraft.world.item.Item.Properties properties) {
        super(properties, BmcModRarity.MYTHIC);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.pass(stack);
        }
        if (MelodyHornTuneRegistry.size() <= 0) {
            return InteractionResultHolder.pass(stack);
        }
        initSession(sp);
        sp.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    private static void initSession(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        int tune = player.getRandom().nextInt(MelodyHornTuneRegistry.size());
        root.putInt(TAG_TUNE, tune);
        root.putInt(TAG_TICK, 0);
        showPlayingTune(player, tune);
    }

    /** Action-bar line (subtitle-style) so the player knows which inspired tune is looping. */
    private static void showPlayingTune(ServerPlayer player, int tuneIndex) {
        Component title = Component.translatable(MelodyHornTuneRegistry.translationKey(tuneIndex));
        Component line = Component.translatable("bmcmod.melody_horn.subtitle", title)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        player.displayClientMessage(line, true);
    }

    private static void clearSession(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        root.remove(TAG_TUNE);
        root.remove(TAG_TICK);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer sp)) {
            return;
        }
        CompoundTag root = sp.getPersistentData();
        if (!root.contains(TAG_TUNE)) {
            initSession(sp);
        }
        int tuneIdx = root.getInt(TAG_TUNE);
        int total = MelodyHornTuneRegistry.size();
        if (total <= 0) {
            return;
        }
        if (tuneIdx < 0 || tuneIdx >= total) {
            tuneIdx = 0;
            root.putInt(TAG_TUNE, tuneIdx);
        }
        MelodyHornTune tune = MelodyHornTuneRegistry.get(tuneIdx);
        if (tune == null || tune.loopLengthTicks() <= 0) {
            return;
        }
        int tick = root.getInt(TAG_TICK);
        for (MelodyNote note : tune.notesAt(tick)) {
            float pitch = NoteBlock.getPitchFromNote(note.note());
            float vol = NOTE_VOLUME * note.volume();
            // null player: broadcast to everyone nearby (including the blower).
            level.playSeededSound(
                    null,
                    sp.getX(),
                    sp.getY(),
                    sp.getZ(),
                    note.instrument().getSoundEvent(),
                    SoundSource.RECORDS,
                    vol,
                    pitch,
                    level.random.nextLong());
            if (level instanceof ServerLevel sl) {
                spawnNoteParticlesAtHornTip(sl, sp, note.note());
            }
        }
        root.putInt(TAG_TICK, (tick + 1) % tune.loopLengthTicks());
    }

    /**
     * Vanilla NOTE particles: with {@code count == 0}, the client uses {@code maxSpeed * xDist} as the
     * colour parameter (same idea as {@link NoteBlock} / note {@code i} / 24).
     */
    private static void spawnNoteParticlesAtHornTip(ServerLevel level, ServerPlayer player, int noteId) {
        Vec3 forward = player.getViewVector(1.0F);
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(forward.x, 0.0, -forward.z);
        }
        side = side.normalize();
        HumanoidArm main = player.getMainArm();
        if (player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            main = main.getOpposite();
        }
        double sideSign = main == HumanoidArm.RIGHT ? 1.0 : -1.0;
        Vec3 tip = eye.add(forward.scale(0.42)).add(side.scale(0.16 * sideSign)).add(0.0, -0.1, 0.0);
        double colour = Mth.clamp(noteId, 0, 24) / 24.0;
        for (int i = 0; i < 3; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.08;
            double oy = (level.random.nextDouble() - 0.5) * 0.06;
            double oz = (level.random.nextDouble() - 0.5) * 0.08;
            level.sendParticles(ParticleTypes.NOTE, tip.x + ox, tip.y + oy, tip.z + oz, 0, colour, 0.0, 0.0, 1.0);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!level.isClientSide() && entity instanceof ServerPlayer sp) {
            clearSession(sp);
        }
        super.releaseUsing(stack, level, entity, timeCharged);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
