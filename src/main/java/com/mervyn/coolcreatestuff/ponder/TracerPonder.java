package com.mervyn.coolcreatestuff.ponder;

import com.mervyn.coolcreatestuff.Tracer;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class TracerPonder implements PonderPlugin {
    @Override
    public String getModId() {
        return Tracer.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(Tracer.LATHE_HEADSTOCK.getId(), Tracer.LATHE_BED.getId(), Tracer.LATHE_TAILSTOCK.getId())
                .addStoryBoard("lathe/lathe", LatheScenes::build)
                .addStoryBoard("lathe/lathe", LatheScenes::cutting);
        helper.forComponents(Tracer.TRACER_HOLDER.getId(), Tracer.TRACER_RAIL.getId(), Tracer.TURNED_BLOCK.getId())
                .addStoryBoard("lathe/tracer", LatheScenes::tracer);
        helper.forComponents(Tracer.TEMPLATE_TABLE.getId(), Tracer.PROFILE_TEMPLATE.getId())
                .addStoryBoard("lathe/template_table", LatheScenes::templateTable);
    }
}
