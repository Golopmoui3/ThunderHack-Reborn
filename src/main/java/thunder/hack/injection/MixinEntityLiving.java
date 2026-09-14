package thunder.hack.injection;

import net.minecraft.block.FluidBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventPlayerJump;
import thunder.hack.events.impl.EventTravel;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.features.modules.render.Animations;
import thunder.hack.utility.interfaces.IEntityLiving;

import java.util.ArrayList;
import java.util.List;

import static thunder.hack.features.modules.Module.mc;
import static thunder.hack.features.modules.movement.WaterSpeed.Mode.CancelResurface;

@Mixin(LivingEntity.class)
public class MixinEntityLiving implements IEntityLiving {
    @Unique
    double prevServerX, prevServerY, prevServerZ;

    @Unique
    public List<Aura.Position> positonHistory = new ArrayList<>();

    @Override
    public List<Aura.Position> getPositionHistory() {
        return positonHistory;
    }

    @Inject(method = "getHandSwingDuration", at = {@At("HEAD")}, cancellable = true)
    private void getArmSwingAnimationEnd(final CallbackInfoReturnable<Integer> info) {
        if (!ModuleManager.noRender.noSwing.getValue() && ModuleManager.animations.shouldChangeAnimationDuration() && Animations.slowAnimation.getValue())
            info.setReturnValue(Animations.slowAnimationVal.getValue());
    }

    @Inject(method = {"tickMovement"}, at = {@At("HEAD")})
    private void updateTrackedPositionAndAnglesHook(CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if (!((Object) this instanceof PlayerEntity self)) return;
        prevServerX = self.getX();
        prevServerY = self.getY();
        prevServerZ = self.getZ();
        positonHistory.add(new Aura.Position(self.getX(), self.getY(), self.getZ()));
        positonHistory.removeIf(Aura.Position::shouldRemove);
    }

    @Inject(method = "jump", at = @At("HEAD"))
    private void onJumpPre(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity)
            ThunderHack.EVENT_BUS.post(new EventPlayerJump(true));
    }

    @Inject(method = "jump", at = @At("RETURN"))
    private void onJumpPost(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity)
            ThunderHack.EVENT_BUS.post(new EventPlayerJump(false));
    }

    @Override
    public double getPrevServerX() {
        return prevServerX;
    }

    @Override
    public double getPrevServerY() {
        return prevServerY;
    }

    @Override
    public double getPrevServerZ() {
        return prevServerZ;
    }

    @Unique
    private boolean prevFlying = false;

    @Inject(method = "isGliding", at = @At("TAIL"), cancellable = true)
    public void isFallFlyingHook(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.elytraRecast.isEnabled()) {
            boolean elytra = cir.getReturnValue();
            if (prevFlying && !cir.getReturnValue()) {
                cir.setReturnValue(ModuleManager.elytraRecast.castElytra());
            }
            prevFlying = elytra;
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void travelHook(Vec3d movementInput, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if ((LivingEntity) (Object) this != mc.player) return;
        final EventTravel event = new EventTravel(mc.player.getVelocity(), true);
        ThunderHack.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            mc.player.move(MovementType.SELF, event.getmVec());
            ci.cancel();
        }
    }

    @Inject(method = "travel", at = @At("RETURN"), cancellable = true)
    public void travelPostHook(Vec3d movementInput, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if ((LivingEntity) (Object) this != mc.player) return;
        final EventTravel event = new EventTravel(movementInput, false);
        ThunderHack.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            mc.player.move(MovementType.SELF, mc.player.getVelocity());
            ci.cancel();
        }
    }

    @ModifyVariable(method = "setSprinting", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private boolean setSprintingHook(boolean sprinting) {
        if (mc.player != null && mc.world != null && ModuleManager.waterSpeed.isEnabled() && ModuleManager.waterSpeed.mode.is(CancelResurface)) {
            if (mc.player.isTouchingWater() || mc.world.getBlockState(BlockPos.ofFloored(mc.player.getEntityPos().add(0, -0.5, 0))).getBlock() instanceof FluidBlock)
                return true;
        }
        return sprinting;
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void onGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noSwing.getValue()) {
            cir.setReturnValue(0);
            cir.cancel();
        }
    }
}