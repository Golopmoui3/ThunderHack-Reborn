package thunder.hack.injection;

import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    // TODO(1.21.11): WorldRenderer was rewritten around WorldRenderState/FrameGraph:
    //  - the old setupTerrain spectator hook (FreeCam) needs a new injection point
    //  - the PostEffectProcessor.render(F) redirect (Shaders module) needs a PostEffectPipeline port
    //  - the renderWeather hook (NoRender.noWeather) needs a weather-state hook
}
