package thunder.hack.features.modules.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3x2fStack;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render3DEngine;

public class TotemAnimation extends Module {
    public TotemAnimation() {
        super("TotemAnimation", Category.RENDER);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.FadeOut);
    private final Setting<Integer> speed = new Setting<>("Speed", 40, 1, 100);

    private ItemStack floatingItem = null;
    private int floatingItemTimeLeft;

    public void showFloatingItem(ItemStack floatingItem) {
        this.floatingItem = floatingItem;
        floatingItemTimeLeft = getTime();
    }

    @Override
    public void onUpdate() {
        if (floatingItemTimeLeft > 0) {
            --floatingItemTimeLeft;
            if (floatingItemTimeLeft == 0) {
                floatingItem = null;
            }
        }
    }

    @Override
    public void onRender2D(DrawContext context) {
        renderFloatingItem(context, Render3DEngine.getTickDelta());
    }

    public void renderFloatingItem(DrawContext context, float tickDelta) {
        if (floatingItem != null && floatingItemTimeLeft > 0 && !mode.is(Mode.Off)) {
            int scaledWidth = mc.getWindow().getScaledWidth();
            int scaledHeight = mc.getWindow().getScaledHeight();

            int elapsedTime = getTime() - floatingItemTimeLeft;
            float animationProgress = ((float) elapsedTime + tickDelta) / (float) getTime();
            float progressSquared = animationProgress * animationProgress;
            float progressCubed = animationProgress * progressSquared;
            float oscillationFactor = 10.25F * progressCubed * progressSquared - 24.95F * progressSquared * progressSquared + 25.5F * progressCubed - 13.8F * progressSquared + 4.0F * animationProgress;
            float oscillationRadians = oscillationFactor * 3.1415927F;
            Matrix3x2fStack matrices = context.getMatrices();
            matrices.pushMatrix();
            float adjustedProgress = ((float) elapsedTime + tickDelta);
            float scale = 50.0F + 175.0F * MathHelper.sin(oscillationRadians);

            switch (mode.getValue()) {
                case FadeOut -> {
                    final float x2 = (float) (Math.sin(((adjustedProgress * 112) / 180f)) * 100);
                    final float y2 = (float) (Math.cos(((adjustedProgress * 112) / 180f)) * 50);
                    matrices.translate((float) (scaledWidth / 2) + x2, (float) (scaledHeight / 2) + y2);
                    matrices.scale(scale / 16f, scale / 16f);
                }

                case Size -> {
                    matrices.translate((float) (scaledWidth / 2), (float) (scaledHeight / 2));
                    matrices.scale(scale / 16f, scale / 16f);
                }

                case Otkisuli -> {
                    matrices.translate((float) (scaledWidth / 2), (float) (scaledHeight / 2));
                    // TODO(1.21.11): X rotation has no 2D equivalent and was dropped
                    matrices.rotate((float) Math.toRadians(adjustedProgress * 2));
                    float s = (200 - adjustedProgress * 1.5f) / 16f;
                    matrices.scale(s, s);
                }

                case Insert -> {
                    matrices.translate((float) (scaledWidth / 2), (float) (scaledHeight / 2));
                    // TODO(1.21.11): X rotation has no 2D equivalent and was dropped
                    float s = (200 - adjustedProgress * 1.5f) / 16f;
                    matrices.scale(s, s);
                }

                case Fall -> {
                    float downFactor = (float) (Math.pow(adjustedProgress, 3) * 0.2f);
                    matrices.translate((float) (scaledWidth / 2), (float) (scaledHeight / 2) + downFactor);
                    matrices.rotate((float) Math.toRadians(adjustedProgress * 5));
                    float s = (200 - adjustedProgress * 1.5f) / 16f;
                    matrices.scale(s, s);
                }

                case Rocket -> {
                    float downFactor = (float) (Math.pow(adjustedProgress, 3) * 0.2f) - 20;
                    matrices.translate((float) (scaledWidth / 2), (float) (scaledHeight / 2) - downFactor);
                    // TODO(1.21.11): Y spin has no 2D equivalent and was dropped
                    float s = (200 - adjustedProgress * 1.5f) / 16f;
                    matrices.scale(s, s);
                }

                case Roll -> {
                    float rightFactor = (float) (Math.pow(adjustedProgress, 2) * 4.5f);
                    matrices.translate((float) (scaledWidth / 2) + rightFactor, (float) (scaledHeight / 2));
                    matrices.rotate((float) Math.toRadians(adjustedProgress * 40));
                    float s = (200 - adjustedProgress * 1.5f) / 16f;
                    matrices.scale(s, s);
                }
            }

            // TODO(1.21.11): no global shader tint anymore; totem draws untinted for now
            context.drawItem(floatingItem, -8, -8);
            matrices.popMatrix();
        }
    }

    private int getTime() {
        int invertedSpeed = 101 - speed.getValue();

        if (mode.is(Mode.FadeOut))
            return invertedSpeed / 4;

        if (mode.is(Mode.Insert))
            return invertedSpeed / 2;

        return invertedSpeed;
    }

    private enum Mode {
        FadeOut, Size, Otkisuli, Insert, Fall, Rocket, Roll, Off
    }
}
