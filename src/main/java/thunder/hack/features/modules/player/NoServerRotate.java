package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;

public class NoServerRotate extends Module {
    public NoServerRotate() {
        super("NoServerRotate", Category.PLAYER);
    }

    private float prevYaw, prevPitch;

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (fullNullCheck()) return;
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket && mc.player != null) {
            prevYaw = mc.player.getYaw();
            prevPitch = mc.player.getPitch();
        }
    }

    @EventHandler
    public void onPacketReceivePost(PacketEvent.ReceivePost e) {
        if (fullNullCheck()) return;
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket && mc.player != null) {
            // 1.21.11 made the packet an immutable record - restore our rotation after vanilla applies it
            mc.player.setYaw(prevYaw);
            mc.player.setPitch(prevPitch);
        }
    }
}
