package thunder.hack.injection;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventEatFood;
import thunder.hack.features.modules.render.Tooltips;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void finishUsingHook(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        ThunderHack.EVENT_BUS.post(new EventEatFood(cir.getReturnValue()));
    }

    @Inject(method = "appendComponentTooltip", at = @At("HEAD"), cancellable = true)
    private void onAppendComponentTooltip(ComponentType<?> componentType, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> consumer, TooltipType type, CallbackInfo ci) {
        if (componentType != DataComponentTypes.CONTAINER || ModuleManager.tooltips == null) return;
        ItemStack self = (ItemStack) (Object) this;
        if (Tooltips.storage.getValue() && self.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock) {
            ci.cancel();
        }
    }
}