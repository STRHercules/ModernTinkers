package com.moderntinkers;

import com.moderntinkers.content.MaterialItems;
import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.material.MaterialManager;
import com.moderntinkers.content.fluid.MaterialFluids;
import com.moderntinkers.content.recipe.TinkerRecipeManager;
import com.moderntinkers.content.smeltery.SmelteryContent;
import com.moderntinkers.content.tools.TinkersToolEvents;
import com.moderntinkers.content.tools.ProjectileContent;
import com.moderntinkers.content.world.WorldContent;
import com.moderntinkers.content.world.WorldEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(ModernTinkers.MOD_ID)
public final class ModernTinkers {
    public static final String MOD_ID = "moderntinkers";

    public ModernTinkers(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(MaterialManager::addReloadListener);
        NeoForge.EVENT_BUS.addListener(TinkerRecipeManager::addReloadListener);
        NeoForge.EVENT_BUS.addListener(TinkersToolEvents::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(TinkersToolEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(TinkersToolEvents::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(TinkersToolEvents::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(TinkersToolEvents::onLivingExperience);
        NeoForge.EVENT_BUS.addListener(TinkersToolEvents::onArmorHurt);
        NeoForge.EVENT_BUS.addListener(TinkersToolEvents::onShieldBlock);
        NeoForge.EVENT_BUS.addListener(TinkersToolEvents::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(TinkersToolEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(TinkersToolEvents::onItemFished);
        NeoForge.EVENT_BUS.addListener(TinkersToolEvents::onLivingTick);
        modEventBus.addListener(SmelteryContent::registerCapabilities);
        MaterialItems.register(modEventBus);
        ProjectileContent.register(modEventBus);
        StaticContent.register(modEventBus);
        WorldContent.register(modEventBus);
        WorldEntities.register(modEventBus);
        MaterialFluids.register(modEventBus);
        SmelteryContent.register(modEventBus);
    }
}
