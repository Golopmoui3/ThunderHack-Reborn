package thunder.hack.gui.windows;

import net.minecraft.client.gl.RenderPipelines;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import thunder.hack.gui.clickui.ClickGUI;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static thunder.hack.core.manager.IManager.mc;

public class WindowsScreen extends Screen {
    private List<WindowBase> windows = new ArrayList<>();
    public static WindowBase lastClickedWindow;
    public static WindowBase draggingWindow;
    private static final Identifier clickGuiIcon = Identifier.of("thunderhack", "textures/gui/elements/clickgui.png");

    public WindowsScreen(WindowBase... windows) {
        super(Text.of("THWindows"));
        this.windows.clear();
        lastClickedWindow = null;
        this.windows = Arrays.stream(windows).toList();
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        //   super.render(context, mouseX, mouseY, delta);

        int i = mc.getWindow().getScaledWidth() / 2;

        float offset = (windows.size() * 20f) / -2f - 23;

        Render2DEngine.drawHudBase(context, i + offset - 1.5f, mc.getWindow().getScaledHeight() - 25, windows.size() * 20f + 23f, 19, HudEditor.hudRound.getValue());

        GlStateManager._enableBlend();
        GlStateManager.glBlendFuncSeparate(770, 771, 1, 0);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, clickGuiIcon, (int) (i + offset) + 1, mc.getWindow().getScaledHeight() - 23, 15, 15, 0, 0, 15, 15, 15, 15);
        GlStateManager._disableBlend();

        Render2DEngine.drawLine(context, i + offset + 20, mc.getWindow().getScaledHeight() - 23, i + offset + 20, mc.getWindow().getScaledHeight() - 9, Color.GRAY.getRGB());

        offset += 23;
        for (WindowBase w : windows) {
            Color c = Render2DEngine.isHovered(mouseX, mouseY, i + offset, mc.getWindow().getScaledHeight() - 24, 17, 17) ? new Color(0x7C2F2F2F, true) :
                    !w.isVisible() ? new Color(0x7C1E1E1E, true) : new Color(0x7C3B3B3B, true);
            Render2DEngine.drawRect(context, i + offset, mc.getWindow().getScaledHeight() - 24, 17, 17, HudEditor.hudRound.getValue(), 0.7f, c, c, c, c);
            GlStateManager._enableBlend();
            GlStateManager.glBlendFuncSeparate(770, 1, 1, 0);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, w.getIcon() != null ? w.getIcon() : TextureStorage.configIcon, (int) (i + offset) + 3, mc.getWindow().getScaledHeight() - 21, 11, 11, 0, 0, 11, 11, 11, 11);
            GlStateManager._disableBlend();
            offset += 20f;
        }

        windows.stream().filter(WindowBase::isVisible).forEach(w -> {
            if (w != lastClickedWindow)
                w.render(context, mouseX, mouseY);
        });

        if (lastClickedWindow != null && lastClickedWindow.isVisible())
            lastClickedWindow.render(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        windows.forEach(w -> w.mouseReleased(click.x(), click.y(), click.button()));
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        windows.stream().filter(WindowBase::isVisible).forEach(w -> w.mouseClicked(mouseX, mouseY, button));

        int i = mc.getWindow().getScaledWidth() / 2;
        float offset = (windows.size() * 20f) / -2f - 23;

        if (Render2DEngine.isHovered(mouseX, mouseY, (i + offset) + 1, mc.getWindow().getScaledHeight() - 23, 15, 15))
            mc.setScreen(ClickGUI.getClickGui());

        offset += 23;
        for (WindowBase w : windows) {
            if (Render2DEngine.isHovered(mouseX, mouseY, i + offset, mc.getWindow().getScaledHeight() - 24, 17, 17))
                w.setVisible(!w.isVisible());
            offset += 20f;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        windows.stream().filter(WindowBase::isVisible).forEach(w -> w.keyPressed(input.key(), input.scancode(), input.modifiers()));
        return super.keyPressed(input);
    }

    public boolean charTyped(CharInput input) {
        windows.stream().filter(WindowBase::isVisible).forEach(w -> w.charTyped((char) input.codepoint(), input.modifiers()));
        return super.charTyped(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        windows.stream().filter(WindowBase::isVisible).forEach(w -> w.mouseScrolled((int) (verticalAmount * 5D)));
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
