package thunder.hack.injection;

import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventMouse;

import static thunder.hack.features.modules.Module.mc;

@Mixin(Mouse.class)
public class MixinMouse {
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    public void onMouseButtonHook(long window, MouseInput buttonInfo, int action, CallbackInfo ci) {
        if (window == mc.getWindow().getHandle()) {
            int button = buttonInfo.button();
            if (action == 0) Managers.MODULE.onMoseKeyReleased(button);
            if (action == 1) Managers.MODULE.onMoseKeyPressed(button);

            ThunderHack.EVENT_BUS.post(new EventMouse(button, action));
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window == mc.getWindow().getHandle()) {
            ThunderHack.EVENT_BUS.post(new EventMouse((int) vertical, 2));
        }
    }
}