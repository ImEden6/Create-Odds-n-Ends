package com.mervyn.createoddsnends.organ;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

public enum MatchMode implements INamedIconOptions {
    EXACT(AllIcons.I_CONFIRM),
    OCTAVE(AllIcons.I_REFRESH);

    private final AllIcons icon;

    MatchMode(AllIcons icon) {
        this.icon = icon;
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return "createoddsnends.organ.match." + name().toLowerCase();
    }
}
