package com.soleapixel.bmcmod.quest;

public enum QuestKind {
    KILL("kill"),
    COLLECT("collect"),
    MINE("mine"),
    VILLAGER_TRADE("trade"),
    FISH("fish"),
    CRAFT("craft"),
    EXPLORE("explore"),
    TREASURE("treasure");

    private final String id;

    QuestKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static QuestKind fromId(String s) {
        for (QuestKind k : values()) {
            if (k.id.equals(s)) {
                return k;
            }
        }
        return KILL;
    }
}
