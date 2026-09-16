package thunder.hack.injection;

import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventKeyboardInput;
import thunder.hack.features.modules.Module;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onSneak(CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        ThunderHack.EVENT_BUS.post(new EventKeyboardInput());
    }
}
