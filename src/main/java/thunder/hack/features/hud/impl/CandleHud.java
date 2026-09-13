package thunder.hack.features.hud.impl;

import net.minecraft.client.gl.RenderPipelines;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import thunder.hack.features.hud.HudElement;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;
import thunder.hack.utility.render.animation.AnimationUtility;

import java.awt.*;

public class CandleHud extends HudElement {
    public CandleHud() {
        super("Candle", 10, 100);
    }

    private Setting<Integer> scale = new Setting<>("Scale", 25, 15, 100);
    private Setting<For> mode = new Setting<>("For", For.Win);

    private float xAnim, yAnim, prevPitch;

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        float yDelta = mc.player.lastHeadYaw - mc.player.getHeadYaw();
        if (yDelta > 0) xAnim = AnimationUtility.fast(xAnim, -15, 10);
        else if (yDelta < 0) xAnim = AnimationUtility.fast(xAnim, 35, 10);
        else xAnim = AnimationUtility.fast(xAnim, 10, 10);

        float pDelta = prevPitch - mc.player.getPitch();
        if (pDelta > 0) yAnim = AnimationUtility.fast(yAnim, -15, 10);
        else if (pDelta < 0) yAnim = AnimationUtility.fast(yAnim, 35, 10);
        else yAnim = AnimationUtility.fast(yAnim, 10, 10);

        prevPitch = mc.player.getPitch();

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((int) getPosX(), (int) getPosY());
        float scalefactor = (float) scale.getValue() / 100f;
        context.getMatrices().scale(scalefactor, scalefactor);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TextureStorage.candle, 0, -5, 0, 0, 102, 529, 102, 529);
        context.getMatrices().popMatrix();

        drawFire(context, getPosX() + (40 + 15f - xAnim) * scalefactor, getPosY() + (10 - yAnim) * scalefactor, 7 * scalefactor, 7 * scalefactor,
                Render2DEngine.applyOpacity(new Color(0xFA460F), (float) Math.sin((mc.player.age + 10) / 15f) + 1.4f));
        drawFire(context, getPosX() + (40 + 5.5f - xAnim / 2f) * scalefactor, getPosY() + (15 - yAnim / 2f) * scalefactor, 15 * scalefactor, 15 * scalefactor,
                Render2DEngine.applyOpacity(new Color(0xFF8F1E), (float) Math.sin((mc.player.age + 20) / 15f) + 1.5f));
        drawFire(context, getPosX() + (40 + 1.5f - xAnim / 3f) * scalefactor, getPosY() + (27 - yAnim / 3f) * scalefactor, 20 * scalefactor, 20 * scalefactor,
                Render2DEngine.applyOpacity(new Color(0xFCC352), (float) Math.sin((mc.player.age + 30) / 15f) + 1.6f));
        drawFire(context, getPosX() + (40 - xAnim / 4f) * scalefactor, getPosY() + (45 - yAnim / 4f) * scalefactor, 25 * scalefactor, 25 * scalefactor,
                Render2DEngine.applyOpacity(new Color(0xFCD087), (float) Math.sin((mc.player.age + 40) / 15f) + 1.7f));
        drawFire(context, getPosX() + (40 + 4f - xAnim / 4f) * scalefactor, getPosY() + (43 - yAnim / 4f) * scalefactor, 15 * scalefactor, 15 * scalefactor,
                Render2DEngine.applyOpacity(new Color(0x6690F6), (float) Math.sin((mc.player.age + 40) / 15f) + 1.8f));

        setBounds(getPosX(), getPosY(), (int) (102 * scalefactor), (int) (529 * scalefactor));
    }

    private void drawFire(DrawContext context, float x, float y, float width, float height, Color color) {
        width = width + 7 * 2;
        height = height + 7 * 2;
        x = x - 7;
        y = y - 7;

        GlStateManager._enableBlend();
        GlStateManager.glBlendFuncSeparate(770, 1, 1, 0);
        Render2DEngine.bindTexture(TextureStorage.firefly);
        Render2DEngine.renderGradientTexture(context, x, y, width, height, 0, 0, width, height, width, height, color, color, color, color);
        GlStateManager._disableBlend();
    }

    private enum For {
        Luck, Win, LowPing, AntiKick
    }
}
