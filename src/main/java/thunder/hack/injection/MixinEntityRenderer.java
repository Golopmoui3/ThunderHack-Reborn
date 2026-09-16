package thunder.hack.injection;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void renderLabelIfPresent(EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo info) {
        if (state.entityType == EntityType.ARMOR_STAND && ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noArmorStands.getValue())
            info.cancel();

        if (state.entityType == EntityType.PLAYER && ModuleManager.nameTags.isEnabled())
            info.cancel();
    }
}
