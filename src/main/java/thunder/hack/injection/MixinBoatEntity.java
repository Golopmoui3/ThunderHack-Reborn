package thunder.hack.injection;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(Entity.class)
public class MixinBoatEntity {

    @Unique
    private float prevYaw, prevHeadYaw;

    @Inject(method = "updatePassengerPosition(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity$PositionUpdater;)V", at = @At("HEAD"))
    protected void updatePassengerPositionHookPre(Entity passenger, Entity.PositionUpdater positionUpdater, CallbackInfo ci) {
        if ((Object) this instanceof BoatEntity && ModuleManager.boatFly.isEnabled()) {
            prevYaw = passenger.getYaw();
            prevHeadYaw = passenger.getHeadYaw();
        }
    }

    @Inject(method = "updatePassengerPosition(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity$PositionUpdater;)V", at = @At("RETURN"))
    protected void updatePassengerPositionHookPost(Entity passenger, Entity.PositionUpdater positionUpdater, CallbackInfo ci) {
        if ((Object) this instanceof BoatEntity && ModuleManager.boatFly.isEnabled()) {
            passenger.setYaw(prevYaw);
            passenger.setHeadYaw(prevHeadYaw);
        }
    }
}
