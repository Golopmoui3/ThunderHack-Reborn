package thunder.hack.injection.accesors;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DrawContext.class)
public interface IDrawContextAccessor {
    @Accessor("state")
    GuiRenderState thunderhack$getRenderState();
}
