package dev.arnesurv.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.arnesurv.comfort.Zoom;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Zoom: verkleinert das Sichtfeld. */
@Mixin(Camera.class)
public class CameraMixin {
	@ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
	private float arnesurv$zoom(float fov) {
		return Zoom.applyFov(fov);
	}
}
