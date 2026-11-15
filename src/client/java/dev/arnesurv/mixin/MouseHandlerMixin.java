package dev.arnesurv.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.arnesurv.comfort.Zoom;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Beim Zoomen: Maus langsamer, Mausrad ändert die Zoomstärke. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@ModifyExpressionValue(method = "turnPlayer", at = @At(value = "INVOKE", target = "Ljava/lang/Double;doubleValue()D", ordinal = 0))
	private double arnesurv$zoomSensitivity(double sensitivity) {
		return sensitivity * Zoom.sensitivityFactor();
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void arnesurv$zoomScroll(long window, double x, double y, CallbackInfo ci) {
		if (Zoom.onScroll(y)) ci.cancel();
	}
}
