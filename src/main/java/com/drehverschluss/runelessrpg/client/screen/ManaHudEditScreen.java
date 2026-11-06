package com.drehverschluss.runelessrpg.client.screen;

import com.drehverschluss.runelessrpg.client.config.RunelessClientConfig;
import com.drehverschluss.runelessrpg.client.hud.ManaBar;
import com.drehverschluss.runelessrpg.client.hud.ManaBarStyle;
import com.drehverschluss.runelessrpg.client.hud.TextMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/**
 * Layout editor: drag the mana bar with the mouse, scroll to scale, right click to show / hide, arrow keys to nudge
 * (shift = 10 px). The layout is saved when the screen is closed.
 * <p>
 * Based on {@code HudEditScreen} from Perilscope (same author, MIT), reduced to the single mana bar.
 */
public final class ManaHudEditScreen extends Screen {
    private static final int SNAP_DISTANCE = 4;
    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 20;
    private static final int OPTION_WIDTH = 120;
    private static final int SPACING = 4;
    private static final int BACKGROUND_COLOR = 0x50000000;
    private static final int OUTLINE_COLOR = 0xFFFFFFFF;
    private static final int SELECTED_OUTLINE_COLOR = 0xFFFFFF55;
    private static final int GUIDE_COLOR = 0xA0FFFF55;
    private static final float SCALE_STEP = 0.1F;

    @Nullable
    private final Screen parent;
    private boolean selected;
    private boolean dragging;
    private int grabOffsetX;
    private int grabOffsetY;
    private int guideX = -1;
    private int guideY = -1;
    private int hintY;
    private int anchorY;
    private Button styleButton;
    private Button textButton;
    private Button visibleButton;

    public ManaHudEditScreen(@Nullable Screen parent) {
        super(Component.translatable("runelessrpg.editor.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Controls in the middle of the screen: the bar's usual places (bottom, corners) stay free for dragging.
        int top = height / 2 - 30;
        anchorY = top;
        hintY = top + font.lineHeight + 4;
        int optionsY = hintY + font.lineHeight * 2 + 8;

        styleButton = optionButton(() -> {
            ManaBarStyle[] styles = ManaBarStyle.values();
            RunelessClientConfig.STYLE.set(styles[(RunelessClientConfig.STYLE.get().ordinal() + 1) % styles.length]);
        });
        textButton = optionButton(() -> {
            TextMode[] modes = TextMode.values();
            RunelessClientConfig.TEXT_MODE.set(modes[(RunelessClientConfig.TEXT_MODE.get().ordinal() + 1) % modes.length]);
        });
        visibleButton = optionButton(() -> ManaBar.INSTANCE.setVisible(!ManaBar.INSTANCE.isVisible()));
        int bottomY = layoutOptionButtons(List.of(styleButton, textButton, visibleButton), optionsY);

        addRenderableWidget(Button.builder(Component.translatable("runelessrpg.editor.reset"), button -> {
                    ManaBar.INSTANCE.resetLayout();
                    refreshButtons();
                })
                .bounds(width / 2 - BUTTON_WIDTH - 2, bottomY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 + 2, bottomY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        refreshButtons();
    }

    private Button optionButton(Runnable action) {
        return addRenderableWidget(Button.builder(Component.empty(), button -> {
            action.run();
            refreshButtons();
        }).size(OPTION_WIDTH, BUTTON_HEIGHT).build());
    }

    /** Places the option buttons in centered rows starting at {@code top}; returns the y below the last row. */
    private int layoutOptionButtons(List<Button> buttons, int top) {
        int perRow = Mth.clamp((width - 8) / (OPTION_WIDTH + SPACING), 1, buttons.size());
        int rows = Mth.positiveCeilDiv(buttons.size(), perRow);
        for (int i = 0; i < buttons.size(); i++) {
            int row = i / perRow;
            int inRow = Math.min(perRow, buttons.size() - row * perRow);
            int rowWidth = inRow * OPTION_WIDTH + (inRow - 1) * SPACING;
            buttons.get(i).setPosition((width - rowWidth) / 2 + (i % perRow) * (OPTION_WIDTH + SPACING),
                    top + row * (BUTTON_HEIGHT + SPACING));
        }
        return top + rows * (BUTTON_HEIGHT + SPACING);
    }

    private void refreshButtons() {
        styleButton.setMessage(CommonComponents.optionNameValue(Component.translatable("runelessrpg.editor.option.style"),
                Component.translatable("runelessrpg.style." + RunelessClientConfig.STYLE.get().name().toLowerCase(Locale.ROOT))));
        textButton.setMessage(CommonComponents.optionNameValue(Component.translatable("runelessrpg.editor.option.text"),
                Component.translatable("runelessrpg.text_mode." + RunelessClientConfig.TEXT_MODE.get().name().toLowerCase(Locale.ROOT))));
        visibleButton.setMessage(CommonComponents.optionStatus(Component.translatable("runelessrpg.editor.option.visible"),
                ManaBar.INSTANCE.isVisible()));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // No blur, the bar has to stay readable
        graphics.fill(0, 0, width, height, BACKGROUND_COLOR);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        ManaBar bar = ManaBar.INSTANCE;
        bar.renderPreview(graphics);
        ManaBar.Bounds bounds = bar.getEditorBounds(width, height);
        boolean hovered = bounds.contains(mouseX, mouseY);
        graphics.renderOutline(bounds.x() - 2, bounds.y() - 2, bounds.width() + 4, bounds.height() + 4,
                selected ? SELECTED_OUTLINE_COLOR : hovered ? OUTLINE_COLOR : 0x60FFFFFF);
        if (guideX >= 0) {
            graphics.vLine(guideX, 0, height, GUIDE_COLOR);
        }
        if (guideY >= 0) {
            graphics.hLine(0, width, guideY, GUIDE_COLOR);
        }
        graphics.drawCenteredString(font, Component.translatable("runelessrpg.editor.hint.mouse"), width / 2, hintY, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("runelessrpg.editor.hint.keys"), width / 2,
                hintY + font.lineHeight + 2, 0xFFAAAAAA);
        graphics.drawCenteredString(font, Component.translatable("runelessrpg.editor.anchor",
                Component.translatable("runelessrpg.anchor." + bar.getAnchor().name().toLowerCase(Locale.ROOT))), width / 2, anchorY, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        ManaBar bar = ManaBar.INSTANCE;
        if (!bar.getEditorBounds(width, height).contains(mouseX, mouseY)) {
            selected = false;
            return false;
        }
        selected = true;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            ManaBar.Bounds bounds = bar.getBounds(width, height);
            dragging = true;
            grabOffsetX = (int) mouseX - bounds.x();
            grabOffsetY = (int) mouseY - bounds.y();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            bar.setVisible(!bar.isVisible());
            refreshButtons();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        ManaBar bar = ManaBar.INSTANCE;
        ManaBar.Bounds bounds = bar.getBounds(width, height);
        int x = (int) mouseX - grabOffsetX;
        int y = (int) mouseY - grabOffsetY;
        guideX = -1;
        guideY = -1;
        if (!hasShiftDown()) {
            x = snap(x, bounds.width(), width, true);
            y = snap(y, bounds.height(), height, false);
        }
        bar.moveTo(x, y, width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = false;
            guideX = -1;
            guideY = -1;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ManaBar bar = ManaBar.INSTANCE;
        if (scrollY == 0 || !bar.getEditorBounds(width, height).contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        selected = true;
        float scale = bar.getScale() + (scrollY > 0 ? SCALE_STEP : -SCALE_STEP);
        bar.setScale(Math.round(scale * 10.0F) / 10.0F);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selected) {
            int step = hasShiftDown() ? 10 : 1;
            int dx = 0;
            int dy = 0;
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> dx = -step;
                case GLFW.GLFW_KEY_RIGHT -> dx = step;
                case GLFW.GLFW_KEY_UP -> dy = -step;
                case GLFW.GLFW_KEY_DOWN -> dy = step;
                default -> {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }
            ManaBar bar = ManaBar.INSTANCE;
            ManaBar.Bounds bounds = bar.getBounds(width, height);
            bar.moveTo(bounds.x() + dx, bounds.y() + dy, width, height);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        RunelessClientConfig.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Snaps the position to the screen edges and the screen center and remembers the guide line to draw. */
    private int snap(int position, int size, int screenSize, boolean horizontal) {
        int[] targets = {0, (screenSize - size) / 2, screenSize - size};
        int best = position;
        int bestDistance = SNAP_DISTANCE + 1;
        int bestGuide = -1;
        for (int target : targets) {
            int distance = Math.abs(position - target);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = target;
                bestGuide = target == 0 ? 0 : target == screenSize - size ? screenSize - 1 : screenSize / 2;
            }
        }
        if (bestDistance <= SNAP_DISTANCE) {
            if (horizontal) {
                guideX = bestGuide;
            } else {
                guideY = bestGuide;
            }
            return best;
        }
        return position;
    }
}
