package com.soleapixel.bmcmod.item;

import net.minecraft.world.item.BowItem;

/** Arc upgradable : bonus gérés par {@link com.soleapixel.bmcmod.gameplay.UpgradeBowEvents}. */
public final class UpgradeBowItem extends BowItem {
    private final float drawScale;
    private final float velocityScale;
    private final float damageScale;

    public UpgradeBowItem(Properties properties, float drawScale, float velocityScale, float damageScale) {
        super(properties);
        this.drawScale = drawScale;
        this.velocityScale = velocityScale;
        this.damageScale = damageScale;
    }

    public float drawScale() {
        return drawScale;
    }

    public float velocityScale() {
        return velocityScale;
    }

    public float damageScale() {
        return damageScale;
    }
}
