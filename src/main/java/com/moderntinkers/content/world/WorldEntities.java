package com.moderntinkers.content.world;

import com.moderntinkers.ModernTinkers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom slime entities and their server-side spawn contracts. */
public final class WorldEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ModernTinkers.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SkySlimeEntity>> SKY_SLIME =
            register("sky_slime", SkySlimeEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<EnderSlimeEntity>> ENDER_SLIME =
            register("ender_slime", EnderSlimeEntity::new);
    public static final DeferredHolder<EntityType<?>, EntityType<TerracubeEntity>> TERRACUBE =
            register("terracube", TerracubeEntity::new);

    private WorldEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(WorldEntities::registerAttributes);
        bus.addListener(WorldEntities::registerSpawnPlacements);
    }

    private static <T extends net.minecraft.world.entity.Entity> DeferredHolder<EntityType<?>, EntityType<T>>
    register(String id, EntityType.EntityFactory<T> factory) {
        return ENTITIES.register(id, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(2.04F, 2.04F)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(ModernTinkers.MOD_ID + ":" + id));
    }

    private static void registerAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = Monster.createMonsterAttributes().build();
        event.put(SKY_SLIME.get(), attributes);
        event.put(ENDER_SLIME.get(), Monster.createMonsterAttributes().build());
        event.put(TERRACUBE.get(), Monster.createMonsterAttributes().build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(SKY_SLIME.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new SlimePlacementPredicate<>(WorldContent.SKY_SLIME_SPAWN),
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(ENDER_SLIME.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new SlimePlacementPredicate<>(WorldContent.ENDER_SLIME_SPAWN),
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(TERRACUBE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                TerracubeEntity::canSpawnHere,
                RegisterSpawnPlacementsEvent.Operation.OR);
    }
}
