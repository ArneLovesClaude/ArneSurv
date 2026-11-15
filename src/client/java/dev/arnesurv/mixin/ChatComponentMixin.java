package dev.arnesurv.mixin;

import dev.arnesurv.comfort.ChatTweaks;
import dev.arnesurv.config.ModConfig;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Chat: Zeitstempel/Erwähnungen/Waypoint-Links und längerer Verlauf. */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {
	@ModifyVariable(
		method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
		at = @At("HEAD"), argsOnly = true)
	private Component arnesurv$process(Component message) {
		return ChatTweaks.process(message);
	}

	@ModifyConstant(method = {"addMessageToDisplayQueue", "addMessageToQueue"}, constant = @Constant(intValue = 100))
	private int arnesurv$longerHistory(int original) {
		return Math.max(original, ModConfig.get().chatHistoryLines);
	}
}
