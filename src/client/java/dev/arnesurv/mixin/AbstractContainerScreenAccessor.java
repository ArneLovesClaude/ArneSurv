package dev.arnesurv.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Zugriff auf Position/Größe des Inventarfensters, um Knöpfe daneben zu setzen. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int arnesurv$getLeftPos();

	@Accessor("topPos")
	int arnesurv$getTopPos();

	@Accessor("imageWidth")
	int arnesurv$getImageWidth();
}
