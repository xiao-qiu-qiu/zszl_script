package com.zszl.zszlScriptMod.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Mouse;
import com.zszl.zszlScriptMod.gui.components.GuiTheme.UiState;

public class ThemedButton extends GuiButton {

    public ThemedButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }

        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width
                && mouseY < this.y + this.height;
        boolean pressed = this.hovered && Mouse.isButtonDown(0);

        UiState state;
        if (!this.enabled) {
            state = UiState.DISABLED;
        } else if (pressed) {
            state = UiState.PRESSED;
        } else if (this.hovered) {
            state = UiState.HOVER;
        } else {
            state = UiState.NORMAL;
        }

        GuiTheme.drawButtonFrame(this.x, this.y, this.width, this.height, state);
        int textColor = GuiTheme.getStateTextColor(state);
        if (state == UiState.NORMAL && this.enabled) {
            textColor = GuiTheme.LABEL_TEXT;
        }
        textColor = GuiTheme.resolveTextColor(this.displayString, textColor);
        String text = this.displayString == null ? "" : this.displayString;
        String plainText = TextFormatting.getTextWithoutFormattingCodes(text);
        int textWidth = mc.fontRenderer.getStringWidth(plainText == null ? text : plainText);
        float widthScale = textWidth <= 0 ? 1.0F : Math.min(1.0F, (this.width - 8.0F) / textWidth);
        float heightScale = Math.min(1.0F, (this.height - 4.0F) / Math.max(1.0F, mc.fontRenderer.FONT_HEIGHT));
        float scale = Math.max(0.55F, Math.min(widthScale, heightScale));

        if (scale >= 0.995F) {
            drawCenteredString(mc.fontRenderer, text, this.x + this.width / 2,
                    this.y + (this.height - mc.fontRenderer.FONT_HEIGHT) / 2, textColor);
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(this.x + this.width / 2.0F, this.y + this.height / 2.0F, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        drawCenteredString(mc.fontRenderer, text, 0, -mc.fontRenderer.FONT_HEIGHT / 2, textColor);
        GlStateManager.popMatrix();
    }
}
