package dev.cudzer.cobblemonsizevariation.compat;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class BreedingMixinPlugin implements IMixinConfigPlugin {
    private boolean enabled;
    @Override public void onLoad(String mixinPackage) {
        var mod = FabricLoader.getInstance().getModContainer("cobbreeding");
        enabled = mod.map(container -> container.getMetadata().getVersion().getFriendlyString().equals("2.2.2")).orElse(false);
        if (mod.isPresent() && !enabled)
            org.slf4j.LoggerFactory.getLogger("cobblemonsizevariation").warn("Parent size inheritance supports Cobbreeding 2.2.2; leaving this version's breeding unchanged");
    }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return enabled; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
