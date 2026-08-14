package com.moderntinkers.content.tools;

import com.moderntinkers.ModernTinkers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Entity registration for stack-preserving tool projectiles. */
public final class ProjectileContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ModernTinkers.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ModifiableArrowEntity>>
            MODIFIABLE_ARROW = ENTITIES.register("modifiable_arrow", () -> EntityType.Builder
                    .of(ModifiableArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                    .build("moderntinkers:modifiable_arrow"));
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownToolEntity>>
            THROWN_TOOL = ENTITIES.register("thrown_tool", () -> EntityType.Builder
                    .of(ThrownToolEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                    .build("moderntinkers:thrown_tool"));
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownShurikenEntity>>
            THROWN_SHURIKEN = ENTITIES.register("thrown_shuriken", () -> EntityType.Builder
                    .of(ThrownShurikenEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                    .build("moderntinkers:thrown_shuriken"));

    private ProjectileContent() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
