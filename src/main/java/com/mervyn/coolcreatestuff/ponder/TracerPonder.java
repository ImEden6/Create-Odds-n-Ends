package com.mervyn.coolcreatestuff.ponder;

import com.mervyn.coolcreatestuff.Tracer;
import com.mervyn.coolcreatestuff.jacquard.Jacquard;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class TracerPonder implements PonderPlugin {
    private static final ResourceLocation TAG = ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "cool_stuffs");

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
        helper.forComponents(Jacquard.JACQUARD_HEAD.getId(), Jacquard.LOOM_FRAME.getId(), Jacquard.LOOM_DRIVE.getId())
                .addStoryBoard("jacquard/loom", JacquardScenes::build)
                .addStoryBoard("jacquard/loom", JacquardScenes::weaving);
        helper.forComponents(Jacquard.TAPESTRY.getId())
                .addStoryBoard("jacquard/loom", JacquardScenes::weaving);
        helper.forComponents(Jacquard.CARD_PUNCH.getId(), Jacquard.BLANK_CARD.getId(), Jacquard.PUNCHED_CARD_CHAIN.getId())
                .addStoryBoard("jacquard/card_punch", JacquardScenes::punch);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(TAG)
                .addToIndex()
                .item(Tracer.LATHE_HEADSTOCK.get(), true, false)
                .title("Create: Cool Stuffs")
                .description("The copy lathe, the jacquard loom and the tools that go with them")
                .register();
        helper.addToTag(TAG)
                .add(Tracer.LATHE_HEADSTOCK.getId())
                .add(Tracer.LATHE_BED.getId())
                .add(Tracer.LATHE_TAILSTOCK.getId())
                .add(Tracer.TRACER_RAIL.getId())
                .add(Tracer.TRACER_HOLDER.getId())
                .add(Tracer.TURNED_BLOCK.getId())
                .add(Tracer.TEMPLATE_TABLE.getId())
                .add(Tracer.PROFILE_TEMPLATE.getId())
                .add(Jacquard.JACQUARD_HEAD.getId())
                .add(Jacquard.LOOM_FRAME.getId())
                .add(Jacquard.LOOM_DRIVE.getId())
                .add(Jacquard.CARD_PUNCH.getId());
        helper.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES)
                .add(Tracer.LATHE_HEADSTOCK.getId())
                .add(Jacquard.LOOM_DRIVE.getId())
                .add(Jacquard.CARD_PUNCH.getId());
        helper.addToTag(AllCreatePonderTags.ARM_TARGETS)
                .add(Tracer.LATHE_HEADSTOCK.getId())
                .add(Tracer.LATHE_BED.getId())
                .add(Tracer.LATHE_TAILSTOCK.getId())
                .add(Tracer.TRACER_RAIL.getId())
                .add(Tracer.TRACER_HOLDER.getId())
                .add(Jacquard.JACQUARD_HEAD.getId())
                .add(Jacquard.LOOM_FRAME.getId())
                .add(Jacquard.LOOM_DRIVE.getId())
                .add(Jacquard.CARD_PUNCH.getId());
    }
}
