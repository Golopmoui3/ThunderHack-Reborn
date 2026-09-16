package thunder.hack.injection;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.render.block.entity.AbstractSignBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.SignBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(AbstractSignBlockEntityRenderer.class)
public class MixinSignBlockEntityRenderer {
    @Inject(method = "updateRenderState", at = {@At("TAIL")})
    public void updateRenderStateHook(SignBlockEntity entity, SignBlockEntityRenderState state, float tickDelta, Vec3d pos, ModelCommandRenderer.CrumblingOverlayCommand overlay, CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.signText.getValue()) {
            state.frontText = new SignText();
            state.backText = new SignText();
        }
    }
}
