package thunder.hack.injection;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventHeldItemRenderer;
import thunder.hack.features.modules.Module;

import static thunder.hack.features.modules.Module.mc;

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinHeldItemRenderer {

    @Unique
    private static boolean renderingArm = false;

    @Inject(method = "renderRightArm", at = @At("HEAD"), cancellable = true)
    private void onRenderRightArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinId, boolean sleeve, CallbackInfo ci) {
        if (renderingArm || Module.fullNullCheck()) return;
        if (renderArmHook(Hand.MAIN_HAND, Arm.RIGHT, matrices, queue, light, skinId, sleeve)) ci.cancel();
    }

    @Inject(method = "renderLeftArm", at = @At("HEAD"), cancellable = true)
    private void onRenderLeftArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinId, boolean sleeve, CallbackInfo ci) {
        if (renderingArm || Module.fullNullCheck()) return;
        if (renderArmHook(Hand.OFF_HAND, Arm.LEFT, matrices, queue, light, skinId, sleeve)) ci.cancel();
    }

    private boolean renderArmHook(Hand hand, Arm arm, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinId, boolean sleeve) {
        EventHeldItemRenderer event = new EventHeldItemRenderer(hand, mc.player.getStackInHand(hand), 0f, matrices);
        ThunderHack.EVENT_BUS.post(event);

        if (Managers.MODULE != null && ModuleManager.animations.shouldAnimate()) {
            ModuleManager.animations.renderSwordAnimation(matrices, 0f, mc.player.handSwingProgress, 0f, arm);
            renderingArm = true;
            try {
                PlayerEntityRenderer<AbstractClientPlayerEntity> renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player);
                if (arm != Arm.LEFT)
                    renderer.renderRightArm(matrices, queue, light, skinId, mc.player.isModelPartVisible(PlayerModelPart.RIGHT_SLEEVE));
                else
                    renderer.renderLeftArm(matrices, queue, light, skinId, mc.player.isModelPartVisible(PlayerModelPart.LEFT_SLEEVE));
            } finally {
                renderingArm = false;
            }
            return true;
        }
        return false;
    }
}
