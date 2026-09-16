package thunder.hack.gui.mainmenu;

import net.minecraft.client.gl.RenderPipelines;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

import static thunder.hack.features.modules.Module.mc;

public class CreditsScreen extends Screen {
    public ArrayList<Contributor> contributors = new ArrayList<>();

    private static int scroll;
    private static final int SCROLL_SPEED = 1;

    protected CreditsScreen() {
        super(Text.of("CreditsScreen"));
        INSTANCE = this;
        for (String line : ThunderHack.contributors) {
            if (line == null)
                continue;
            String name = line.split(";")[0];
            String avatar = line.split(";")[1];
            String role = line.split(";")[2];
            String description = line.split(";")[3];
            String clickAction = line.split(";")[4];
            CreditsScreen.getInstance().contributors.add(new CreditsScreen.Contributor(name, CreditsScreen.getAvatar(avatar), role, description.replace('т', '\n'), clickAction));
        }
    }

    private static CreditsScreen INSTANCE = new CreditsScreen();

    public static CreditsScreen getInstance() {
        scroll = 150;
        if (INSTANCE == null) {
            INSTANCE = new CreditsScreen();
        }
        return INSTANCE;
    }

    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        float halfOfWidth = mc.getWindow().getScaledWidth() / 2f;
        float halfOfHeight = mc.getWindow().getScaledHeight() / 2f;
        float globalOffset = (contributors.size() * 150) / 2f;

        //  Render2DEngine.drawMainMenuShader(context, 0, 0, halfOfWidth * 2f, halfOfHeight * 2);

        GlStateManager._enableBlend();
        GlStateManager.glBlendFuncSeparate(770, 771, 1, 0);
        int offset = 0;

        for (Contributor contributor : contributors) {
            float cX = halfOfWidth + offset - globalOffset + scroll;
            float cY = halfOfHeight - 120;
            Render2DEngine.drawHudBase(context, cX, cY, 140, 240, 20, false);
            FontRenderers.sf_medium.drawGradientString(context, contributor.name, (cX + 70) - FontRenderers.sf_medium.getStringWidth(contributor.name) / 2f, halfOfHeight - 57, 30);
            FontRenderers.sf_medium.drawCenteredString(context, contributor.role, cX + 70, halfOfHeight - 48, new Color(0x818181).getRGB());

            Render2DEngine.horizontalGradient(context, cX + 2, cY + 90, cX + 70, cY + 91, Render2DEngine.injectAlpha(new Color(-1), 0), new Color(-1));
            Render2DEngine.horizontalGradient(context, cX + 70, cY + 90, cX + 138, cY + 91, new Color(-1), Render2DEngine.injectAlpha(new Color(-1), 0));
            Render2DEngine.drawRound(context, cX + 5, cY + 100, 130, 130, 8, new Color(0x73000000, true));
            FontRenderers.sf_medium.drawString(context, contributor.description, cX + 10, cY + 108, new Color(0x818182).getRGB());

            if (contributor.avatar != null)
                context.drawTexture(RenderPipelines.GUI_TEXTURED, contributor.avatar, (int) (cX + 70 - 24), (int) (halfOfHeight - 110), 48, 48, 0, 0, 96, 96, 96, 96);

            if (Render2DEngine.isHovered(mouseX, mouseY, cX, cY, 140, 240) && !Objects.equals(contributor.clickAction, ""))
                Render2DEngine.drawRound(context, cX, cY, 140, 240, 8, new Color(0x5FFFFFF, true));

            offset += 150;
        }
        GlStateManager._disableBlend();
        Render2DEngine.drawHudBase(context, mc.getWindow().getScaledWidth() - 40, mc.getWindow().getScaledHeight() - 40, 30, 30, 5, Render2DEngine.isHovered(mouseX, mouseY, mc.getWindow().getScaledWidth() - 60, mc.getWindow().getScaledHeight() - 60, 40, 40) ? 0.7f : 1f);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TextureStorage.thTeam, mc.getWindow().getScaledWidth() - 40, mc.getWindow().getScaledHeight() - 40, 30, 30, 0, 0, 30, 30, 30, 30);
    }


    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        float halfOfWidth = mc.getWindow().getScaledWidth() / 2f;
        float halfOfHeight = mc.getWindow().getScaledHeight() / 2f;
        float globalOffset = (contributors.size() * 150) / 2f;
        int offset = 0;
        for (Contributor contributor : contributors) {
            float cX = (float) (halfOfWidth + offset - globalOffset + Render2DEngine.interpolate(scroll, scroll + 1, Render3DEngine.getTickDelta()));
            float cY = halfOfHeight - 120;
            if (Render2DEngine.isHovered(mouseX, mouseY, cX, cY, 140, 240) && !Objects.equals(contributor.clickAction, "none"))
                Util.getOperatingSystem().open(URI.create(contributor.clickAction));
            offset += 150;
        }

        if (Render2DEngine.isHovered(mouseX, mouseY, mc.getWindow().getScaledWidth() - 40, mc.getWindow().getScaledHeight() - 40, 40, 40))
            mc.setScreen(MainMenuScreen.getInstance());

        return super.mouseClicked(click, doubled);
    }

    public record Contributor(String name, Identifier avatar, String role, String description, String clickAction) {
    }

    public static Identifier getAvatar(String name) {
        try {
            NativeImageBackedTexture nIBT = getAvatarFromURL("https://cdn.discordapp.com/avatars/" + name + ".png?size=96");

            if (nIBT != null) {
                Identifier id = Identifier.of("thunderhack", "contributors/th-contributors-" + (int) MathUtility.random(0, 1000000));
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, nIBT);
                return id;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static NativeImageBackedTexture getAvatarFromURL(String HeadStringURL) {
        try {
            return getAvatarFromStream(new URL(HeadStringURL).openStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static NativeImageBackedTexture getAvatarFromStream(InputStream image) {
        NativeImage pic = null;
        try {
            pic = NativeImage.read(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (pic != null) {
            return new NativeImageBackedTexture(() -> "th-avatar", parseAvatar(pic));
        }
        return null;
    }

    public static NativeImage parseAvatar(NativeImage image) {
        NativeImage imgNew = new NativeImage(96, 96, true);
        for (int x = 0; x < 96; x++) {
            for (int y = 0; y < 96; y++) {
                if (Math.hypot(x - 48, y - 48) > 45)
                    imgNew.setColorArgb(x, y, Render2DEngine.injectAlpha(new Color(image.getColorArgb(x, y)), (int) ((float) (48 - Math.hypot(x - 48, y - 48)) / 3f * 255f)).getRGB());
                else imgNew.setColorArgb(x, y, image.getColorArgb(x, y));
            }
        }
        image.close();
        return imgNew;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll += (int) (verticalAmount * 5D);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }


    @Override
    public void tick() {
        scroll -= SCROLL_SPEED;

        if (scroll <= -(contributors.size() * 150) + 100)
            scroll = 0;
    }
}