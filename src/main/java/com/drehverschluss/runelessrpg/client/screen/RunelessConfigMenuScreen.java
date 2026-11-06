package com.drehverschluss.runelessrpg.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * Entry screen of the mod's config button: the drag and drop layout editor or the plain config values.
 * Based on {@code PerilscopeConfigMenuScreen} (same author, MIT).
 */
public final class RunelessConfigMenuScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 4;

    private final ModContainer container;
    private final Screen parent;

    public RunelessConfigMenuScreen(ModContainer container, Screen parent) {
        super(Component.translatable("runelessrpg.configuration.title"));
        this.container = container;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (width - BUTTON_WIDTH) / 2;
        int y = height / 2 - BUTTON_HEIGHT - SPACING;
        addRenderableWidget(Button.builder(Component.translatable("runelessrpg.menu.edit_layout"),
                        button -> minecraft.setScreen(new ManaHudEditScreen(this)))
                .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("runelessrpg.menu.edit_values"),
                        button -> minecraft.setScreen(new ConfigurationScreen(container, this)))
                .bounds(x, y + BUTTON_HEIGHT + SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(x, y + (BUTTON_HEIGHT + SPACING) * 3, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 60, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
