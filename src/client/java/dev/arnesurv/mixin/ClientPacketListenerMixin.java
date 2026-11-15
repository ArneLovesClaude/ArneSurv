package dev.arnesurv.mixin;

import dev.arnesurv.stats.StatsRecorder;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Statistiken vom Server mitlesen (dieselben, die man unter Escape → Statistiken sieht). */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(method = "handleAwardStats", at = @At("TAIL"))
	private void arnesurv$recordStats(ClientboundAwardStatsPacket packet, CallbackInfo ci) {
		StatsRecorder.onStats(packet.stats());
	}
}
