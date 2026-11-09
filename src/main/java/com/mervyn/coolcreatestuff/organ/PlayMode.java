package com.mervyn.coolcreatestuff.organ;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

public enum PlayMode implements INamedIconOptions {
    ONCE(AllIcons.I_PLAY),
    LOOP(AllIcons.I_REFRESH);

    private final AllIcons icon;

    PlayMode(AllIcons icon) {
        this.icon = icon;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return "createcoolstuffs.organ.mode." + name().toLowerCase();
    }
}
