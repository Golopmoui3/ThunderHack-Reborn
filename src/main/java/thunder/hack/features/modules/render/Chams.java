package thunder.hack.features.modules.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventHeldItemRenderer;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;

public class Chams extends Module {
    public Chams() {
        super("Chams", Category.RENDER);
    }

    public final Setting<Boolean> handItems = new Setting<>("HandItems", false);
    private final Setting<ColorSetting> handItemsColor = new Setting<>("HandItemsColor", new ColorSetting(new Color(0x9317DE5D, true)), v -> handItems.getValue());

    public final Setting<Boolean> crystals = new Setting<>("Crystals", false);
    private final Setting<ColorSetting> crystalColor = new Setting<>("CrystalColor", new ColorSetting(new Color(0x932DD8E8, true)), v -> crystals.getValue());
    private final Setting<Boolean> staticCrystal = new Setting<>("StaticCrystal", true, v -> crystals.getValue());
    private final Setting<CMode> crystalMode = new Setting<>("CrystalMode", CMode.One, v -> crystals.getValue());

    public final Setting<Boolean> players = new Setting<>("Players", false);
    private final Setting<ColorSetting> playerColor = new Setting<>("PlayerColor", new ColorSetting(new Color(0x932DD8E8, true)), v -> players.getValue());
    private final Setting<ColorSetting> friendColor = new Setting<>("FriendColor", new ColorSetting(new Color(0x932DE830, true)), v -> players.getValue());
    private final Setting<Boolean> playerTexture = new Setting<>("PlayerTexture", true, v -> players.getValue());
    private final Setting<Boolean> simple = new Setting<>("Simple", false, v -> players.getValue());

    public boolean playerTexture() {
        return players.getValue() && playerTexture.getValue();
    }

    private final Setting<Boolean> alternativeBlending = new Setting<>("AlternativeBlending", true);

    private enum CMode {
        One, Two, Three
    }

    private final Identifier crystalTexture = Identifier.of("textures/entity/end_crystal/end_crystal.png");
    private static final float SINE_45_DEGREES = (float) Math.sin(0.7853981633974483);

    public void renderCrystal(EndCrystalEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, EndCrystalEntityModel model) {
        Identifier tex = crystalMode.getValue() == CMode.Three
                ? Identifier.ofVanilla("textures/entity/end_crystal/end_crystal.png")
                : TextureStorage.crystalTexture2;
        matrices.push();
        if (!staticCrystal.getValue())
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.age * 3f));
        matrices.scale(2f, 2f, 2f);
        matrices.translate(0f, -0.5f, 0f);
        model.setAngles(state);
        queue.submitModel(model, state, matrices, RenderLayers.entityTranslucent(tex, false),
                state.light, OverlayTexture.DEFAULT_UV, crystalColor.getValue().getColorObject().getRGB(), null);
        matrices.pop();
    }

    public void renderPlayer(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, PlayerEntityModel model, PlayerEntity pe) {
        if (simple.getValue()) return; // TODO(1.21.11): white silhouette needs a dedicated untextured layer
        model.setAngles(state);
        Identifier skin = state.skinTextures.body().texturePath();
        int color = (pe != null && Managers.FRIEND.isFriend(pe) ? friendColor : playerColor).getValue().getColorObject().getRGB();
        queue.submitModel(model, state, matrices, RenderLayers.entityTranslucent(skin, false),
                state.light, OverlayTexture.DEFAULT_UV, color, null);
    }

    @EventHandler
    public void onRenderHands(EventHeldItemRenderer e) {
        // TODO(1.21.11): hand tinting used the removed global shader color; needs a custom hand-item layer
    }
}