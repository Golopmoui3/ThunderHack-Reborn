package thunder.hack.utility.render;

import net.minecraft.client.gl.Framebuffer;
import org.jetbrains.annotations.NotNull;

/**
 * MSAA helper.
 * <p>
 * TODO(1.21.11): the old implementation poked raw GL frame/renderbuffer ids, but
 * 1.21.11 Framebuffers are GpuTexture-based. Port to multisampled GpuTextures
 * (see RadarRewrite usage). For now this is a pass-through so the game runs.
 */
public class MSAAFramebuffer {
    public static void use(boolean fancy, Runnable drawAction) {
        drawAction.run();
    }

    public static void use(int samples, @NotNull Framebuffer mainBuffer, @NotNull Runnable drawAction) {
        drawAction.run();
    }
}
