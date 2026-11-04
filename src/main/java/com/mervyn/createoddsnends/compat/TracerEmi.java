package com.mervyn.createoddsnends.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mervyn.createoddsnends.Profile;
import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.client.AnimatedCnc;
import com.mervyn.createoddsnends.client.AnimatedLathe;
import com.mervyn.createoddsnends.cnc.Cnc;
import com.mervyn.createoddsnends.cnc.CncJob;
import com.mervyn.createoddsnends.jacquard.Jacquard;
import com.mervyn.createoddsnends.jacquard.client.AnimatedLoom;
import com.mervyn.createoddsnends.jacquard.client.AnimatedPunch;
import com.mervyn.createoddsnends.organ.Organ;
import com.mervyn.createoddsnends.turned.TurnedBlockItem;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// The same four pages as the JEI plugin, drawn with EMI's own widgets so they show up without JEI installed.
// With both installed EMI uses this one and leaves the JEI plugin alone. EMI has no Create pages of its own,
// so the CNC page lists the deploying and sequenced assembly jobs it borrows as well.
@EmiEntrypoint
public class TracerEmi implements EmiPlugin {
    private static final EmiRecipeCategory TURNING = category("turning", Tracer.LATHE_HEADSTOCK_ITEM.get());
    private static final EmiRecipeCategory PUNCHING = category("punching", Jacquard.CARD_PUNCH_ITEM.get());
    private static final EmiRecipeCategory WEAVING = category("weaving", Jacquard.JACQUARD_HEAD_ITEM.get());
    private static final EmiRecipeCategory CNC = category("cnc", Cnc.CNC_PENDANT_ITEM.get());

    private static EmiRecipeCategory category(String name, Item icon) {
        return new EmiRecipeCategory(id(name), EmiStack.of(icon)) {
            @Override
            public Component getName() {
                return Component.translatable("createoddsnends.jei." + name);
            }
        };
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Tracer.MODID, path);
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(TURNING);
        registry.addCategory(PUNCHING);
        registry.addCategory(WEAVING);
        registry.addCategory(CNC);
        for (Item item : List.of(Tracer.LATHE_HEADSTOCK_ITEM.get(), Tracer.LATHE_BED_ITEM.get(), Tracer.LATHE_TAILSTOCK_ITEM.get()))
            registry.addWorkstation(TURNING, EmiStack.of(item));
        registry.addWorkstation(PUNCHING, EmiStack.of(Jacquard.CARD_PUNCH_ITEM.get()));
        for (Item item : List.of(Jacquard.JACQUARD_HEAD_ITEM.get(), Jacquard.LOOM_FRAME_ITEM.get(), Jacquard.LOOM_DRIVE_ITEM.get()))
            registry.addWorkstation(WEAVING, EmiStack.of(item));
        for (Item item : List.of(Cnc.CNC_PENDANT_ITEM.get(), Cnc.CNC_FRAME_ITEM.get(), Cnc.CNC_DRIVE_ITEM.get()))
            registry.addWorkstation(CNC, EmiStack.of(item));

        turning(registry, "template", Examples.template(), Profile.BALUSTER, "createoddsnends.jei.template");
        turning(registry, "master", Examples.master(), Profile.VASE, "createoddsnends.jei.master");

        ItemStack chain = Examples.chain(), tapestry = Examples.tapestry(), book = Examples.book();
        int rows = Examples.DESIGN.pixelHeight();
        punching(registry, "editor", List.of(), Examples.blanks(rows), "createoddsnends.jei.blanks_design", chain, "createoddsnends.jei.punch_editor");
        punching(registry, "copy", List.of(chain, tapestry), Examples.blanks(rows), "createoddsnends.jei.blanks_design", chain, "createoddsnends.jei.punch_copy");
        punching(registry, "score", List.of(), Examples.blanks(Examples.SCORE_PAGES), "createoddsnends.jei.blanks_score", book, "createoddsnends.jei.punch_score");
        punching(registry, "copy_score", List.of(book), Examples.blanks(Examples.SCORE_PAGES), "createoddsnends.jei.blanks_score", book, "createoddsnends.jei.punch_copy_score");

        weaving(registry, chain, Examples.wool(Examples.DESIGN), tapestry);

        for (CncJob job : CncJob.all(Minecraft.getInstance().level))
            cnc(registry, job);

        info(registry, Tracer.TEMPLATE_TABLE_ITEM.get(), "createoddsnends.jei.template_table");
        info(registry, Cnc.CNC_FRAME_ITEM.get(), "createoddsnends.jei.info.cnc");
        info(registry, Organ.BARREL_ORGAN_ITEM.get(), "createoddsnends.jei.info.barrel_organ");
        info(registry, Organ.WIND_CHEST_ITEM.get(), "createoddsnends.jei.info.wind_chest");
        info(registry, Organ.WIND_LINE_ITEM.get(), "createoddsnends.jei.info.wind_line");
        info(registry, Jacquard.CARD_PUNCH_ITEM.get(), "createoddsnends.jei.info.card_punch");
    }

    private static void info(EmiRegistry registry, Item item, String key) {
        registry.addRecipe(new EmiInfoRecipe(List.of(EmiStack.of(item)), List.of(Component.translatable(key)),
                id("/info/" + BuiltInRegistries.ITEM.getKey(item).getPath())));
    }

    private static Component hint(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.GRAY);
    }

    private static void turning(EmiRegistry registry, String name, ItemStack pattern, Profile profile, String hint) {
        EmiIngredient blocks = EmiIngredient.of(Examples.BLOCKS.stream().map(block -> (EmiIngredient) EmiStack.of(block)).toList());
        EmiIngredient turned = EmiIngredient.of(Examples.BLOCKS.stream().map(block -> (EmiIngredient) EmiStack.of(TurnedBlockItem.of(profile, block))).toList());
        AnimatedLathe lathe = new AnimatedLathe(profile, Examples.BLOCKS);
        registry.addRecipe(new Page(TURNING, id("/turning/" + name), 150, 62, List.of(EmiStack.of(pattern), blocks), List.of(), widgets -> {
            widgets.addSlot(EmiStack.of(pattern), 4, 11).appendTooltip(hint(hint));
            widgets.addSlot(blocks, 4, 33).appendTooltip(hint("createoddsnends.jei.any_block"));
            widgets.addDrawable(40, 4, 60, 50, (graphics, mouseX, mouseY, delta) -> {
                AllGuiTextures.JEI_SHADOW.render(graphics, 5, 44);
                lathe.draw(graphics, 0, 0);
            });
            widgets.addSlot(turned, 127, 22);
        }, turned));
    }

    private static void punching(EmiRegistry registry, String name, List<ItemStack> copyFrom, ItemStack blanks, String blanksHint, ItemStack output, String hint) {
        AnimatedPunch punch = new AnimatedPunch();
        List<EmiIngredient> inputs = new ArrayList<>();
        EmiIngredient copy = EmiIngredient.of(copyFrom.stream().map(stack -> (EmiIngredient) EmiStack.of(stack)).toList());
        if (!copyFrom.isEmpty())
            inputs.add(copy);
        inputs.add(EmiStack.of(blanks));
        registry.addRecipe(new Page(PUNCHING, id("/punching/" + name), 150, 60, inputs, List.of(), widgets -> {
            if (!copyFrom.isEmpty())
                widgets.addSlot(copy, 4, 10).appendTooltip(hint("createoddsnends.jei.copy_slot"));
            widgets.addSlot(EmiStack.of(blanks), 4, 32).appendTooltip(hint(blanksHint));
            widgets.addDrawable(40, 4, 50, 50, (graphics, mouseX, mouseY, delta) -> {
                AllGuiTextures.JEI_SHADOW.render(graphics, 0, 42);
                punch.draw(graphics, 0, 0);
            });
            widgets.addSlot(EmiStack.of(output), 127, 21).appendTooltip(hint(hint));
        }, EmiStack.of(output)));
    }

    private static void weaving(EmiRegistry registry, ItemStack chain, List<ItemStack> wool, ItemStack tapestry) {
        int perRow = 5, height = Math.max(62, 26 + 19 * ((wool.size() + perRow - 1) / perRow));
        AnimatedLoom loom = new AnimatedLoom();
        List<EmiIngredient> inputs = new ArrayList<>();
        inputs.add(EmiStack.of(chain).setRemainder(EmiStack.of(chain)));
        wool.forEach(stack -> inputs.add(EmiStack.of(stack)));
        registry.addRecipe(new Page(WEAVING, id("/weaving/sample"), 180, height, inputs, List.of(), widgets -> {
            widgets.addSlot(inputs.get(0), 4, 4).appendTooltip(hint("createoddsnends.jei.chain_stays"));
            for (int i = 0; i < wool.size(); i++)
                widgets.addSlot(inputs.get(i + 1), 4 + 19 * (i % perRow), 25 + 19 * (i / perRow)).appendTooltip(hint("createoddsnends.jei.thread"));
            widgets.addDrawable(98, height / 2 - 27, 60, 50, (graphics, mouseX, mouseY, delta) -> {
                AllGuiTextures.JEI_SHADOW.render(graphics, 4, 41);
                loom.draw(graphics, 0, 0);
            });
            widgets.addSlot(EmiStack.of(tapestry), 157, height / 2 - 10).appendTooltip(hint("createoddsnends.jei.tapestry"));
        }, EmiStack.of(tapestry)));
    }

    private static void cnc(EmiRegistry registry, CncJob job) {
        List<EmiIngredient> needs = job.needs().stream().filter(need -> !need.isEmpty()).map(EmiIngredient::of).toList();
        List<EmiIngredient> tools = job.tools().stream().filter(tool -> !tool.isEmpty()).map(EmiIngredient::of).toList();
        List<EmiStack> results = new ArrayList<>();
        for (ProcessingOutput out : job.shown())
            results.add(EmiStack.of(out.getStack()).setChance(out.getChance()));
        // a sequenced assembly's scrap pool goes in one cycling slot after the likeliest result
        List<EmiIngredient> slots = new ArrayList<>();
        if (job.weighted() && results.size() > 1) {
            slots.add(results.getFirst());
            slots.add(EmiIngredient.of(results.subList(1, results.size())));
        } else
            slots.addAll(results);
        ItemStack[] first = job.needs().isEmpty() ? new ItemStack[0] : job.needs().get(0).getItems();
        AnimatedCnc mill = new AnimatedCnc(first.length == 0 ? ItemStack.EMPTY : first[0]);
        ResourceLocation source = job.id();
        registry.addRecipe(new Page(CNC, id("/cnc/" + source.getNamespace() + "/" + source.getPath()), 180, 66, needs, tools, widgets -> {
            int shown = 0;
            for (EmiIngredient need : needs) {
                if (shown == 9)
                    break;
                widgets.addSlot(need, 3 + shown % 3 * 19, 4 + shown / 3 * 19);
                shown++;
            }
            for (EmiIngredient tool : tools) {
                if (shown == 9)
                    break;
                widgets.addSlot(tool, 3 + shown % 3 * 19, 4 + shown / 3 * 19).catalyst(true)
                        .appendTooltip(Component.translatable("createoddsnends.jei.cnc_tool").withStyle(ChatFormatting.GOLD));
                shown++;
            }
            widgets.addDrawable(60, 2, 80, 60, (graphics, mouseX, mouseY, delta) -> {
                AllGuiTextures.JEI_SHADOW.render(graphics, 15, 48);
                mill.draw(graphics, 0, 0);
            });
            for (int i = 0; i < slots.size(); i++)
                widgets.addSlot(slots.get(i), 142 + i % 2 * 19, 14 + i / 2 * 19);
        }, results.toArray(EmiStack[]::new)));
    }

    private static class Page extends BasicEmiRecipe {
        private final Consumer<WidgetHolder> layout;

        Page(EmiRecipeCategory category, ResourceLocation id, int width, int height, List<EmiIngredient> inputs, List<EmiIngredient> catalysts,
             Consumer<WidgetHolder> layout, EmiStack... outputs) {
            super(category, id, width, height);
            this.inputs.addAll(inputs);
            this.catalysts.addAll(catalysts);
            this.outputs.addAll(List.of(outputs));
            this.layout = layout;
        }

        Page(EmiRecipeCategory category, ResourceLocation id, int width, int height, List<EmiIngredient> inputs, List<EmiIngredient> catalysts,
             Consumer<WidgetHolder> layout, EmiIngredient output) {
            this(category, id, width, height, inputs, catalysts, layout, output.getEmiStacks().toArray(EmiStack[]::new));
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            layout.accept(widgets);
        }
    }
}
