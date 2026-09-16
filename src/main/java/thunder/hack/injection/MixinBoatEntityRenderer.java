package thunder.hack.injection;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.AbstractBoatEntityRenderer;
import net.minecraft.client.render.entity.state.BoatEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(AbstractBoatEntityRenderer.class)
public class MixinBoatEntityRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    public void renderHook(BoatEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        if (ModuleManager.boatFly.isEnabled() && ModuleManager.boatFly.hideBoat.getValue())
            matrices.scale(0.3f, 0.3f, 0.3f);
    }
}
