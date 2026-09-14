package thunder.hack.injection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.ClientSettings;

import static thunder.hack.features.modules.Module.mc;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<S>> {
    private float originalHeadYaw, originalPrevHeadYaw, originalPrevHeadPitch, originalHeadPitch;

    @Shadow
    protected M model;

    @Inject(method = "updateRenderState", at = @At("HEAD"))
    private void updateRenderStatePre(T livingEntity, S livingEntityRenderState, float f, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if (mc.player != null && livingEntity == mc.player && mc.player.getControllingVehicle() == null && ClientSettings.renderRotations.getValue() && !ThunderHack.isFuturePresent()) {
            originalHeadYaw = livingEntity.headYaw;
            originalPrevHeadYaw = livingEntity.lastHeadYaw;
            originalPrevHeadPitch = livingEntity.lastPitch;
            originalHeadPitch = livingEntity.getPitch();

            livingEntity.setPitch(((IClientPlayerEntity) MinecraftClient.getInstance().player).getLastPitch());
            livingEntity.lastPitch = Managers.PLAYER.lastPitch;
            livingEntity.headYaw = ((IClientPlayerEntity) MinecraftClient.getInstance().player).getLastYaw();
            livingEntity.lastHeadYaw = Managers.PLAYER.lastYaw;
            livingEntity.lastBodyYaw = Managers.PLAYER.lastYaw;
        }
        if (ModuleManager.serverHelper.isEnabled() && ModuleManager.serverHelper.trueSight.getValue()
                && livingEntity instanceof PlayerEntity && livingEntity.isInvisible()) {
            // TODO(1.21.11): trueSight reveals invisibles fully opaque for now (no per-pixel alpha in state pipeline)
            livingEntityRenderState.invisible = false;
            livingEntityRenderState.invisibleToPlayer = false;
        }
        if (ModuleManager.freeCam.isEnabled() && ModuleManager.freeCam.track.getValue()
                && ModuleManager.freeCam.trackEntity != null && ModuleManager.freeCam.trackEntity == livingEntity) {
            // TODO(1.21.11): verify no outline renders for the hidden tracked entity
            livingEntityRenderState.invisible = true;
            livingEntityRenderState.invisibleToPlayer = true;
        }
    }

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void updateRenderStatePost(T livingEntity, S livingEntityRenderState, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if (mc.player != null && livingEntity == mc.player && mc.player.getControllingVehicle() == null && ClientSettings.renderRotations.getValue() && !ThunderHack.isFuturePresent()) {
            livingEntity.lastPitch = originalPrevHeadPitch;
            livingEntity.setPitch(originalHeadPitch);
            livingEntity.headYaw = originalHeadYaw;
            livingEntity.lastHeadYaw = originalPrevHeadYaw;
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderPre(S livingEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if (livingEntityRenderState instanceof PlayerEntityRenderState playerState
                && ModuleManager.chams.isEnabled() && ModuleManager.chams.players.getValue()) {
            PlayerEntity pe = null;
            if (mc.world != null) {
                for (PlayerEntity p : mc.world.getPlayers()) {
                    if (p.getId() == playerState.id) {
                        pe = p;
                        break;
                    }
                }
            }
            //noinspection unchecked
            ModuleManager.chams.renderPlayer(playerState, matrixStack, orderedRenderCommandQueue, (PlayerEntityModel) model, pe);
            if (!ModuleManager.chams.playerTexture() && pe != null && !pe.isSpectator())
                ci.cancel();
        }
    }
}
