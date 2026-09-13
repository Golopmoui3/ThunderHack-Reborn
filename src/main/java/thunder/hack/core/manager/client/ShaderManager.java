package thunder.hack.core.manager.client;

import thunder.hack.core.manager.IManager;
import thunder.hack.features.modules.render.Shaders;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-processing shader manager.
 *
 * <p>TODO(1.21.11): the old satin-based post pipeline (PostEffectProcessor with
 * bufIn/bufOut fake targets) no longer exists - 1.21.11 uses PostEffectPipeline
 * + FrameGraph. Entity outline/glow shaders will be reimplemented on top of
 * {@code ShaderLoader.loadPostEffect}. Until then every method below is a safe
 * no-op and the Shaders module has no visual effect.
 */
public class ShaderManager implements IManager {
    private final static List<RenderTask> tasks = new ArrayList<>();

    public float time = 0;

    public void renderShader(Runnable runnable, Shader mode) {
        tasks.add(new RenderTask(runnable, mode));
    }

    public void renderShaders() {
        tasks.clear();
    }

    public void applyShader(Runnable runnable, Shader mode) {
    }

    public void setupShader(Shader shader, Object effect) {
    }

    public void reloadShaders() {
    }

    public boolean fullNullCheck() {
        return false;
    }

    public record RenderTask(Runnable task, Shader shader) {
    }

    public enum Shader {
        Default,
        Smoke,
        Gradient,
        Snow,
        Fade
    }
}
