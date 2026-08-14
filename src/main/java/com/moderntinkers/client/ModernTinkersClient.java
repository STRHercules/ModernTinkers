package com.moderntinkers.client;

import com.moderntinkers.ModernTinkers;
import com.moderntinkers.client.screen.PartBuilderScreen;
import com.moderntinkers.client.screen.TinkerStationScreen;
import com.moderntinkers.client.screen.TinkersAnvilScreen;
import com.moderntinkers.client.screen.ModifierWorktableScreen;
import com.moderntinkers.client.screen.MelterScreen;
import com.moderntinkers.client.screen.CastingScreen;
import com.moderntinkers.client.screen.AlloyerScreen;
import com.moderntinkers.client.screen.SmelteryScreen;
import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.material.MaterialManager;
import com.moderntinkers.content.tools.MaterialPartItem;
import com.moderntinkers.content.tools.TinkersArmorItem;
import com.moderntinkers.content.tools.TinkersArrowItem;
import com.moderntinkers.content.tools.TinkersShieldItem;
import com.moderntinkers.content.tools.TinkersToolItem;
import com.moderntinkers.content.tools.ProjectileContent;
import com.moderntinkers.content.world.WorldEntities;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import com.moderntinkers.content.smeltery.SmelteryContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.SlimeRenderer;

/** Client-only menu presentation for the common table contracts. */
@Mod(value = ModernTinkers.MOD_ID, dist = Dist.CLIENT)
public final class ModernTinkersClient {
    public ModernTinkersClient(IEventBus modEventBus) {
        modEventBus.addListener(ModernTinkersClient::registerScreens);
        modEventBus.addListener(ModernTinkersClient::registerItemColors);
        modEventBus.addListener(ModernTinkersClient::registerEntityRenderers);
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ProjectileContent.MODIFIABLE_ARROW.get(),
                StackProjectileRenderer::new);
        event.registerEntityRenderer(ProjectileContent.THROWN_TOOL.get(),
                StackProjectileRenderer::new);
        event.registerEntityRenderer(ProjectileContent.THROWN_SHURIKEN.get(),
                StackProjectileRenderer::new);
        event.registerEntityRenderer(WorldEntities.SKY_SLIME.get(), SlimeRenderer::new);
        event.registerEntityRenderer(WorldEntities.ENDER_SLIME.get(), SlimeRenderer::new);
        event.registerEntityRenderer(WorldEntities.TERRACUBE.get(), SlimeRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(StaticContent.PART_BUILDER_MENU.get(), PartBuilderScreen::new);
        event.register(StaticContent.TINKER_STATION_MENU.get(), TinkerStationScreen::new);
        event.register(StaticContent.MODIFIER_WORKTABLE_MENU.get(), ModifierWorktableScreen::new);
        event.register(StaticContent.TINKERS_ANVIL_MENU.get(), TinkersAnvilScreen::new);
        event.register(SmelteryContent.MELTER_MENU.get(), MelterScreen::new);
        event.register(SmelteryContent.CASTING_TABLE_MENU.get(), CastingScreen::new);
        event.register(SmelteryContent.CASTING_BASIN_MENU.get(), CastingScreen::new);
        event.register(SmelteryContent.ALLOYER_MENU.get(), AlloyerScreen::new);
        event.register(SmelteryContent.SMELTERY_MENU.get(), SmelteryScreen::new);
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (String id : StaticContent.toolPartIds()) {
            register(event, StaticContent.toolPart(id).get());
        }
        for (TinkersToolItem.ToolKind kind : TinkersToolItem.ToolKind.values()) {
            register(event, StaticContent.tool(kind).get());
        }
        for (var type : java.util.List.of(net.minecraft.world.item.ArmorItem.Type.HELMET,
                net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
                net.minecraft.world.item.ArmorItem.Type.BOOTS)) {
            for (var family : com.moderntinkers.content.tools.TinkersArmorItem.Family.values()) {
                var armor = StaticContent.armor(family, type);
                if (armor != null) {
                    register(event, armor.get());
                }
            }
        }
        register(event, StaticContent.TRAVELERS_SHIELD.get());
        register(event, StaticContent.PLATE_SHIELD.get());
        register(event, StaticContent.TINKERS_ARROW.get());
    }

    private static void register(RegisterColorHandlersEvent.Item event, Item item) {
        event.register(ModernTinkersClient::itemColor, item);
    }

    private static int itemColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return -1;
        }
        String material = "";
        if (MaterialPartItem.isPart(stack)) {
            material = MaterialPartItem.getMaterial(stack);
        } else if (TinkersToolItem.isTool(stack)) {
            material = TinkersToolItem.material(stack, TinkersToolItem.HEAD_KEY);
        } else if (TinkersArmorItem.isArmor(stack)) {
            material = TinkersArmorItem.material(stack);
        } else if (stack.getItem() instanceof TinkersShieldItem) {
            material = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag().getString(TinkersShieldItem.MATERIAL_KEY);
        } else if (stack.getItem() instanceof TinkersArrowItem) {
            material = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag().getString(TinkersArrowItem.HEAD_KEY);
        }
        var definition = MaterialManager.get(material);
        return definition == null ? -1 : definition.color();
    }
}
