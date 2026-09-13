package thunder.hack.injection.accesors;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderLayer.class)
public interface IRenderLayerFactory {
    @Invoker("of")
    static RenderLayer thunderhack$of(String name, RenderSetup setup) {
        throw new AssertionError();
    }
}
