package thunder.hack.utility.render;
import com.mojang.blaze3d.vertex.VertexFormat;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static thunder.hack.features.modules.Module.mc;

public class Render3DEngine {
    public static List<FillAction> FILLED_QUEUE = new ArrayList<>();
    public static List<OutlineAction> OUTLINE_QUEUE = new ArrayList<>();
    public static List<FadeAction> FADE_QUEUE = new ArrayList<>();
    public static List<FillSideAction> FILLED_SIDE_QUEUE = new ArrayList<>();
    public static List<OutlineSideAction> OUTLINE_SIDE_QUEUE = new ArrayList<>();
    public static List<DebugLineAction> DEBUG_LINE_QUEUE = new ArrayList<>();
    public static List<LineAction> LINE_QUEUE = new ArrayList<>();

    public static final Matrix4f lastProjMat = new Matrix4f();
    public static final Matrix4f lastModMat = new Matrix4f();
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

    private static float prevCircleStep;
    private static float circleStep;

    // getTickDelta() -> mc.getRenderTickCounter().getTickDelta(true)

    public static void onRender3D(MatrixStack stack) {
        if (!FILLED_QUEUE.isEmpty() || !FADE_QUEUE.isEmpty() || !FILLED_SIDE_QUEUE.isEmpty()) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            GlStateManager._disableDepthTest();
            setupRender();

            FILLED_QUEUE.forEach(action -> setFilledBoxVertexes(bufferBuilder, stack.peek().getPositionMatrix(), action.box(), action.color()));

            FADE_QUEUE.forEach(action -> setFilledFadePoints(action.box(), bufferBuilder, stack.peek().getPositionMatrix(), action.color(), action.color2()));

            FILLED_SIDE_QUEUE.forEach(action -> setFilledSidePoints(bufferBuilder, stack.peek().getPositionMatrix(), action.box, action.color(), action.side()));
            Render2DEngine.endBuilding(bufferBuilder);

            endRender();
            GlStateManager._enableDepthTest();

            FADE_QUEUE.clear();
            FILLED_SIDE_QUEUE.clear();
            FILLED_QUEUE.clear();
        }

        if (!OUTLINE_QUEUE.isEmpty() || !OUTLINE_SIDE_QUEUE.isEmpty()) {
            setupRender();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
            GlStateManager._disableCull();
            GlStateManager._disableDepthTest();

            OUTLINE_QUEUE.forEach(action -> {
                setOutlinePoints(action.box(), matrixFrom(action.box().minX, action.box().minY, action.box().minZ), buffer, action.color(), action.lineWidth());
            });

            OUTLINE_SIDE_QUEUE.forEach(action -> {
                setSideOutlinePoints(action.box, matrixFrom(action.box().minX, action.box().minY, action.box().minZ), buffer, action.color(), action.side(), action.lineWidth());
            });

            Render2DEngine.endBuildingLines(buffer);

            GlStateManager._enableCull();
            GlStateManager._enableDepthTest();
            endRender();
            OUTLINE_QUEUE.clear();
            OUTLINE_SIDE_QUEUE.clear();
        }

        if (!DEBUG_LINE_QUEUE.isEmpty()) {
            setupRender();
            GlStateManager._disableDepthTest();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);

            GlStateManager._disableCull();
            DEBUG_LINE_QUEUE.forEach(action -> {
                MatrixStack matrices = matrixFrom(action.start.getX(), action.start.getY(), action.start.getZ());
                vertexLine(matrices, buffer, 0f, 0f, 0f, (float) (action.end.getX() - action.start.getX()), (float) (action.end.getY() - action.start.getY()), (float) (action.end.getZ() - action.start.getZ()), action.color, 1f);
            });
            Render2DEngine.endBuildingLines(buffer);
            GlStateManager._enableCull();
            GlStateManager._enableDepthTest();
            endRender();
            DEBUG_LINE_QUEUE.clear();
        }

        if (!LINE_QUEUE.isEmpty()) {
            setupRender();
            Tessellator tessellator = Tessellator.getInstance();
            GlStateManager._disableCull();
            GlStateManager._disableDepthTest();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
            LINE_QUEUE.forEach(action -> {
                MatrixStack matrices = matrixFrom(action.start.getX(), action.start.getY(), action.start.getZ());
                vertexLine(matrices, buffer, 0f, 0f, 0f, (float) (action.end.getX() - action.start.getX()), (float) (action.end.getY() - action.start.getY()), (float) (action.end.getZ() - action.start.getZ()), action.color, 2f);
            });
            Render2DEngine.endBuildingLines(buffer);
            GlStateManager._enableCull();
            GlStateManager._enableDepthTest();
            endRender();
            LINE_QUEUE.clear();
        }
    }

    @Deprecated
    @SuppressWarnings("unused")
    public static void drawFilledBox(MatrixStack stack, Box box, Color c) {
        FILLED_QUEUE.add(new FillAction(box, c));
    }

    public static void setFilledBoxVertexes(@NotNull BufferBuilder bufferBuilder, Matrix4f m, @NotNull Box box, @NotNull Color c) {
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());

        bufferBuilder.vertex(m, minX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, minY, maxZ).color(c.getRGB());

        bufferBuilder.vertex(m, minX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, minZ).color(c.getRGB());

        bufferBuilder.vertex(m, maxX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, maxZ).color(c.getRGB());

        bufferBuilder.vertex(m, minX, minY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, maxZ).color(c.getRGB());

        bufferBuilder.vertex(m, minX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, minY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, minZ).color(c.getRGB());

        bufferBuilder.vertex(m, minX, maxY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, minZ).color(c.getRGB());
    }

    public static @NotNull Box interpolateBox(@NotNull Box from, @NotNull Box to, float delta) {
        double X = Render2DEngine.interpolate(from.maxX, to.maxX, delta);
        double Y = Render2DEngine.interpolate(from.maxY, to.maxY, delta);
        double Z = Render2DEngine.interpolate(from.maxZ, to.maxZ, delta);
        double X1 = Render2DEngine.interpolate(from.minX, to.minX, delta);
        double Y1 = Render2DEngine.interpolate(from.minY, to.minY, delta);
        double Z1 = Render2DEngine.interpolate(from.minZ, to.minZ, delta);
        return new Box(X1, Y1, Z1, X, Y, Z);
    }

    @Deprecated
    public static void drawFilledSide(MatrixStack stack, @NotNull Box box, Color c, Direction dir) {
        FILLED_SIDE_QUEUE.add(new FillSideAction(box, c, dir));
    }

    public static void setFilledSidePoints(BufferBuilder buffer, Matrix4f matrix, Box box, Color c, Direction dir) {
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());

        if (dir == Direction.DOWN) {
            buffer.vertex(matrix, minX, minY, minZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, minY, minZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, minY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, minX, minY, maxZ).color(c.getRGB());
        }

        if (dir == Direction.NORTH) {
            buffer.vertex(matrix, minX, minY, minZ).color(c.getRGB());
            buffer.vertex(matrix, minX, maxY, minZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, maxY, minZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, minY, minZ).color(c.getRGB());
        }

        if (dir == Direction.EAST) {
            buffer.vertex(matrix, maxX, minY, minZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, maxY, minZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, maxY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, minY, maxZ).color(c.getRGB());
        }
        if (dir == Direction.SOUTH) {
            buffer.vertex(matrix, minX, minY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, minY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, maxY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, minX, maxY, maxZ).color(c.getRGB());
        }

        if (dir == Direction.WEST) {
            buffer.vertex(matrix, minX, minY, minZ).color(c.getRGB());
            buffer.vertex(matrix, minX, minY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, minX, maxY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, minX, maxY, minZ).color(c.getRGB());
        }

        if (dir == Direction.UP) {
            buffer.vertex(matrix, minX, maxY, minZ).color(c.getRGB());
            buffer.vertex(matrix, minX, maxY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, maxY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, maxY, minZ).color(c.getRGB());
        }
    }

    public static void drawTextIn3D(String text, @NotNull Vec3d pos, double offX, double offY, double textOffset, @NotNull Color color) {
        if (mc.gameRenderer == null) return;
        Vec3d ndc = mc.gameRenderer.project(pos.add(offX, offY - 0.1, 0));
        if (ndc.z > 1.0) return;
        double sx = (ndc.x * 0.5 + 0.5) * mc.getWindow().getScaledWidth();
        double sy = (1.0 - (ndc.y * 0.5 + 0.5)) * mc.getWindow().getScaledHeight();
        Render2DEngine.queueText3D(text, sx + textOffset, sy, color.getRGB());
    }

    public static @NotNull Vec3d worldSpaceToScreenSpace(@NotNull Vec3d pos) {
        if (mc.gameRenderer == null) return Vec3d.ZERO;
        Vec3d ndc = mc.gameRenderer.project(pos);
        double sx = (ndc.x * 0.5 + 0.5) * mc.getWindow().getScaledWidth();
        double sy = (1.0 - (ndc.y * 0.5 + 0.5)) * mc.getWindow().getScaledHeight();
        return new Vec3d(sx, sy, ndc.z);
    }

    public static double getScaleFactor() {
        return ClientSettings.scaleFactorFix.getValue() ? ClientSettings.scaleFactorFixValue.getValue() : mc.getWindow().getScaleFactor();
    }

    @Deprecated
    @SuppressWarnings("unused")
    public static void drawFilledFadeBox(@NotNull MatrixStack stack, @NotNull Box box, @NotNull Color c, @NotNull Color c1) {
        FADE_QUEUE.add(new FadeAction(box, c, c1));
    }

    public static void setFilledFadePoints(Box box, BufferBuilder buffer, Matrix4f posMatrix, Color c, Color c1) {
        float minX = (float) (box.minX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float minY = (float) (box.minY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float minZ = (float) (box.minZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());
        float maxX = (float) (box.maxX - mc.getEntityRenderDispatcher().camera.getPos().getX());
        float maxY = (float) (box.maxY - mc.getEntityRenderDispatcher().camera.getPos().getY());
        float maxZ = (float) (box.maxZ - mc.getEntityRenderDispatcher().camera.getPos().getZ());

        if (ModuleManager.holeESP.culling.getValue())
            GlStateManager._enableCull();

        buffer.vertex(posMatrix, minX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, minY, minZ).color(c.getRGB());

        buffer.vertex(posMatrix, maxX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(c.getRGB());

        buffer.vertex(posMatrix, minX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(c1.getRGB());

        buffer.vertex(posMatrix, minX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, minZ).color(c1.getRGB());

        buffer.vertex(posMatrix, minX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(c1.getRGB());

        if (ModuleManager.holeESP.culling.getValue())
            GlStateManager._disableCull();
    }

    public static void drawLine(@NotNull Vec3d start, @NotNull Vec3d end, @NotNull Color color) {
        LINE_QUEUE.add(new LineAction(start, end, color));
    }

    @Deprecated
    public static void drawBoxOutline(@NotNull Box box, Color color, float lineWidth) {
        OUTLINE_QUEUE.add(new OutlineAction(box, color, lineWidth));
    }

    public static void setOutlinePoints(Box box, MatrixStack matrices, BufferBuilder buffer, Color color, float width) {
        box = box.offset(new Vec3d(box.minX, box.minY, box.minZ).negate());

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color, width);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color, width);
        vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color, width);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color, width);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color, width);
        vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color, width);
        vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color, width);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color, width);
        vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color, width);
        vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color, width);
        vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color, width);
        vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color, width);
    }

    @Deprecated
    public static void drawSideOutline(@NotNull Box box, Color color, float lineWidth, Direction dir) {
        OUTLINE_SIDE_QUEUE.add(new OutlineSideAction(box, color, lineWidth, dir));
    }

    public static void setSideOutlinePoints(Box box, MatrixStack matrices, BufferBuilder buffer, Color color, Direction dir, float width) {
        box = box.offset(new Vec3d(box.minX, box.minY, box.minZ).negate());

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        switch (dir) {
            case UP -> {
                vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color, width);
                vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color, width);
                vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color, width);
                vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color, width);
            }
            case DOWN -> {
                vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color, width);
                vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color, width);
                vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color, width);
                vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color, width);
            }
            case EAST -> {
                vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color, width);
                vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color, width);
                vertexLine(matrices, buffer, x2, y2, z2, x2, y2, z1, color, width);
                vertexLine(matrices, buffer, x2, y1, z2, x2, y1, z1, color, width);
            }
            case WEST -> {
                vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color, width);
                vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color, width);
                vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color, width);
                vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color, width);
            }
            case NORTH -> {
                vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color, width);
                vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color, width);
                vertexLine(matrices, buffer, x2, y1, z1, x1, y1, z1, color, width);
                vertexLine(matrices, buffer, x2, y2, z1, x1, y2, z1, color, width);
            }
            case SOUTH -> {
                vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color, width);
                vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color, width);
                vertexLine(matrices, buffer, x1, y1, z2, x2, y1, z2, color, width);
                vertexLine(matrices, buffer, x1, y2, z2, x2, y2, z2, color, width);
            }
        }
    }

    public static void drawHoleOutline(@NotNull Box box, Color color, float lineWidth) {
        setupRender();
        MatrixStack matrices = matrixFrom(box.minX, box.minY, box.minZ);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);

        GlStateManager._disableCull();

        box = box.offset(new Vec3d(box.minX, box.minY, box.minZ).negate());

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float y2 = (float) box.maxY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float z2 = (float) box.maxZ;

        vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color, width);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color, width);
        vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color, width);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color, width);

        vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color, width);
        vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color, width);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color, width);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color, width);

        Render2DEngine.endBuildingLines(buffer);
        GlStateManager._enableCull();
        endRender();
    }

    public static void vertexLine(@NotNull MatrixStack matrices, @NotNull VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, @NotNull Color lineColor, float width) {
        Matrix4f model = matrices.peek().getPositionMatrix();
        MatrixStack.Entry entry = matrices.peek();
        Vector3f normalVec = getNormal(x1, y1, z1, x2, y2, z2);
        buffer.vertex(model, x1, y1, z1).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(entry, normalVec.x(), normalVec.y(), normalVec.z()).lineWidth(width);
        buffer.vertex(model, x2, y2, z2).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(entry, normalVec.x(), normalVec.y(), normalVec.z()).lineWidth(width);
    }

    public static @NotNull Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);

        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    public static @NotNull MatrixStack matrixFrom(double x, double y, double z) {
        MatrixStack matrices = new MatrixStack();

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

        matrices.translate(x - camera.getPos().x, y - camera.getPos().y, z - camera.getPos().z);

        return matrices;
    }

    public static void setupRender() {
        // No-op on 1.21.11: blending is baked into RenderPipelines, there is no global shader state anymore.
    }

    public static void endRender() {
        // No-op on 1.21.11, see setupRender().
    }

    public static void drawTargetEsp(MatrixStack stack, @NotNull Entity target) {
        ArrayList<Vec3d> vecs = new ArrayList<>();
        ArrayList<Vec3d> vecs1 = new ArrayList<>();
        ArrayList<Vec3d> vecs2 = new ArrayList<>();

        double x = target.lastX + (target.getX() - target.lastX) * getTickDelta()
                - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = target.lastY + (target.getY() - target.lastY) * getTickDelta()
                - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double z = target.lastZ + (target.getZ() - target.lastZ) * getTickDelta()
                - mc.getEntityRenderDispatcher().camera.getPos().getZ();


        double height = target.getHeight();

        for (int i = 0; i <= 361; ++i) {
            double v = Math.sin(Math.toRadians(i));
            double u = Math.cos(Math.toRadians(i));
            Vec3d vec = new Vec3d((float) (u * 0.5f), height, (float) (v * 0.5f));
            vecs.add(vec);

            double v1 = Math.sin(Math.toRadians((i + 120) % 360));
            double u1 = Math.cos(Math.toRadians(i + 120) % 360);
            Vec3d vec1 = new Vec3d((float) (u1 * 0.5f), height, (float) (v1 * 0.5f));
            vecs1.add(vec1);

            double v2 = Math.sin(Math.toRadians((i + 240) % 360));
            double u2 = Math.cos(Math.toRadians((i + 240) % 360));
            Vec3d vec2 = new Vec3d((float) (u2 * 0.5f), height, (float) (v2 * 0.5f));
            vecs2.add(vec2);
            height -= 0.004f;
        }


        stack.push();
        stack.translate(x, y, z);
        BufferBuilder bufferBuilder;
        setupRender();
        GlStateManager._disableCull();
        GlStateManager._disableDepthTest();

        bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = stack.peek().getPositionMatrix();

        for (int j = 0; j < vecs.size() - 1; ++j) {
            float alpha = 1f - (((float) j + ((System.currentTimeMillis() - ThunderHack.initTime) / 5f)) % 360) / 60f;
            int col = Render2DEngine.injectAlpha(HudEditor.getColor((int) (j / 20f)), (int) (alpha * 255)).getRGB();
            Vec3d a = vecs.get(j);
            Vec3d b = vecs.get(j + 1);
            bufferBuilder.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(col);
            bufferBuilder.vertex(matrix, (float) a.x, (float) a.y + 0.1f, (float) a.z).color(col);
            bufferBuilder.vertex(matrix, (float) b.x, (float) b.y + 0.1f, (float) b.z).color(col);
            bufferBuilder.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(col);
        }
        Render2DEngine.endBuilding(bufferBuilder);

        bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int j = 0; j < vecs1.size() - 1; ++j) {
            float alpha = 1f - (((float) j + ((System.currentTimeMillis() - ThunderHack.initTime) / 5f)) % 360) / 60f;
            int col = Render2DEngine.injectAlpha(HudEditor.getColor((int) (j / 20f)), (int) (alpha * 255)).getRGB();
            Vec3d a = vecs1.get(j);
            Vec3d b = vecs1.get(j + 1);
            bufferBuilder.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(col);
            bufferBuilder.vertex(matrix, (float) a.x, (float) a.y + 0.1f, (float) a.z).color(col);
            bufferBuilder.vertex(matrix, (float) b.x, (float) b.y + 0.1f, (float) b.z).color(col);
            bufferBuilder.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(col);
        }
        Render2DEngine.endBuilding(bufferBuilder);

        bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int j = 0; j < vecs2.size() - 1; ++j) {
            float alpha = 1f - (((float) j + ((System.currentTimeMillis() - ThunderHack.initTime) / 5f)) % 360) / 60f;
            int col = Render2DEngine.injectAlpha(HudEditor.getColor((int) (j / 20f)), (int) (alpha * 255)).getRGB();
            Vec3d a = vecs2.get(j);
            Vec3d b = vecs2.get(j + 1);
            bufferBuilder.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(col);
            bufferBuilder.vertex(matrix, (float) a.x, (float) a.y + 0.1f, (float) a.z).color(col);
            bufferBuilder.vertex(matrix, (float) b.x, (float) b.y + 0.1f, (float) b.z).color(col);
            bufferBuilder.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(col);
        }
        Render2DEngine.endBuilding(bufferBuilder);

        GlStateManager._enableCull();
        stack.translate(-x, -y, -z);
        endRender();
        GlStateManager._enableDepthTest();
        stack.pop();
    }

    public static void renderCrosses(@NotNull Box box, Color color, float lineWidth) {
        setupRender();
        MatrixStack matrices = matrixFrom(box.minX, box.minY, box.minZ);
        GlStateManager._disableCull();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);

        box = box.offset(new Vec3d(box.minX, box.minY, box.minZ).negate());

        vertexLine(matrices, buffer, (float) box.maxX, (float) box.minY, (float) box.minZ, (float) box.minX, (float) box.minY, (float) box.maxZ, color, lineWidth);
        vertexLine(matrices, buffer, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.minY, (float) box.maxZ, color, lineWidth);

        Render2DEngine.endBuildingLines(buffer);
        GlStateManager._enableCull();
        endRender();
    }

    public static void drawSphere(MatrixStack matrix, float radius, int slices, int stacks, int color) {
        float drho = 3.1415927F / ((float) stacks);
        float dtheta = 6.2831855F / ((float) slices - 1f);
        float rho;
        float theta;
        float x;
        float y;
        float z;
        int i;
        int j;
        setupRender();
        for (i = 1; i < stacks; ++i) {
            rho = (float) i * drho;

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);

            for (j = 0; j < slices - 1; ++j) {
                theta = (float) j * dtheta;
                x = (float) (Math.cos(theta) * Math.sin(rho));
                y = (float) (Math.sin(theta) * Math.sin(rho));
                z = (float) Math.cos(rho);
                float theta1 = (float) (j + 1) * dtheta;
                float x1 = (float) (Math.cos(theta1) * Math.sin(rho));
                float y1 = (float) (Math.sin(theta1) * Math.sin(rho));
                float z1 = (float) Math.cos(rho);
                vertexLine(matrix, buffer, x * radius, y * radius, z * radius, x1 * radius, y1 * radius, z1 * radius, new Color(color), 1f);
            }
            Render2DEngine.endBuildingLines(buffer);
        }

        for (j = 0; j < slices; ++j) {
            theta = (float) j * dtheta;

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);

            for (i = 0; i < stacks; ++i) {
                rho = (float) i * drho;
                x = (float) (Math.cos(theta) * Math.sin(rho));
                y = (float) (Math.sin(theta) * Math.sin(rho));
                z = (float) Math.cos(rho);
                float rho1 = (float) (i + 1) * drho;
                float x1 = (float) (Math.cos(theta) * Math.sin(rho1));
                float y1 = (float) (Math.sin(theta) * Math.sin(rho1));
                float z1 = (float) Math.cos(rho1);
                vertexLine(matrix, buffer, x * radius, y * radius, z * radius, x1 * radius, y1 * radius, z1 * radius, new Color(color), 1f);
            }
            Render2DEngine.endBuildingLines(buffer);
        }
        endRender();
    }

    public static void drawCylinder(MatrixStack stack, final float radius, final float height, final int slices, final int stacks, int color) {

        final float da = (float) ((Math.PI * 2f) / slices);
        final float dz = height / stacks;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);

        float y = 0;

        for (int j = 0; j <= stacks; ++j) {
            for (int i = 0; i < slices; ++i) {
                final float x = (float) Math.cos(i * da);
                final float z = (float) Math.sin(i * da);
                final float x1 = (float) Math.cos((i + 1) * da);
                final float z1 = (float) Math.sin((i + 1) * da);
                vertexLine(stack, buffer, x * radius, y, z * radius, x1 * radius, y, z1 * radius, new Color(color), 1f);
            }
            y += dz;
        }

        Render2DEngine.endBuildingLines(buffer);

        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);

        for (int i = 0; i <= slices; ++i) {
            final float x = (float) Math.cos(i * da);
            final float z = (float) Math.sin(i * da);

            vertexLine(stack, buffer, x * radius, 0, z * radius, x * radius, height, z * radius, new Color(color), 1f);
        }

        Render2DEngine.endBuildingLines(buffer);
    }


    public static void drawCircle3D(MatrixStack stack, Entity ent, float radius, int color, int points, boolean hudColor, int colorOffset) {
        setupRender();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
        double x = ent.lastX + (ent.getX() - ent.lastX) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = ent.lastY + (ent.getY() - ent.lastY) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double z = ent.lastZ + (ent.getZ() - ent.lastZ) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getZ();
        stack.push();
        stack.translate(x, y, z);

        for (int i = 0; i < points; i++) {
            if (hudColor)
                color = HudEditor.getColor(i * colorOffset).getRGB();

            vertexLine(stack, bufferBuilder,
                    (float) (radius * Math.cos(i * 6.28 / points)), 0f, (float) (radius * Math.sin(i * 6.28 / points)),
                    (float) (radius * Math.cos((i + 1) * 6.28 / points)), 0f, (float) (radius * Math.sin((i + 1) * 6.28 / points)),
                    new Color(color), 1f);
        }

        Render2DEngine.endBuildingLines(bufferBuilder);
        endRender();
        stack.translate(-x, -y, -z);
        stack.pop();
    }

    public static void drawOldTargetEsp(MatrixStack stack, Entity target) {
        double cs = prevCircleStep + (circleStep - prevCircleStep) * getTickDelta();
        double prevSinAnim = absSinAnimation(cs - 0.45f);
        double sinAnim = absSinAnimation(cs);
        double x = target.lastX + (target.getX() - target.lastX) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = target.lastY + (target.getY() - target.lastY) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getY() + prevSinAnim * target.getHeight();
        double z = target.lastZ + (target.getZ() - target.lastZ) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getZ();
        double nextY = target.lastY + (target.getY() - target.lastY) * getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getY() + sinAnim * target.getHeight();
        stack.push();
        setupRender();
        GlStateManager._disableCull();
        GlStateManager._disableDepthTest();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float cos;
        float sin;
        for (int i = 0; i < 30; i++) {
            cos = (float) (x + Math.cos(i * 6.28 / 30) * target.getWidth() * 0.8);
            sin = (float) (z + Math.sin(i * 6.28 / 30) * target.getWidth() * 0.8);
            float cos1 = (float) (x + Math.cos((i + 1) * 6.28 / 30) * target.getWidth() * 0.8);
            float sin1 = (float) (z + Math.sin((i + 1) * 6.28 / 30) * target.getWidth() * 0.8);
            bufferBuilder.vertex(stack.peek().getPositionMatrix(), cos, (float) nextY, sin).color(Render2DEngine.injectAlpha(HudEditor.getColor(i), 170).getRGB());
            bufferBuilder.vertex(stack.peek().getPositionMatrix(), cos, (float) y, sin).color(Render2DEngine.injectAlpha(HudEditor.getColor(i), 0).getRGB());
            bufferBuilder.vertex(stack.peek().getPositionMatrix(), cos1, (float) y, sin1).color(Render2DEngine.injectAlpha(HudEditor.getColor(i), 0).getRGB());
            bufferBuilder.vertex(stack.peek().getPositionMatrix(), cos1, (float) nextY, sin1).color(Render2DEngine.injectAlpha(HudEditor.getColor(i), 170).getRGB());
        }
        Render2DEngine.endBuilding(bufferBuilder);
        GlStateManager._enableCull();
        endRender();
        GlStateManager._enableDepthTest();
        stack.pop();
    }

    // Kalry не пасть
    // anti yg protection
    public static void renderGhosts(int espLength, int factor, float shaking, float amplitude, Entity target) {
        Camera camera = mc.gameRenderer.getCamera();

        double tPosX = Render2DEngine.interpolate(target.lastX, target.getX(), Render3DEngine.getTickDelta()) - camera.getPos().x;
        double tPosY = Render2DEngine.interpolate(target.lastY, target.getY(), Render3DEngine.getTickDelta()) - camera.getPos().y;
        double tPosZ = Render2DEngine.interpolate(target.lastZ, target.getZ(), Render3DEngine.getTickDelta()) - camera.getPos().z;
        float iAge = (float) Render2DEngine.interpolate(target.age - 1, target.age, Render3DEngine.getTickDelta());

        GlStateManager._enableBlend();
        GlStateManager.glBlendFuncSeparate(770, 1, 1, 0);
        Render2DEngine.bindTexture(TextureStorage.firefly);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        boolean canSee = mc.player.canSee(target);

        if (canSee) {
            GlStateManager._enableDepthTest();
            GlStateManager._depthMask(false);
        } else GlStateManager._disableDepthTest();

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i <= espLength; i++) {
                double radians = Math.toRadians((((float) i / 1.5f + iAge) * factor + (j * 120)) % (factor * 360));
                double sinQuad = Math.sin(Math.toRadians(iAge * 2.5f + i * (j + 1)) * amplitude) / shaking;

                float offset = ((float) i / espLength);
                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                matrices.translate(tPosX + Math.cos(radians) * target.getWidth(), (tPosY + 1 + sinQuad), tPosZ + Math.sin(radians) * target.getWidth());
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                int color = Render2DEngine.applyOpacity(HudEditor.getColor((int) (180 * offset)), offset).getRGB();
                float scale = Math.max(0.24f * (offset), 0.2f);
                buffer.vertex(matrix, -scale, scale, 0).texture(0f, 1f).color(color);
                buffer.vertex(matrix, scale, scale, 0).texture(1f, 1f).color(color);
                buffer.vertex(matrix, scale, -scale, 0).texture(1f, 0).color(color);
                buffer.vertex(matrix, -scale, -scale, 0).texture(0, 0).color(color);
            }
        }

        Render2DEngine.bindTexture(TextureStorage.firefly);
        ThunderRenderLayers.guiTextured(TextureStorage.firefly).draw(buffer.end());

        if (canSee) {
            GlStateManager._depthMask(true);
            GlStateManager._disableDepthTest();
        } else GlStateManager._enableDepthTest();

        GlStateManager._disableBlend();
    }

    public static void updateTargetESP() {
        prevCircleStep = circleStep;
        circleStep += 0.15f;
    }

    public static double absSinAnimation(double input) {
        return Math.abs(1 + Math.sin(input)) / 2;
    }

    public static Vec3d interpolatePos(float prevposX, float prevposY, float prevposZ, float posX, float posY, float posZ) {
        double x = prevposX + ((posX - prevposX) * getTickDelta()) - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = prevposY + ((posY - prevposY) * getTickDelta()) - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double z = prevposZ + ((posZ - prevposZ) * getTickDelta()) - mc.getEntityRenderDispatcher().camera.getPos().getZ();
        return new Vec3d(x, y, z);
    }

    public static void drawLineDebug(Vec3d start, Vec3d end, Color color) {
        DEBUG_LINE_QUEUE.add(new DebugLineAction(start, end, color));
    }

    public static float getTickDelta() {
        return mc.getRenderTickCounter().getTickProgress(true);
    }

    public record FillAction(Box box, Color color) {
    }

    public record OutlineAction(Box box, Color color, float lineWidth) {
    }

    public record FadeAction(Box box, Color color, Color color2) {
    }

    public record FillSideAction(Box box, Color color, Direction side) {
    }

    public record OutlineSideAction(Box box, Color color, float lineWidth, Direction side) {
    }

    public record DebugLineAction(Vec3d start, Vec3d end, Color color) {
    }

    public record LineAction(Vec3d start, Vec3d end, Color color) {
    }
}