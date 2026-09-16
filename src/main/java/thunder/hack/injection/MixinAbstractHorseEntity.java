package thunder.hack.injection;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(AbstractHorseEntity.class)
public abstract class MixinAbstractHorseEntity extends AnimalEntity {
    protected MixinAbstractHorseEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "getControlledMovementInput", at = @At("HEAD"), cancellable = true)
    protected void onControlledMovementInput(PlayerEntity player, Vec3d movementInput, CallbackInfoReturnable<Vec3d> cir) {
        if (ModuleManager.entityControl.isEnabled())
            cir.setReturnValue(movementInput);
    }
}
