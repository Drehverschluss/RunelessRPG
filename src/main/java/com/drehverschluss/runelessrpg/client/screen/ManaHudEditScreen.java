package com.drehverschluss.runelessrpg.client.screen;

import com.drehverschluss.runelessrpg.client.config.RunelessClientConfig;
import com.drehverschluss.runelessrpg.client.hud.ManaBar;
import com.drehverschluss.runelessrpg.client.hud.ManaBarStyle;
import com.drehverschluss.runelessrpg.client.hud.ManaText;
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
 * Layout editor for the two HUD elements, the mana display and the mana number ("Mana Text"): drag to move, scroll to
 * scale (the number in sharp steps), right click to show / hide, arrow keys to nudge (shift = 10 px). Dragging the
 * display moves an attached number along; Ctrl while releasing the number (or the Detach/Attach button) detaches or
 * attaches it. The layout is saved when the screen is closed.
 * <p>
 * Based on {@code HudEditScreen} from Perilscope (same author, MIT).
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
    private static final int TEXT_OUTLINE_COLOR = 0xFF55FFFF;
    private static final int IDLE_OUTLINE_COLOR = 0x60FFFFFF;
    private static final int GUIDE_COLOR = 0xA0FFFF55;
    private static final float SCALE_STEP = 0.1F;

    /** The two elements the editor works on. */
    public enum Element { BAR, TEXT }

    @Nullable
    private final Screen parent;
    @Nullable
    private Element selected;
    @Nullable
    private Element dragging;
    private double grabOffsetX;
    private double grabOffsetY;
    private int guideX = -1;
    private int guideY = -1;
    private int hintY;
    private int anchorY;
    private Button styleButton;
    private Button textButton;
    private Button visibleButton;
    private Button attachButton;

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
        attachButton = optionButton(this::toggleAttached);
        int bottomY = layoutOptionButtons(List.of(styleButton, textButton, visibleButton, attachButton), optionsY);

        addRenderableWidget(Button.builder(Component.translatable("runelessrpg.editor.reset"), button -> {
                    ManaBar.INSTANCE.resetLayout();
                    ManaText.INSTANCE.resetLayout();
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
        attachButton.setMessage(Component.translatable(ManaText.INSTANCE.isAttached()
                ? "runelessrpg.editor.text.detach" : "runelessrpg.editor.text.attach"));
    }

    /** Detaches the number from the display (nearest screen anchor) or attaches it (offset to the display). */
    public void toggleAttached() {
        ManaText.INSTANCE.setAttached(!ManaText.INSTANCE.isAttached(), width, height);
        selected = Element.TEXT;
        refreshButtons();
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
        Element hovered = elementAt(mouseX, mouseY);
        ManaBar.Bounds barBounds = bar.getEditorBounds(width, height);
        frame(graphics, barBounds, Element.BAR, hovered, OUTLINE_COLOR, Component.translatable("runelessrpg.editor.element.bar"));
        ManaText.Layout text = ManaText.INSTANCE.currentLayout(width, height);
        if (text != null) {
            frame(graphics, text.bounds(), Element.TEXT, hovered, TEXT_OUTLINE_COLOR, Component.translatable("runelessrpg.editor.element.text"));
        }
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

    /** Outline of an element plus its name above it (small). */
    private void frame(GuiGraphics graphics, ManaBar.Bounds bounds, Element element, @Nullable Element hovered, int color, Component name) {
        int outline = selected == element ? SELECTED_OUTLINE_COLOR : hovered == element ? color : IDLE_OUTLINE_COLOR;
        graphics.renderOutline(bounds.x() - 2, bounds.y() - 2, bounds.width() + 4, bounds.height() + 4, outline);
        graphics.pose().pushPose();
        graphics.pose().translate(bounds.x() - 2, bounds.y() - 2 - font.lineHeight * 0.5F - 1, 0.0F);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(font, name, 0, 0, outline, true);
        graphics.pose().popPose();
    }

    /** The element under the mouse; the number wins, it is drawn on top. */
    @Nullable
    private Element elementAt(double mouseX, double mouseY) {
        ManaText.Layout text = ManaText.INSTANCE.currentLayout(width, height);
        if (text != null && text.bounds().contains(mouseX, mouseY)) {
            return Element.TEXT;
        }
        return ManaBar.INSTANCE.getEditorBounds(width, height).contains(mouseX, mouseY) ? Element.BAR : null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        Element element = elementAt(mouseX, mouseY);
        selected = element;
        if (element == null) {
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = element;
            if (element == Element.TEXT) {
                ManaText.INSTANCE.beginEdit(width, height);
                ManaText.Layout text = ManaText.INSTANCE.currentLayout(width, height);
                grabOffsetX = text != null ? mouseX - text.x() : 0;
                grabOffsetY = text != null ? mouseY - text.y() : 0;
            } else {
                ManaBar.Bounds bounds = ManaBar.INSTANCE.getBounds(width, height);
                grabOffsetX = mouseX - bounds.x();
                grabOffsetY = mouseY - bounds.y();
            }
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (element == Element.TEXT) {
                ManaText.INSTANCE.setVisible(!ManaText.INSTANCE.isVisible());
            } else {
                ManaBar.INSTANCE.setVisible(!ManaBar.INSTANCE.isVisible());
            }
            refreshButtons();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging == null || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        double x = mouseX - grabOffsetX;
        double y = mouseY - grabOffsetY;
        if (dragging == Element.TEXT) {
            ManaText.INSTANCE.moveTo(x, y, width, height);
            return true;
        }
        ManaBar bar = ManaBar.INSTANCE;
        ManaBar.Bounds bounds = bar.getBounds(width, height);
        int barX = (int) Math.round(x);
        int barY = (int) Math.round(y);
        guideX = -1;
        guideY = -1;
        if (!hasShiftDown()) {
            barX = snap(barX, bounds.width(), width, true);
            barY = snap(barY, bounds.height(), height, false);
        }
        bar.moveTo(barX, barY, width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (dragging == Element.TEXT && hasControlDown()) {
                toggleAttached();
            }
            dragging = null;
            guideX = -1;
            guideY = -1;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Element element = elementAt(mouseX, mouseY);
        if (scrollY == 0 || element == null) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        selected = element;
        if (element == Element.TEXT) {
            ManaText.INSTANCE.scaleStep(scrollY > 0 ? 1 : -1, width, height);
        } else {
            ManaBar bar = ManaBar.INSTANCE;
            float scale = bar.getScale() + (scrollY > 0 ? SCALE_STEP : -SCALE_STEP);
            bar.setScale(Math.round(scale * 10.0F) / 10.0F);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selected != null) {
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
            if (selected == Element.TEXT) {
                ManaText text = ManaText.INSTANCE;
                text.beginEdit(width, height);
                ManaText.Layout layout = text.currentLayout(width, height);
                if (layout != null) {
                    text.moveTo(layout.x() + dx, layout.y() + dy, width, height);
                }
            } else {
                ManaBar bar = ManaBar.INSTANCE;
                ManaBar.Bounds bounds = bar.getBounds(width, height);
                bar.moveTo(bounds.x() + dx, bounds.y() + dy, width, height);
            }
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
