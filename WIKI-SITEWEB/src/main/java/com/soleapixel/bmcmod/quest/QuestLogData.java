package com.soleapixel.bmcmod.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public final class QuestLogData {
    public static final String ROOT = "bmcmod_quest";

    public String questId;
    public QuestKind kind = QuestKind.KILL;
    public String target = "minecraft:zombie";
    public int goal = 10;
    public int progress;
    public QuestDifficulty difficulty = QuestDifficulty.NORMAL;
    public boolean started;
    public long deadlineTick;
    public int durationTicks;
    /** Clé de traduction courte pour titres / messages (racine ; les sous-tâches ont la leur). */
    public String titleKey = "quest.bmcmod.title.generic";
    public int objectiveX;
    public int objectiveZ;

    /** Game time absolu où la cible Bounty Hunter doit apparaître (0 = pas encore planifié). */
    public long bountySpawnGameTime;
    public boolean bountySpawned;
    /** UUID de l’entité {@code bmcmod:bounty_hunter} une fois spawnée. */
    public String bountyTargetUuid = "";

    /** 1 à 3 objectifs ; la source de vérité pour la progression. Les champs {@code kind/target/...} dupliquent la première tâche pour compatibilité. */
    public final List<QuestSubTask> tasks = new ArrayList<>();

    public static final class QuestSubTask {
        public QuestKind kind = QuestKind.KILL;
        public String target = "";
        public int goal;
        public int progress;
        public int objectiveX;
        public int objectiveZ;
        public String titleKey = "quest.bmcmod.title.generic";

        public boolean isComplete() {
            return progress >= goal;
        }

        public ResourceLocation targetRl() {
            return ResourceLocation.parse(target);
        }

        public CompoundTag toNbt() {
            CompoundTag t = new CompoundTag();
            t.putString("kind", kind.id());
            t.putString("target", target);
            t.putInt("goal", goal);
            t.putInt("progress", progress);
            t.putInt("objectiveX", objectiveX);
            t.putInt("objectiveZ", objectiveZ);
            t.putString("titleKey", titleKey.isEmpty() ? "quest.bmcmod.title.generic" : titleKey);
            return t;
        }

        public static QuestSubTask fromNbt(CompoundTag t) {
            QuestSubTask s = new QuestSubTask();
            s.kind = QuestKind.fromId(t.getString("kind"));
            s.target = t.getString("target");
            s.goal = t.getInt("goal");
            s.progress = t.getInt("progress");
            s.objectiveX = t.contains("objectiveX") ? t.getInt("objectiveX") : 0;
            s.objectiveZ = t.contains("objectiveZ") ? t.getInt("objectiveZ") : 0;
            s.titleKey = t.getString("titleKey");
            if (s.titleKey.isEmpty()) {
                s.titleKey = "quest.bmcmod.title.generic";
            }
            return s;
        }
    }

    public static boolean isQuestLog(CompoundTag custom) {
        return custom.contains(ROOT, 10);
    }

    @Nullable
    public static QuestLogData read(CompoundTag custom) {
        if (!custom.contains(ROOT, 10)) {
            return null;
        }
        CompoundTag t = custom.getCompound(ROOT);
        QuestLogData d = new QuestLogData();
        d.questId = t.getString("id");
        d.kind = QuestKind.fromId(t.getString("kind"));
        d.target = t.getString("target");
        d.goal = t.getInt("goal");
        d.progress = t.getInt("progress");
        d.started = t.getBoolean("started");
        d.deadlineTick = t.getLong("deadline");
        d.durationTicks = t.getInt("duration");
        d.titleKey = t.getString("titleKey");
        if (d.titleKey.isEmpty()) {
            d.titleKey = "quest.bmcmod.title.generic";
        }
        d.objectiveX = t.contains("objectiveX") ? t.getInt("objectiveX") : 0;
        d.objectiveZ = t.contains("objectiveZ") ? t.getInt("objectiveZ") : 0;
        d.bountySpawnGameTime = t.contains("bountySpawnGameTime") ? t.getLong("bountySpawnGameTime") : 0L;
        d.bountySpawned = t.getBoolean("bountySpawned");
        d.bountyTargetUuid = t.contains("bountyTargetUuid") ? t.getString("bountyTargetUuid") : "";
        try {
            d.difficulty = QuestDifficulty.valueOf(t.getString("difficulty"));
        } catch (IllegalArgumentException e) {
            d.difficulty = QuestDifficulty.NORMAL;
        }

        d.tasks.clear();
        if (t.contains("tasks", Tag.TAG_LIST)) {
            ListTag list = t.getList("tasks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                d.tasks.add(QuestSubTask.fromNbt(list.getCompound(i)));
            }
        }

        if (d.tasks.isEmpty()) {
            QuestSubTask s = new QuestSubTask();
            s.kind = d.kind;
            s.target = d.target;
            s.goal = d.goal;
            s.progress = d.progress;
            s.objectiveX = d.objectiveX;
            s.objectiveZ = d.objectiveZ;
            s.titleKey = d.titleKey;
            d.tasks.add(s);
        } else {
            d.syncLegacyFromFirst();
        }

        return d;
    }

    public void write(CompoundTag custom) {
        ensureTasks();
        syncLegacyFromFirst();

        CompoundTag t = new CompoundTag();
        t.putString("id", questId);
        t.putString("kind", kind.id());
        t.putString("target", target);
        t.putInt("goal", goal);
        t.putInt("progress", progress);
        t.putBoolean("started", started);
        t.putLong("deadline", deadlineTick);
        t.putInt("duration", durationTicks);
        t.putString("difficulty", difficulty.name());
        t.putString("titleKey", titleKey);
        t.putInt("objectiveX", objectiveX);
        t.putInt("objectiveZ", objectiveZ);
        t.putLong("bountySpawnGameTime", bountySpawnGameTime);
        t.putBoolean("bountySpawned", bountySpawned);
        t.putString("bountyTargetUuid", bountyTargetUuid != null ? bountyTargetUuid : "");

        ListTag list = new ListTag();
        for (QuestSubTask s : tasks) {
            list.add(s.toNbt());
        }
        t.put("tasks", list);

        custom.put(ROOT, t);
    }

    /** Garantit au moins une entrée dans {@link #tasks} (migré depuis les champs plats si besoin). */
    public void ensureTasks() {
        if (!tasks.isEmpty()) {
            return;
        }
        QuestSubTask s = new QuestSubTask();
        s.kind = kind;
        s.target = target;
        s.goal = goal;
        s.progress = progress;
        s.objectiveX = objectiveX;
        s.objectiveZ = objectiveZ;
        s.titleKey = titleKey.isEmpty() ? "quest.bmcmod.title.generic" : titleKey;
        tasks.add(s);
    }

    public void syncLegacyFromFirst() {
        ensureTasks();
        QuestSubTask s0 = tasks.getFirst();
        this.kind = s0.kind;
        this.target = s0.target;
        this.goal = s0.goal;
        this.progress = s0.progress;
        this.objectiveX = s0.objectiveX;
        this.objectiveZ = s0.objectiveZ;
        this.titleKey = tasks.size() > 1 ? "quest.bmcmod.title.multi" : s0.titleKey;
    }

    public boolean isComplete() {
        ensureTasks();
        for (QuestSubTask s : tasks) {
            if (!s.isComplete()) {
                return false;
            }
        }
        return true;
    }

    public boolean isBountyContractActive() {
        return this.difficulty == QuestDifficulty.BOUNTY_HUNTER && this.started && !this.isComplete();
    }

    public int completedSubtaskCount() {
        ensureTasks();
        int n = 0;
        for (QuestSubTask s : tasks) {
            if (s.isComplete()) {
                n++;
            }
        }
        return n;
    }

    public ResourceLocation targetRl() {
        return ResourceLocation.parse(target);
    }

    @Nullable
    public UUID bountyTargetAsUuid() {
        if (bountyTargetUuid == null || bountyTargetUuid.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(bountyTargetUuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
