package thunder.hack.features.modules.render;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventAttack;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;
import thunder.hack.utility.render.ThunderRenderLayers;

import java.util.ArrayList;

public class HitBubbles extends Module {
    public HitBubbles() {
        super("HitBubbles", Category.RENDER);
    }

    public final Setting<Integer> lifeTime = new Setting<>("LifeTime", 30, 1, 150);

    private final ArrayList<HitBubble> bubbles = new ArrayList<>();

    @EventHandler
    public void onHit(EventAttack e) {
        Vec3d point = Managers.PLAYER.getRtxPoint(((IClientPlayerEntity) mc.player).getLastYaw(), ((IClientPlayerEntity) mc.player).getLastPitch(), ModuleManager.aura.attackRange.getValue());
        if (point != null && !e.isPre())
            bubbles.add(new HitBubble((float) point.x, (float) point.y, (float) point.z, -((IClientPlayerEntity) mc.player).getLastYaw(), ((IClientPlayerEntity) mc.player).getLastPitch(), new Timer()));
    }

    public void onRender3D(MatrixStack matrixStack) {
        GlStateManager._disableDepthTest();
        ArrayList<HitBubble> bubblesCopy = Lists.newArrayList(bubbles);
        bubblesCopy.forEach(b -> {
            matrixStack.push();
            matrixStack.translate(b.x - mc.getEntityRenderDispatcher().camera.getCameraPos().getX(), b.y - mc.getEntityRenderDispatcher().camera.getCameraPos().getY(), b.z - mc.getEntityRenderDispatcher().camera.getCameraPos().getZ());
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(b.yaw));
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(b.pitch));
            drawBubble3D(matrixStack, -b.life.getPassedTimeMs() / 4f, b.life.getPassedTimeMs() / 1500f);
            matrixStack.pop();
        });
        GlStateManager._enableDepthTest();
        bubbles.removeIf(b -> b.life.passedMs(lifeTime.getValue() * 50));
    }

    private void drawBubble3D(MatrixStack matrices, float angle, float factor) {
        float scale = factor * 2f;
        float rad = (float) Math.toRadians(angle);
        float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);
        Matrix4f m = matrices.peek().getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int c = Render2DEngine.applyOpacity(HudEditor.getColor(270), 1f - factor).getRGB();
        float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, b = (c & 255) / 255f, a = (c >> 24 & 255) / 255f;
        float h = scale / 2f;
        float[] xs = {-h, h, h, -h}, ys = {-h, -h, h, h};
        float[] us = {0f, 1f, 1f, 0f}, vs = {0f, 0f, 1f, 1f};
        for (int i = 0; i < 4; i++) {
            float xr = xs[i] * cos - ys[i] * sin;
            float yr = xs[i] * sin + ys[i] * cos;
            bb.vertex(m, xr, yr, 0).texture(us[i], vs[i]).color(r, g, b, a);
        }
        ThunderRenderLayers.guiTextured(TextureStorage.bubble).draw(bb.end());
    }

    public record HitBubble(float x, float y, float z, float yaw, float pitch, Timer life) {
    }
}
