package thunder.hack.injection;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventEatFood;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void finishUsingHook(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        ThunderHack.EVENT_BUS.post(new EventEatFood(cir.getReturnValue()));
    }
}
