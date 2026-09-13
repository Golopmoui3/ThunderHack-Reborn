package thunder.hack.utility.render;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;
import thunder.hack.injection.accesors.IRenderLayerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * RenderLayers for ThunderHack's immediate-mode renderer on 1.21.11.
 * <p>
 * Vanilla 1.21.11 has no stock RenderLayer with the GUI textured pipeline,
 * so we build them via RenderSetup (public API) + an invoker for the
 * package-private RenderLayer.of factory.
 */
public final class ThunderRenderLayers {
    private ThunderRenderLayers() {
    }

    private static final Map<Identifier, RenderLayer> GUI_TEXTURED = new HashMap<>();

    /**
     * GUI textured layer (POSITION_TEX_COLOR, no depth test, translucent)
     * for the given texture, e.g. icons, glyph atlas pages, blurred shadows.
     */
    public static RenderLayer guiTextured(Identifier texture) {
        return GUI_TEXTURED.computeIfAbsent(texture, id ->
                IRenderLayerFactory.thunderhack$of("th_gui_textured",
                        RenderSetup.builder(RenderPipelines.GUI_TEXTURED).texture("Sampler0", id).build()));
    }
}
