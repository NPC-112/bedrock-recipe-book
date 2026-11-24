package dev.bedrockrecipebook.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Contains no Controlify references, so the base mod can load without that optional mod. */
public final class ControlifyMixinPlugin implements IMixinConfigPlugin {
	@Override public void onLoad(String mixinPackage) {}
	@Override public String getRefMapperConfig() { return null; }
	@Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return FabricLoader.getInstance().isModLoaded("controlify");
	}
	@Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
	@Override public List<String> getMixins() { return null; }
	@Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
	@Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
