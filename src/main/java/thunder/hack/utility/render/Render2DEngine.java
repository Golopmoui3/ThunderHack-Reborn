package thunder.hack.utility.render;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.ColoredQuadGuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.lwjgl.BufferUtils;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.gui.font.Texture;
import thunder.hack.injection.accesors.IDrawContextAccessor;
import thunder.hack.utility.math.MathUtility;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static thunder.hack.features.modules.Module.mc;

public class Render2DEngine {
    // TODO(1.21.11): custom core shaders (rounded rect / hud / blur / arc / mainmenu) are not yet
    //  ported to the RenderPipeline system - the methods below use state-based fallbacks that keep
    //  the same shapes but without shader effects. The .fsh sources in assets/thunderhack/shaders
    //  are kept for the future port.
    public static HashMap<Integer, BlurredShadow> shadowCache = new HashMap<>();
    public static HashMap<Integer, BlurredShadow> shadowCache1 = new HashMap<>();
    final static Stack<Rectangle> clipStack = new Stack<>();

    private static Identifier boundTexture = null;

    private record QueuedText(String text, double x, double y, int color) {
    }

    private static final List<QueuedText> TEXT_QUEUE = new ArrayList<>();

    /**
     * Binds a texture for the following textured draw calls.
     * Replaces RenderSystem.setShaderTexture(0, id) which no longer exists.
     */
    public static void bindTexture(Identifier id) {
        boundTexture = id;
    }

    private static GuiRenderState state(DrawContext context) {
        return ((IDrawContextAccessor) context).thunderhack$getRenderState();
    }

    private static ScreenRect scissor() {
        if (clipStack.empty()) {
            return new ScreenRect(0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
        }
        Rectangle r = clipStack.peek();
        return new ScreenRect((int) r.x, (int) r.y, (int) Math.max(0, r.x1 - r.x), (int) Math.max(0, r.y1 - r.y));
    }

    public static ScreenRect currentScissor() {
        return scissor();
    }

    private static void quad(DrawContext context, float x0, float y0, float x1, float y1, int col1, int col2) {
        state(context).addSimpleElement(new ColoredQuadGuiElementRenderState(
                RenderPipelines.GUI, TextureSetup.empty(), new Matrix3x2f(context.getMatrices()),
                (int) x0, (int) y0, (int) x1, (int) y1, col1, col2, scissor()));
    }

    private static void texturedQuad(DrawContext context, Identifier id, float x0, float y0, float x1, float y1,
                                     float u0, float v0, float u1, float v1, int color) {
        AbstractTexture tex = mc.getTextureManager().getTexture(id);
        state(context).addSimpleElement(new TexturedQuadGuiElementRenderState(
                RenderPipelines.GUI_TEXTURED, TextureSetup.of(tex.getGlTextureView(), tex.getSampler()),
                new Matrix3x2f(context.getMatrices()),
                (int) x0, (int) y0, (int) x1, (int) y1, u0, u1, v0, v1, color, scissor()));
    }

    /**
     * Queues text projected from 3D space, drawn on the next 2D pass.
     * Replaces billboarded immediate text which is no longer possible in world rendering.
     */
    public static void queueText3D(String text, double x, double y, int color) {
        TEXT_QUEUE.add(new QueuedText(text, x, y, color));
    }

    public static void flushTextQueue(DrawContext context) {
        if (TEXT_QUEUE.isEmpty()) return;
        for (QueuedText t : TEXT_QUEUE) {
            FontRenderers.sf_medium.drawCenteredString(context, t.text, t.x, t.y, t.color);
        }
        TEXT_QUEUE.clear();
    }

    public static void addWindow(DrawContext context, Rectangle r1) {
        Matrix3x2fStack m = context.getMatrices();
        float x = m.m00() * r1.x + m.m10() * r1.y + m.m20();
        float y = m.m01() * r1.x + m.m11() * r1.y + m.m21();
        float endX = m.m00() * r1.x1 + m.m10() * r1.y1 + m.m20();
        float endY = m.m01() * r1.x1 + m.m11() * r1.y1 + m.m21();
        Rectangle r = new Rectangle(x, y, endX, endY);
        if (clipStack.empty()) {
            clipStack.push(r);
            context.enableScissor((int) r1.x, (int) r1.y, (int) r1.x1, (int) r1.y1);
        } else {
            Rectangle lastClip = clipStack.peek();
            float lsx = lastClip.x;
            float lsy = lastClip.y;
            float lstx = lastClip.x1;
            float lsty = lastClip.y1;
            float nsx = MathHelper.clamp(r.x, lsx, lstx);
            float nsy = MathHelper.clamp(r.y, lsy, lsty);
            float nstx = MathHelper.clamp(r.x1, nsx, lstx);
            float nsty = MathHelper.clamp(r.y1, nsy, lsty);
            clipStack.push(new Rectangle(nsx, nsy, nstx, nsty));
            context.enableScissor((int) nsx, (int) nsy, (int) nstx, (int) nsty);
        }
    }

    public static void popWindow(DrawContext context) {
        clipStack.pop();
        if (clipStack.empty()) {
            context.disableScissor();
        } else {
            Rectangle r = clipStack.peek();
            context.enableScissor((int) r.x, (int) r.y, (int) r.x1, (int) r.y1);
        }
    }

    public static void addWindow(DrawContext context, float x, float y, float x1, float y1, double animation_factor) {
        float h = y + y1;
        float h2 = (float) (h * (1d - MathUtility.clamp(animation_factor, 0, 1.0025f)));

        float x3 = x;
        float y3 = y + h2;
        float x4 = x1;
        float y4 = y1 - h2;

        if (x4 < x3) x4 = x3;
        if (y4 < y3) y4 = y3;
        addWindow(context, new Rectangle(x3, y3, x4, y4));
    }

    public static void horizontalGradient(DrawContext context, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
        quad(context, x1, y1, x2, y2, startColor.getRGB(), endColor.getRGB());
    }

    public static void verticalGradient(DrawContext context, float left, float top, float right, float bottom, Color startColor, Color endColor) {
        quad(context, left, top, right, bottom, startColor.getRGB(), endColor.getRGB());
    }

    public static void drawRect(DrawContext context, float x, float y, float width, float height, Color c) {
        quad(context, x, y, x + width, y + height, c.getRGB(), c.getRGB());
    }

    public static void drawRectWithOutline(DrawContext context, float x, float y, float width, float height, Color c, Color c2) {
        quad(context, x, y, x + width, y + height, c.getRGB(), c.getRGB());
        quad(context, x, y, x + width, y + 1, c2.getRGB(), c2.getRGB());
        quad(context, x, y + height - 1, x + width, y + height, c2.getRGB(), c2.getRGB());
        quad(context, x, y, x + 1, y + height, c2.getRGB(), c2.getRGB());
        quad(context, x + width - 1, y, x + width, y + height, c2.getRGB(), c2.getRGB());
    }

    public static void drawRectDumbWay(DrawContext context, float x, float y, float x1, float y1, Color c1) {
        quad(context, x, y, x1, y1, c1.getRGB(), c1.getRGB());
    }

    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX - width <= x && mouseY >= y && mouseY - height <= y;
    }

    public static void drawBlurredShadow(DrawContext context, float x, float y, float width, float height, int blurRadius, Color color) {
        if (!HudEditor.glow.getValue()) return;
        width = width + blurRadius * 2;
        height = height + blurRadius * 2;
        x = x - blurRadius;
        y = y - blurRadius;

        int identifier = (int) (width * height + width * blurRadius);
        if (shadowCache.containsKey(identifier)) {
            bindTexture(shadowCache.get(identifier).id.getId());
        } else {
            BufferedImage original = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = original.getGraphics();
            g.setColor(new Color(-1));
            g.fillRect(blurRadius, blurRadius, (int) (width - blurRadius * 2), (int) (height - blurRadius * 2));
            g.dispose();
            GaussianFilter op = new GaussianFilter(blurRadius);
            BufferedImage blurred = op.filter(original, null);
            shadowCache.put(identifier, new BlurredShadow(blurred));
            return;
        }

        renderGradientTexture(context, x, y, width, height, 0, 0, width, height, width, height, color, color, color, color);
    }

    public static void drawGradientBlurredShadow(DrawContext context, float x, float y, float width, float height, int blurRadius, Color color1, Color color2, Color color3, Color color4) {
        if (!HudEditor.glow.getValue()) return;
        width = width + blurRadius * 2;
        height = height + blurRadius * 2;
        x = x - blurRadius;
        y = y - blurRadius;

        int identifier = (int) (width * height + width * blurRadius);
        if (shadowCache.containsKey(identifier)) {
            bindTexture(shadowCache.get(identifier).id.getId());
        } else {
            BufferedImage original = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = original.getGraphics();
            g.setColor(new Color(-1));
            g.fillRect(blurRadius, blurRadius, (int) (width - blurRadius * 2), (int) (height - blurRadius * 2));
            g.dispose();
            GaussianFilter op = new GaussianFilter(blurRadius);
            BufferedImage blurred = op.filter(original, null);
            shadowCache.put(identifier, new BlurredShadow(blurred));
            return;
        }

        renderGradientTexture(context, x, y, width, height, 0, 0, width, height, width, height, color1, color2, color3, color4);
    }

    public static void drawGradientBlurredShadow1(DrawContext context, float x, float y, float width, float height, int blurRadius, Color color1, Color color2, Color color3, Color color4) {
        if (!HudEditor.glow.getValue()) return;
        width = width + blurRadius * 2;
        height = height + blurRadius * 2;
        x = x - blurRadius;
        y = y - blurRadius;

        int identifier = (int) (width * height + width * blurRadius);
        if (shadowCache1.containsKey(identifier)) {
            bindTexture(shadowCache1.get(identifier).id.getId());
        } else {
            BufferedImage original = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = original.getGraphics();
            g.setColor(new Color(-1));
            g.fillRect(blurRadius, blurRadius, (int) (width - blurRadius * 2), (int) (height - blurRadius * 2));
            g.dispose();
            BufferedImage blurred = new GaussianFilter(blurRadius).filter(original, null);

            BufferedImage black = new BufferedImage((int) width + blurRadius * 2, (int) height + blurRadius * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics g2 = black.getGraphics();
            g2.setColor(new Color(0x000000));
            g2.fillRect(0, 0, (int) width + blurRadius * 2, (int) height + blurRadius * 2);
            g2.dispose();

            BufferedImage combined = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
            Graphics g1 = combined.getGraphics();
            g1.drawImage(black, -blurRadius, -blurRadius, null);
            g1.drawImage(blurred, 0, 0, null);
            g1.dispose();

            shadowCache1.put(identifier, new BlurredShadow(combined));
            return;
        }

        renderGradientTexture(context, x, y, width, height, 0, 0, width, height, width, height, color1, color2, color3, color4);
    }

    public static void registerBufferedImageTexture(Texture i, BufferedImage bi) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            byte[] bytes = baos.toByteArray();
            registerTexture(i, bytes);
        } catch (Exception ignored) {
        }
    }

    private static void registerTexture(Texture i, byte[] content) {
        try {
            ByteBuffer data = BufferUtils.createByteBuffer(content.length).put(content);
            data.flip();
            NativeImageBackedTexture tex = new NativeImageBackedTexture(i.getId()::toString, NativeImage.read(data));
            mc.execute(() -> mc.getTextureManager().registerTexture(i.getId(), tex));
        } catch (Exception ignored) {
        }
    }

    public static void renderTexture(DrawContext context, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight) {
        if (boundTexture == null) return;
        float u0 = u / (float) textureWidth, v0 = v / (float) textureHeight;
        float u1 = (u + (float) regionWidth) / (float) textureWidth, v1 = (v + (float) regionHeight) / (float) textureHeight;
        texturedQuad(context, boundTexture, (float) x0, (float) y0, (float) (x0 + width), (float) (y0 + height), u0, v0, u1, v1, -1);
    }

    public static void renderGradientTexture(DrawContext context, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight, Color c1, Color c2, Color c3, Color c4) {
        if (boundTexture == null) return;
        // TODO(1.21.11): textured states carry a single tint color - gradient textures use the first color
        float u0 = u / (float) textureWidth, v0 = v / (float) textureHeight;
        float u1 = (u + (float) regionWidth) / (float) textureWidth, v1 = (v + (float) regionHeight) / (float) textureHeight;
        texturedQuad(context, boundTexture, (float) x0, (float) y0, (float) (x0 + width), (float) (y0 + height), u0, v0, u1, v1, c1.getRGB());
    }

    public static void renderRoundedGradientRect(DrawContext context, Color color1, Color color2, Color color3, Color color4, float x, float y, float width, float height, float Radius) {
        // TODO(1.21.11): rounded gradient rects need a 9-slice implementation; flat fallback
        draw2DGradientRect(context, x, y, x + width, y + height, color1, color2, color3, color4);
    }

    private static final Map<Integer, Identifier> ROUNDED_CACHE = new HashMap<>();

    private static Identifier roundedTexture(int radius) {
        return ROUNDED_CACHE.computeIfAbsent(radius, r -> {
            int size = r * 2 + 1;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, size, size, r * 2, r * 2);
            g.dispose();
            Texture tex = new Texture("temp/rounded_" + r);
            registerBufferedImageTexture(tex, img);
            return tex.getId();
        });
    }

    private static void roundedRect(DrawContext context, float x, float y, float width, float height, float radius, int color) {
        int r = Math.max(1, (int) radius);
        float hw = width / 2f, hh = height / 2f;
        if (r > hw) r = (int) hw;
        if (r > hh) r = (int) hh;
        if (r <= 0) {
            quad(context, x, y, x + width, y + height, color, color);
            return;
        }
        Identifier id = roundedTexture(r);
        float s = r;
        // corners
        texturedQuad(context, id, x, y, x + s, y + s, 0, 0, 0.5f, 0.5f, color);
        texturedQuad(context, id, x + width - s, y, x + width, y + s, 0.5f, 0, 1f, 0.5f, color);
        texturedQuad(context, id, x, y + height - s, x + s, y + height, 0, 0.5f, 0.5f, 1f, color);
        texturedQuad(context, id, x + width - s, y + height - s, x + width, y + height, 0.5f, 0.5f, 1f, 1f, color);
        // edges + center
        texturedQuad(context, id, x + s, y, x + width - s, y + s, 0.5f, 0, 0.5f, 0.5f, color);
        texturedQuad(context, id, x + s, y + height - s, x + width - s, y + height, 0.5f, 0.5f, 0.5f, 1f, color);
        texturedQuad(context, id, x, y + s, x + s, y + height - s, 0, 0.5f, 0.5f, 0.5f, color);
        texturedQuad(context, id, x + width - s, y + s, x + width, y + height - s, 0.5f, 0.5f, 1f, 0.5f, color);
        texturedQuad(context, id, x + s, y + s, x + width - s, y + height - s, 0.5f, 0.5f, 0.5f, 0.5f, color);
    }

    public static void drawRound(DrawContext context, float x, float y, float width, float height, float radius, Color color) {
        roundedRect(context, x, y, width, height, radius, color.getRGB());
    }

    public static void renderRoundedQuad(DrawContext context, Color c, double fromX, double fromY, double toX, double toY, double radius, double samples) {
        roundedRect(context, (float) fromX, (float) fromY, (float) (toX - fromX), (float) (toY - fromY), (float) radius, c.getRGB());
    }

    public static void renderRoundedQuad2(DrawContext context, Color c, Color c2, Color c3, Color c4, double fromX, double fromY, double toX, double toY, double radius) {
        // TODO(1.21.11): per-corner gradient rounded rects use the first color for now
        roundedRect(context, (float) fromX, (float) fromY, (float) (toX - fromX), (float) (toY - fromY), (float) radius, c.getRGB());
    }

    public static void draw2DGradientRect(DrawContext context, float left, float top, float right, float bottom, Color leftBottomColor, Color leftTopColor, Color rightBottomColor, Color rightTopColor) {
        // ColoredQuad states interpolate vertically (col1 top -> col2 bottom); approximate 4-corner gradient
        quad(context, left, top, right, bottom,
                interpolateColorC(leftTopColor, rightTopColor, 0.5f).getRGB(),
                interpolateColorC(leftBottomColor, rightBottomColor, 0.5f).getRGB());
    }

    public static void setupRender() {
        // No-op on 1.21.11: blending/depth are baked into RenderPipelines, there is no global shader state anymore.
    }

    public static void drawTracerPointer(DrawContext context, float x, float y, float size, float tracerWidth, float downHeight, boolean down, boolean glow, int color) {
        switch (HudEditor.arrowsStyle.getValue()) {
            case Default -> drawDefaultArrow(context, x, y, size, tracerWidth, downHeight, down, glow, color);
            case New -> drawNewArrow(context, x, y, size + 8, new Color(color));
        }
    }

    public static void drawNewArrow(DrawContext context, float x, float y, float size, Color color) {
        texturedQuad(context, TextureStorage.arrow, x - (size / 2f), y, x + size / 2f, y + size, 0f, 0f, 1f, 1f, color.getRGB());
    }

    public static void drawDefaultArrow(DrawContext context, float x, float y, float size, float tracerWidth, float downHeight, boolean down, boolean glow, int color) {
        if (glow)
            Render2DEngine.drawBlurredShadow(context, x - size * tracerWidth, y, (x + size * tracerWidth) - (x - size * tracerWidth), size, 10, Render2DEngine.injectAlpha(new Color(color), 140));

        quad(context, x, y, x, y, color, color);
        quad(context, x, y, x - size * tracerWidth, y + size, color, color);
        quad(context, x, y, x, y + size - downHeight, color, color);
        quad(context, x, y, x, y, color, color);
        color = Render2DEngine.darker(new Color(color), 0.8f).getRGB();
        quad(context, x, y, x, y, color, color);
        quad(context, x, y, x, y + size - downHeight, color, color);
        quad(context, x, y, x + size * tracerWidth, y + size, color, color);
        quad(context, x, y, x, y, color, color);

        if (down) {
            color = Render2DEngine.darker(new Color(color), 0.6f).getRGB();
            quad(context, x - size * tracerWidth, y + size, x + size * tracerWidth, y + size, color, color);
            quad(context, x + size * tracerWidth, y + size, x, y + size - downHeight, color, color);
            quad(context, x, y + size - downHeight, x - size * tracerWidth, y + size, color, color);
            quad(context, x - size * tracerWidth, y + size, x - size * tracerWidth, y + size, color, color);
        }
    }


    public static void endRender() {
        // No-op on 1.21.11, see setupRender().
    }

    public static void drawGradientRound(DrawContext context, float v, float v1, float i, float i1, float v2, Color darker, Color darker1, Color darker2, Color darker3) {
        renderRoundedQuad2(context, darker, darker1, darker2, darker3, v, v1, v + i, v1 + i1, v2);
    }

    public static float scrollAnimate(float endPoint, float current, float speed) {
        boolean shouldContinueAnimation = endPoint > current;
        if (speed < 0.0f) {
            speed = 0.0f;
        } else if (speed > 1.0f) {
            speed = 1.0f;
        }

        float dif = Math.max(endPoint, current) - Math.min(endPoint, current);
        float factor = dif * speed;
        return current + (shouldContinueAnimation ? factor : -factor);
    }

    public static Color injectAlpha(final Color color, final int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
    }

    public static Color TwoColoreffect(Color cl1, Color cl2, double speed, double count) {
        int angle = (int) (((System.currentTimeMillis()) / speed + count) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return interpolateColorC(cl1, cl2, angle / 360f);
    }

    public static Color astolfo(boolean clickgui, int yOffset) {
        float speed = clickgui ? 35 * 100 : 30 * 100;
        float hue = (System.currentTimeMillis() % (int) speed) + yOffset;
        if (hue > speed) {
            hue -= speed;
        }
        hue /= speed;
        if (hue > 0.5F) {
            hue = 0.5F - (hue - 0.5F);
        }
        hue += 0.5F;
        return Color.getHSBColor(hue, 0.4F, 1F);
    }

    public static Color rainbow(int delay, float saturation, float brightness) {
        double rainbow = Math.ceil((System.currentTimeMillis() + delay) / 16f);
        rainbow %= 360;
        return Color.getHSBColor((float) (rainbow / 360), saturation, brightness);
    }

    public static Color skyRainbow(int speed, int index) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        return Color.getHSBColor((double) ((float) ((angle %= 360) / 360.0)) < 0.5 ? -((float) (angle / 360.0)) : (float) (angle / 360.0), 0.5F, 1.0F);
    }

    public static Color fade(int speed, int index, Color color, float alpha) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;

        Color colorHSB = new Color(Color.HSBtoRGB(hsb[0], hsb[1], angle / 360f));

        return new Color(colorHSB.getRed(), colorHSB.getGreen(), colorHSB.getBlue(), Math.max(0, Math.min(255, (int) (alpha * 255))));
    }

    public static Color getAnalogousColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float degree = 0.84f;
        float newHueSubtracted = hsb[0] - degree;
        return new Color(Color.HSBtoRGB(newHueSubtracted, hsb[1], hsb[2]));
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static int applyOpacity(int color_int, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        Color color = new Color(color_int);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity)).getRGB();
    }

    public static Color darker(Color color, float factor) {
        return new Color(Math.max((int) (color.getRed() * factor), 0), Math.max((int) (color.getGreen() * factor), 0), Math.max((int) (color.getBlue() * factor), 0), color.getAlpha());
    }

    public static Color rainbow(int speed, int index, float saturation, float brightness, float opacity) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        float hue = angle / 360f;
        Color color = new Color(Color.HSBtoRGB(hue, saturation, brightness));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, (int) (opacity * 255))));
    }

    public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end, boolean trueColor) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return trueColor ? interpolateColorHue(start, end, angle / 360f) : interpolateColorC(start, end, angle / 360f);
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(interpolateInt(color1.getRed(), color2.getRed(), amount), interpolateInt(color1.getGreen(), color2.getGreen(), amount), interpolateInt(color1.getBlue(), color2.getBlue(), amount), interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static Color interpolateColorHue(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
        float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);

        Color resultColor = Color.getHSBColor(interpolateFloat(color1HSB[0], color2HSB[0], amount), interpolateFloat(color1HSB[1], color2HSB[1], amount), interpolateFloat(color1HSB[2], color2HSB[2], amount));

        return new Color(resultColor.getRed(), resultColor.getGreen(), resultColor.getBlue(), interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static double interpolate(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static int interpolateInt(float oldValue, float newValue, double interpolationValue) {
        return (int) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static void drawArc(DrawContext context, float x, float y, float width, float height, float radius, float thickness, float start, float end, Color c1, Color c2) {
        int segments = 48;
        float a0 = (float) Math.toRadians(start);
        float a1 = (float) Math.toRadians(end);
        float rx = width / 2f, ry = height / 2f;
        Matrix3x2f base = new Matrix3x2f(context.getMatrices());
        for (int i = 0; i < segments; i++) {
            float t0 = a0 + (a1 - a0) * i / segments;
            float t1 = a0 + (a1 - a0) * (i + 1) / segments;
            float tm = (t0 + t1) / 2f;
            float co = (float) Math.cos(tm), si = (float) Math.sin(tm);
            float segLen = (float) (Math.hypot((Math.cos(t1) - Math.cos(t0)) * rx, (Math.sin(t1) - Math.sin(t0)) * ry) / 2f);
            Matrix3x2f pose = new Matrix3x2f(base);
            pose.translate(x + co * (rx - thickness / 2f), y + si * (ry - thickness / 2f));
            pose.rotate((float) (Math.atan2(Math.cos(tm) * ry, -Math.sin(tm) * rx) + Math.PI / 2));
            Color c = interpolateColorC(c1, c2, i / (float) segments);
            state(context).addSimpleElement(new ColoredQuadGuiElementRenderState(
                    RenderPipelines.GUI, TextureSetup.empty(), pose,
                    (int) -segLen, (int) (-thickness / 2), (int) segLen, (int) (thickness / 2),
                    c.getRGB(), c.getRGB(), scissor()));
        }
    }

    public static void drawRect(DrawContext context, float x, float y, float width, float height, float radius, float alpha) {
        // TODO(1.21.11): rounded core shader not yet ported; 9-slice fallback keeps the shape
        roundedRect(context, x, y, width, height, radius, new Color(10, 10, 15, (int) (alpha * 255)).getRGB());
    }

    public static void drawRect(DrawContext context, float x, float y, float width, float height, float radius, float alpha, Color c1, Color c2, Color c3, Color c4) {
        // TODO(1.21.11): rounded core shader not yet ported; 9-slice fallback keeps the shape
        roundedRect(context, x, y, width, height, radius, c1.getRGB());
    }

    public static void drawHudBase(DrawContext context, float x, float y, float width, float height, float radius) {
        roundedRect(context, x, y, width, height, radius, new Color(13, 15, 20, (int) (HudEditor.alpha.getValue() * 255)).getRGB());
    }

    public static void drawHudBase2(DrawContext context, float x, float y, float width, float height, float radius, float blurStrenth, float blurOpacity, float animationFactor) {
        // TODO(1.21.11): blur behind panels not yet ported; flat fallback
        roundedRect(context, x, y, width, height, radius, new Color(13, 15, 20, (int) (HudEditor.alpha.getValue() * 255)).getRGB());
    }

    public static void drawHudBase(DrawContext context, float x, float y, float width, float height, float radius, boolean hud) {
        roundedRect(context, x, y, width, height, radius, new Color(13, 15, 20, (int) (HudEditor.alpha.getValue() * 255)).getRGB());
    }

    public static void drawRoundedBlur(DrawContext context, float x, float y, float width, float height, float radius, Color c1) {
        // TODO(1.21.11): blur core shader not yet ported; flat fallback
        roundedRect(context, x, y, width, height, radius, c1.getRGB());
    }

    public static void drawRoundedBlur(DrawContext context, float x, float y, float width, float height, float radius, Color c1, float blurStrenth, float blurOpacity) {
        // TODO(1.21.11): blur core shader not yet ported; flat fallback
        roundedRect(context, x, y, width, height, radius, c1.getRGB());
    }

    public static void drawHudBase(DrawContext context, float x, float y, float width, float height, float radius, float alpha) {
        roundedRect(context, x, y, width, height, radius, new Color(13, 15, 20, (int) (alpha * 255)).getRGB());
    }

    public static void drawGuiBase(DrawContext context, float x, float y, float width, float height, float radius, float opacity) {
        roundedRect(context, x, y, width, height, radius, new Color(13, 15, 20, (int) (opacity * 255)).getRGB());
    }

    public static void drawMainMenuShader(DrawContext context, float x, float y, float width, float height) {
        // TODO(1.21.11): mainmenu core shader not yet ported; gradient fallback
        verticalGradient(context, x, y, x + width, y + height, new Color(8, 10, 20), new Color(24, 18, 36));
    }

    public static void drawOrbiz(DrawContext context, float z, final double r, Color c) {
        texturedQuad(context, TextureStorage.default_circle, (float) -r, (float) -r, (float) r, (float) r, 0f, 0f, 1f, 1f, c.getRGB());
    }

    public static void drawStar(DrawContext context, Color c, float scale) {
        bindTexture(TextureStorage.star);
        Render2DEngine.renderGradientTexture(context, 0, 0, scale, scale, 0, 0, 128, 128, 128, 128, c, c, c, c);
    }

    public static void drawHeart(DrawContext context, Color c, float scale) {
        bindTexture(TextureStorage.heart);
        Render2DEngine.renderGradientTexture(context, 0, 0, scale, scale, 0, 0, 128, 128, 128, 128, c, c, c, c);
    }

    public static void drawBloom(DrawContext context, Color c, float scale) {
        bindTexture(TextureStorage.firefly);
        Render2DEngine.renderGradientTexture(context, 0, 0, scale, scale, 0, 0, 128, 128, 128, 128, c, c, c, c);
    }

    public static void drawBubble(DrawContext context, float angle, float factor) {
        float scale = factor * 2f;
        Matrix3x2f pose = new Matrix3x2f(context.getMatrices());
        pose.rotate((float) Math.toRadians(angle));
        state(context).addSimpleElement(new TexturedQuadGuiElementRenderState(
                RenderPipelines.GUI_TEXTURED, textureSetup(TextureStorage.bubble), pose,
                (int) (-scale / 2), (int) (-scale / 2), (int) (scale / 2), (int) (scale / 2),
                0f, 1f, 0f, 1f,
                applyOpacity(HudEditor.getColor(270), 1f - factor).getRGB(), scissor()));
    }

    public static void drawLine(DrawContext context, float x, float y, float x1, float y1, int color) {
        float dx = x1 - x, dy = y1 - y;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.5f) return;
        Matrix3x2f pose = new Matrix3x2f(context.getMatrices());
        pose.translate(x, y);
        pose.rotate((float) Math.atan2(dy, dx));
        state(context).addSimpleElement(new ColoredQuadGuiElementRenderState(
                RenderPipelines.GUI, TextureSetup.empty(), pose,
                0, 0, (int) len, 1, color, color, scissor()));
    }

    /**
     * Arbitrary quad (e.g. ellipse arc segment) approximated as a rotated rect.
     * Used where triangle strips/fans were used before - states only support axis-aligned quads.
     */
    public static void drawQuadStrip(DrawContext context, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
        float dx = x3 - x0, dy = y3 - y0;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.5f) return;
        float w = ((float) Math.hypot(x1 - x0, y1 - y0) + (float) Math.hypot(x2 - x3, y2 - y3)) / 2f;
        Matrix3x2f pose = new Matrix3x2f(context.getMatrices());
        pose.translate(x0, y0);
        pose.rotate((float) Math.atan2(dy, dx));
        int c = color.getRGB();
        state(context).addSimpleElement(new ColoredQuadGuiElementRenderState(
                RenderPipelines.GUI, TextureSetup.empty(), pose,
                0, 0, (int) len, (int) Math.max(1, w), c, c, scissor()));
    }

    //http://www.java2s.com/example/java/2d-graphics/check-if-a-color-is-more-dark-than-light.html
    public static boolean isDark(Color color) {
        return isDark(color.getRed() / 255.0f, color.getGreen() / 255f, color.getBlue() / 255f);
    }

    public static boolean isDark(float r, float g, float b) {
        return colorDistance(r, g, b, 0f, 0f, 0f) < colorDistance(r, g, b, 1f, 1f, 1f);
    }

    public static float colorDistance(float r1, float g1, float b1, float r2, float g2, float b2) {
        float a = r2 - r1;
        float b = g2 - g1;
        float c = b2 - b1;
        return (float) Math.sqrt(a * a + b * b + c * c);
    }

    public static void initShaders() {
        // TODO(1.21.11): custom core shaders will be ported to RenderPipelines; see ThunderRenderLayers
    }

    public static @NotNull Color getColor(@NotNull Color start, @NotNull Color end, float progress, boolean smooth) {
        if (!smooth)
            return progress >= 0.95 ? end : start;

        final int rDiff = end.getRed() - start.getRed();
        final int gDiff = end.getGreen() - start.getGreen();
        final int bDiff = end.getBlue() - start.getBlue();
        final int aDiff = end.getAlpha() - start.getAlpha();

        return new Color(
                fixColorValue(start.getRed() + (int) (rDiff * progress)),
                fixColorValue(start.getGreen() + (int) (gDiff * progress)),
                fixColorValue(start.getBlue() + (int) (bDiff * progress)),
                fixColorValue(start.getAlpha() + (int) (aDiff * progress)));
    }

    private static int fixColorValue(int colorVal) {
        return colorVal > 255 ? 255 : Math.max(colorVal, 0);
    }

    public static void endBuilding(BufferBuilder bb) {
        BuiltBuffer builtBuffer = bb.endNullable();
        if (builtBuffer != null)
            RenderLayers.debugQuads().draw(builtBuffer);
    }

    public static void endBuildingTextured(BufferBuilder bb) {
        BuiltBuffer builtBuffer = bb.endNullable();
        if (builtBuffer != null && boundTexture != null)
            ThunderRenderLayers.guiTextured(boundTexture).draw(builtBuffer);
    }

    public static void endBuildingFan(BufferBuilder bb) {
        BuiltBuffer builtBuffer = bb.endNullable();
        if (builtBuffer != null)
            RenderLayers.debugTriangleFan().draw(builtBuffer);
    }

    public static void endBuildingLines(BufferBuilder bb) {
        BuiltBuffer builtBuffer = bb.endNullable();
        if (builtBuffer != null)
            RenderLayers.linesTranslucent().draw(builtBuffer);
    }

    public static TextureSetup textureSetup(Identifier id) {
        AbstractTexture tex = mc.getTextureManager().getTexture(id);
        return TextureSetup.of(tex.getGlTextureView(), tex.getSampler());
    }

    public static class BlurredShadow {
        Texture id;

        public BlurredShadow(BufferedImage bufferedImage) {
            this.id = new Texture("texture/remote/" + RandomStringUtils.randomAlphanumeric(16));
            registerBufferedImageTexture(id, bufferedImage);
        }

        public void bind() {
            bindTexture(id.getId());
        }
    }

    public record Rectangle(float x, float y, float x1, float y1) {
        public boolean contains(double x, double y) {
            return x >= this.x && x <= x1 && y >= this.y && y <= y1;
        }
    }
}
