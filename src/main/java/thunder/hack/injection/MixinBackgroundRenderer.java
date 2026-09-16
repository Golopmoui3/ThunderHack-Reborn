package thunder.hack.injection;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class MixinBackgroundRenderer {
    // TODO(1.21.11): fog is UBO-based now (FogData/GpuBufferSlice); NoRender.fog/blindness and
    //  WorldTweaks.fogModify need a FogData/UBO hook instead of RenderSystem.setShaderFog*.
    @Inject(method = "applyFog", at = @At("TAIL"))
    private void onApplyFog(Camera camera, int viewDistance, RenderTickCounter tickCounter, float skyDarkness, ClientWorld world, CallbackInfoReturnable<Vector4f> info) {
    }
}
