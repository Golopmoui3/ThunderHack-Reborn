package thunder.hack.injection;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventHeldItemRenderer;
import thunder.hack.features.modules.Module;

import static thunder.hack.features.modules.Module.mc;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {

    @Inject(method = "renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IFLnet/minecraft/util/Arm;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderArmHoldingItem(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, float equipProgress, float swingProgress, Arm arm, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        ClientPlayerEntity player = mc.player;
        Hand hand = arm == player.getMainArm() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        EventHeldItemRenderer event = new EventHeldItemRenderer(hand, player.getStackInHand(hand), equipProgress, matrices);
        ThunderHack.EVENT_BUS.post(event);

        if (Managers.MODULE != null && ModuleManager.animations.shouldAnimate()) {
            ci.cancel();
            ModuleManager.animations.renderSwordAnimation(matrices, 0f, swingProgress, equipProgress, arm);
            PlayerEntityRenderer<AbstractClientPlayerEntity> renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(player);
            net.minecraft.util.Identifier skinId = player.getSkin().body().texturePath();
            if (arm != Arm.LEFT)
                renderer.renderRightArm(matrices, queue, light, skinId, player.isModelPartVisible(PlayerModelPart.RIGHT_SLEEVE));
            else
                renderer.renderLeftArm(matrices, queue, light, skinId, player.isModelPartVisible(PlayerModelPart.LEFT_SLEEVE));
        }
    }

    @ModifyVariable(method = "renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IFLnet/minecraft/util/Arm;)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float noSwingHook(float swingProgress) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noSwing.getValue()) return 0f;
        return swingProgress;
    }
}
