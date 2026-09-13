package thunder.hack.features.hud.impl;

import net.minecraft.client.gl.RenderPipelines;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.client.Media;
import thunder.hack.features.modules.misc.NameProtect;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.TextUtil;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WaterMark extends HudElement {
    public WaterMark() {
        super("WaterMark", 100, 35);
    }

    public static final Setting<Mode> mode = new Setting<>("Mode", Mode.Big);
    private final Setting<Boolean> ru = new Setting<>("RU", false);

    private final TextUtil textUtil = new TextUtil(
            "ТандерХак",
            "ГромХак",
            "ГрозаКлиент",
            "ТандерХуй",
            "ТандерХряк",
            "ТандерХрюк",
            "ТиндерХак",
            "ТундраХак",
            "ГромВзлом"
    );

    private enum Mode {
        Big, Small, Classic, BaltikaClient, Rifk
    }

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        String username = ((ModuleManager.media.isEnabled() && Media.nickProtect.getValue()) || ModuleManager.nameProtect.isEnabled()) ? (ModuleManager.nameProtect.isEnabled() ? NameProtect.getCustomName() : "Protected") : mc.getSession().getUsername();

        if (mode.getValue() == Mode.Big) {
            Render2DEngine.drawHudBase(context, getPosX(), getPosY(), 106, 30, HudEditor.hudRound.getValue());
            FontRenderers.thglitch.drawString(context, "THUNDERHACK", getPosX() + 5.5, getPosY() + 5, -1);
            FontRenderers.monsterrat.drawGradientString(context, "recode", getPosX() + 35.5f, getPosY() + 21f, 1);
            setBounds(getPosX(), getPosY(), 106, 30);
        } else if (mode.getValue() == Mode.Small) {
            if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
                float offset1 = FontRenderers.sf_bold.getStringWidth(username) + 72;
                float offset2 = FontRenderers.sf_bold.getStringWidth((mc.isInSingleplayer() ? "SinglePlayer" : mc.getNetworkHandler().getServerInfo().address));
                float offset3 = (Managers.PROXY.isActive() ? FontRenderers.sf_bold.getStringWidth(Managers.PROXY.getActiveProxy().getName()) + 11 : 0);

                Render2DEngine.drawRoundedBlur(context, getPosX(), getPosY(), 50f, 15f, 3, HudEditor.blurColor.getValue().getColorObject());
                Render2DEngine.drawRoundedBlur(context, getPosX() + 55, getPosY(), offset1 + offset2 - 36 + offset3, 15f, 3, HudEditor.blurColor.getValue().getColorObject());

                Render2DEngine.setupRender();

                Render2DEngine.drawRect(context, getPosX() + 13, getPosY() + 1.5f, 0.5f, 11, new Color(0x44FFFFFF, true));

                FontRenderers.sf_bold.drawGradientString(context, "Recode", getPosX() + 18, getPosY() + 5, 20);

                GlStateManager.glBlendFuncSeparate(770, 1, 1, 0);
                Render2DEngine.bindTexture(TextureStorage.miniLogo);
                Render2DEngine.renderGradientTexture(context, getPosX() + 1, getPosY() + 2, 11, 11, 0, 0, 128, 128, 128, 128,
                        HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));

                Render2DEngine.bindTexture(TextureStorage.playerIcon);
                Render2DEngine.renderGradientTexture(context, getPosX() + 58, getPosY() + 3, 8, 8, 0, 0, 128, 128, 128, 128,
                        HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));

                Render2DEngine.bindTexture(TextureStorage.serverIcon);
                Render2DEngine.renderGradientTexture(context, getPosX() + offset1, getPosY() + 2, 10, 10, 0, 0, 128, 128, 128, 128,
                        HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));

                if (Managers.PROXY.isActive()) {
                    Render2DEngine.bindTexture(TextureStorage.proxyIcon);
                    Render2DEngine.renderGradientTexture(context, getPosX() + offset1 + offset2 + 16, getPosY() + 2, 10, 10, 0, 0, 128, 128, 128, 128,
                            HudEditor.getColor(270), HudEditor.getColor(0), HudEditor.getColor(180), HudEditor.getColor(90));

                    FontRenderers.sf_bold.drawString(context, Managers.PROXY.getActiveProxy().getName(), getPosX() + offset1 + offset2 + 28, getPosY() + 5, -1);
                }

                Render2DEngine.endRender();

                Render2DEngine.setupRender();
                GlStateManager.glBlendFuncSeparate(770, 771, 1, 0);
                FontRenderers.sf_bold.drawString(context, username, getPosX() + 68, getPosY() + 4.5f, HudEditor.textColor.getValue().getColor());
                FontRenderers.sf_bold.drawString(context, (mc.isInSingleplayer() ? "SinglePlayer" : mc.getNetworkHandler().getServerInfo().address), getPosX() + offset1 + 13, getPosY() + 4.5f, HudEditor.textColor.getValue().getColor());
                Render2DEngine.endRender();
                setBounds(getPosX(), getPosY(), 100, 15f);
            } else {
                String info = Formatting.DARK_GRAY + "| " + Formatting.RESET + username + Formatting.DARK_GRAY + " | " + Formatting.RESET + Managers.SERVER.getPing() + " ms" + Formatting.DARK_GRAY + " | " + Formatting.RESET + (mc.isInSingleplayer() ? "SinglePlayer" : mc.getNetworkHandler().getServerInfo().address);
                float width = FontRenderers.sf_bold.getStringWidth("ThunderHack " + info) + 5;
                Render2DEngine.drawHudBase(context, getPosX(), getPosY(), width, 10, 3);
                FontRenderers.sf_bold.drawGradientString(context, ru.getValue() ? textUtil + " " : "ThunderHack ", getPosX() + 2, getPosY() + 2.5f, 10);
                FontRenderers.sf_bold.drawString(context, info, getPosX() + 2 + FontRenderers.sf_bold.getStringWidth("ThunderHack "), getPosY() + 2.5f, HudEditor.textColor.getValue().getColor());
                setBounds(getPosX(), getPosY(), width, 10);
            }

        } else if (mode.getValue() == Mode.BaltikaClient) {
            Render2DEngine.drawHudBase(context, getPosX(), getPosY(), 100, 64, HudEditor.hudRound.getValue());

            Render2DEngine.addWindow(context, getPosX(), getPosY(), getPosX() + 100, getPosY() + 64, 1f);
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(getPosX() + 10, getPosY() + 32, 0);
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.toRadians(mc.player.age * 3 + Render3DEngine.getTickDelta())));
            context.getMatrices().translate(-(getPosX() + 10), -(getPosY() + 32), 0);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TextureStorage.baltika, (int) getPosX() - 10, (int) getPosY() + 2, 0, 0, 40, 64, 40, 64);
            context.getMatrices().popMatrix();
            Render2DEngine.popWindow();

            FontRenderers.thglitch.drawString(context, "BALTIKA", getPosX() + 43, getPosY() + 41.5, -1);
            setBounds(getPosX(), getPosY(), 100, 64);
        } else if (mode.is(Mode.Rifk)) {
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

            // лень вставлять реал билд дату
            // too lazy to insert the real build date

            String info = Formatting.GREEN + String.format("th7 | build: 16/06/2024 | rate: %d | %s", Math.round(Managers.SERVER.getTPS()), format.format(date));
            float width = FontRenderers.profont.getStringWidth(info) + 5;
            Render2DEngine.drawRectWithOutline(context, getPosX(), getPosY(), width, 8, Color.decode("#192A1A"), Color.decode("#833B7B"));
            Render2DEngine.drawGradientBlurredShadow1(context, getPosX(), getPosY(), width, 8, 10, Color.decode("#161A1E"), Color.decode("#161A1E"), Color.decode("#382E37"), Color.decode("#382E37"));
            FontRenderers.profont.drawString(context, info, getPosX() + 2.7, getPosY() + 2.953, HudEditor.textColor.getValue().getColor());
            setBounds(getPosX(), getPosY(), width, 8);
        } else {
            FontRenderers.monsterrat.drawGradientString(context, "ThunderHack v" + ThunderHack.VERSION, getPosX() + 5.5f, getPosY() + 5, 10);
            setBounds(getPosX(), getPosY(), 100, 3);
        }
    }

    @Override
    public void onUpdate() {
        textUtil.tick();
    }
}