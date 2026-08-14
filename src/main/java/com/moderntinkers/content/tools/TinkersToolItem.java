package com.moderntinkers.content.tools;

import com.mojang.serialization.MapCodec;
import com.moderntinkers.ModernTinkers;
import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.fluid.MaterialFluids;
import com.moderntinkers.content.material.MaterialDefinition;
import com.moderntinkers.content.material.MaterialManager;
import com.moderntinkers.content.modifier.ModifierManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A compact, stack-component backed Tinkers tool. The item type identifies the
 * tool behavior; the stack stores its three assembled material variants.
 */
public final class TinkersToolItem extends Item {
    private static final TagKey<Block> MINEABLE_MATTOCK = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(ModernTinkers.MOD_ID, "mineable/mattock"));
    public static final String KIND_KEY = "moderntinkers:tool_kind";
    public static final String HEAD_KEY = "moderntinkers:head_material";
    public static final String HANDLE_KEY = "moderntinkers:handle_material";
    public static final String BINDING_KEY = "moderntinkers:binding_material";
    /** Indexed material payload for tools with zero, two, three, or four parts. */
    public static final String PARTS_KEY = "moderntinkers:parts";
    public static final String MODIFIERS_KEY = "moderntinkers:modifiers";
    public static final String MODIFIER_SLOTS_KEY = "moderntinkers:modifier_slots";
    public static final String STORED_EXPERIENCE_KEY = "moderntinkers:stored_experience";
    public static final String WAR_CHARGE_KEY = "moderntinkers:war_charge";
    public static final String TOOL_FLUID_KEY = "moderntinkers:tool_fluid";
    public static final String TOOL_FLUID_AMOUNT_KEY = "moderntinkers:tool_fluid_amount";
    public static final String OVERSLIME_KEY = "moderntinkers:overslime";
    /** Ammo captured when an ammo-using tool starts drawing. */
    public static final String DRAWN_AMMO_KEY = "moderntinkers:drawn_ammo";
    /** Ammo consumed while loading a crossbow-family tool and fired later. */
    public static final String LOADED_AMMO_KEY = "moderntinkers:loaded_ammo";
    public static final int TOOL_TANK_CAPACITY = 4000;
    public static final int DEFAULT_MODIFIER_SLOTS = 3;
    public static final int OVERSLIME_PER_REINFORCEMENT = 75;

    private final ToolKind kind;

    public TinkersToolItem(Properties properties, ToolKind kind) {
        super(properties.stacksTo(kind == ToolKind.SHURIKEN || kind == ToolKind.THROWING_AXE
                ? 16 : 1));
        this.kind = kind;
    }

    public ToolKind kind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        boolean hasFluidInteraction = kind == ToolKind.SWASHER || kind == ToolKind.MELTING_PAN
                || hasModifier(tool, "spilling") || hasModifier(tool, "spitting");
        if (hasFluidInteraction) {
            InteractionResult bucketResult = useBucketInteraction(tool, player, hand);
            if (bucketResult.consumesAction()) {
                return InteractionResultHolder.sidedSuccess(tool, level.isClientSide());
            }
            if (kind == ToolKind.SWASHER && !player.isShiftKeyDown()
                    && !toolFluid(tool).isEmpty()) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(tool);
            }
        }
        if (kind != ToolKind.SWASHER && !player.isShiftKeyDown()
                && hasModifier(tool, "spitting") && !toolFluid(tool).isEmpty()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(tool);
        }
        if (kind == ToolKind.BATTLESIGN) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(tool);
        }
        if (kind == ToolKind.FISHING_ROD) {
            ItemStack rod = tool;
            if (!level.isClientSide) {
                if (player.fishing != null) {
                    int damage = player.fishing.retrieve(rod);
                    damageTool(rod, Math.max(1, damage), player, EquipmentSlot.MAINHAND);
                    player.fishing = null;
                } else {
                    FishingHook hook = new FishingHook(player, level,
                            modifierLevel(rod, "luck"), 0);
                    level.addFreshEntity(hook);
                    player.fishing = hook;
                }
            }
            return InteractionResultHolder.sidedSuccess(rod, level.isClientSide());
        }
        if (!kind.ranged()) {
            return super.use(level, player, hand);
        }
        ItemStack bow = tool;
        if (kind == ToolKind.CROSSBOW || kind == ToolKind.WAR_PICK) {
            ItemStack loaded = loadedAmmo(bow, level);
            if (!loaded.isEmpty()) {
                if (!level.isClientSide) {
                    clearLoadedAmmo(bow);
                    launchProjectile(bow, loaded, 1.0F, level, player, hand);
                }
                return InteractionResultHolder.sidedSuccess(bow, level.isClientSide());
            }
            ItemStack ammo = player.getAbilities().instabuild
                    ? new ItemStack(Items.ARROW) : findArrow(player);
            if (ammo.isEmpty()) {
                return InteractionResultHolder.fail(bow);
            }
            if (!level.isClientSide) {
                captureDrawnAmmo(bow, ammo, level);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(bow);
        }
        if (kind.requiresAmmo() && !player.getAbilities().instabuild && findArrow(player).isEmpty()) {
            return InteractionResultHolder.fail(bow);
        }
        if (kind.requiresAmmo() && !level.isClientSide) {
            ItemStack ammo = findArrow(player);
            if (!ammo.isEmpty()) {
                CustomData.update(DataComponents.CUSTOM_DATA, bow,
                        tag -> tag.put(DRAWN_AMMO_KEY,
                                ammo.copyWithCount(1).save(level.registryAccess())));
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(bow);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return kind.ranged() || kind == ToolKind.BATTLESIGN || kind == ToolKind.SWASHER
                || hasModifier(stack, "spitting") ? 72000
                : super.getUseDuration(stack, entity);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (kind == ToolKind.BATTLESIGN) {
            return UseAnim.BLOCK;
        }
        if (kind == ToolKind.LONGBOW || kind == ToolKind.FISHING_ROD) {
            return UseAnim.BOW;
        }
        if (kind == ToolKind.SWASHER) {
            return UseAnim.BOW;
        }
        if (hasModifier(stack, "spitting")) {
            return UseAnim.BOW;
        }
        return kind == ToolKind.CROSSBOW || kind == ToolKind.WAR_PICK ? UseAnim.CROSSBOW
                : kind.ranged() ? UseAnim.SPEAR : super.getUseAnimation(stack);
    }

    @Override
    public void releaseUsing(ItemStack bow, Level level, LivingEntity living, int timeLeft) {
        if (kind == ToolKind.SWASHER) {
            if (!level.isClientSide && living instanceof Player player) {
                spitFluid(bow, player, player.getUsedItemHand(),
                        getUseDuration(bow, player) - timeLeft);
            }
            return;
        }
        if (hasModifier(bow, "spitting")) {
            if (!level.isClientSide && living instanceof Player player) {
                spitFluid(bow, player, player.getUsedItemHand(),
                        getUseDuration(bow, player) - timeLeft);
            }
            return;
        }
        if (!kind.ranged() || !(living instanceof Player player)) {
            return;
        }
        if (kind == ToolKind.CROSSBOW || kind == ToolKind.WAR_PICK) {
            loadCrossbow(bow, level, player, player.getUsedItemHand(), timeLeft);
            return;
        }
        ItemStack drawnAmmo = drawnAmmo(bow, level);
        clearDrawnAmmo(bow);
        ItemStack ammo = kind.requiresAmmo()
                ? (drawnAmmo.isEmpty() ? findArrow(player) : drawnAmmo) : ItemStack.EMPTY;
        ItemStack ammoSource = kind.requiresAmmo() && !drawnAmmo.isEmpty()
                ? findArrow(player, drawnAmmo) : ammo;
        boolean creative = player.getAbilities().instabuild;
        if (kind.requiresAmmo() && (ammo.isEmpty() || ammoSource.isEmpty()) && !creative) {
            return;
        }
        int charge = getUseDuration(bow, player) - timeLeft;
        float power = Math.min(1.0F, charge / 20.0F);
        power = (power * power + power * 2.0F) / 3.0F;
        if (power < 0.1F) {
            return;
        }
        if (ammo.isEmpty()) {
            ammo = new ItemStack(Items.ARROW);
        }
        if (!level.isClientSide) {
            launchProjectile(bow, ammo, power, level, player, player.getUsedItemHand());
            if (kind.requiresAmmo() && !creative) {
                ammoSource.shrink(1);
            }
        }
    }

    /** Stores a full-charge projectile instead of firing a crossbow-family tool immediately. */
    private void loadCrossbow(ItemStack bow, Level level, Player player, InteractionHand hand,
                              int timeLeft) {
        ItemStack drawn = drawnAmmo(bow, level);
        clearDrawnAmmo(bow);
        if (!loadedAmmo(bow, level).isEmpty()) {
            return;
        }
        int charge = getUseDuration(bow, player) - timeLeft;
        if (charge < 20) {
            return;
        }
        boolean creative = player.getAbilities().instabuild;
        ItemStack ammo = drawn.isEmpty()
                ? (creative ? new ItemStack(Items.ARROW) : findArrow(player)) : drawn;
        ItemStack source = creative || ammo.isEmpty() ? ammo : findArrow(player, ammo);
        if (ammo.isEmpty() || (!creative && source.isEmpty())) {
            return;
        }
        if (!level.isClientSide) {
            if (!creative) {
                source.shrink(1);
            }
            setLoadedAmmo(bow, ammo, level);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0F,
                    1.0F / (level.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
        }
    }

    /** Creates and launches the current tool's projectile on the server. */
    private void launchProjectile(ItemStack bow, ItemStack ammo, float power, Level level,
                                  Player player, InteractionHand hand) {
        boolean creative = player.getAbilities().instabuild;
        StackProjectileEntity projectile;
        if (kind == ToolKind.JAVELIN) {
            ThrownToolEntity thrown = new ThrownToolEntity(
                    ProjectileContent.THROWN_TOOL.get(), level);
            thrown.initialize(bow, player);
            projectile = thrown;
        } else if (kind == ToolKind.SHURIKEN || kind == ToolKind.THROWING_AXE) {
            ThrownShurikenEntity thrown = new ThrownShurikenEntity(
                    ProjectileContent.THROWN_SHURIKEN.get(), level);
            thrown.initialize(bow, player);
            projectile = thrown;
        } else {
            ModifiableArrowEntity materialArrow = new ModifiableArrowEntity(
                    ProjectileContent.MODIFIABLE_ARROW.get(), level);
            materialArrow.initialize(ammo, bow, player);
            projectile = materialArrow;
        }
        float velocity = kind == ToolKind.CROSSBOW || kind == ToolKind.WAR_PICK ? 3.5F
                : kind == ToolKind.JAVELIN ? 2.8F
                : kind == ToolKind.SHURIKEN ? 2.6F
                : kind == ToolKind.THROWING_AXE ? 2.4F
                : 2.0F + power;
        if (kind == ToolKind.WAR_PICK) {
            velocity *= 1.0F + 0.01F * warCharge(bow);
        }
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                power * velocity, 1.0F);
        projectile.setCritArrow(power >= 1.0F && kind == ToolKind.LONGBOW);
        if (kind == ToolKind.CROSSBOW || kind == ToolKind.WAR_PICK) {
            projectile.setSoundEvent(SoundEvents.CROSSBOW_HIT);
        }
        projectile.pickup = kind.requiresAmmo()
                ? (creative ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED)
                : AbstractArrow.Pickup.DISALLOWED;
        projectile.setBaseDamage(projectile.getBaseDamage() + kind.projectileDamage());
        level.addFreshEntity(projectile);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                kind == ToolKind.CROSSBOW || kind == ToolKind.WAR_PICK
                        ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F,
                1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        if (kind == ToolKind.WAR_PICK) {
            setWarCharge(bow, 0);
        }
        if (kind == ToolKind.JAVELIN || kind == ToolKind.SHURIKEN
                || kind == ToolKind.THROWING_AXE) {
            if (!creative) {
                bow.shrink(1);
            }
        } else {
            damageTool(bow, 1, player, handSlot(hand));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (kind == ToolKind.FLINT_AND_BRICK) {
            return Items.FLINT_AND_STEEL.useOn(context);
        }
        if (kind == ToolKind.SWASHER) {
            return Items.SHEARS.useOn(context);
        }
        if (kind == ToolKind.MATTOCK) {
            InteractionResult result = Items.WOODEN_AXE.useOn(context);
            if (result.consumesAction()) {
                return result;
            }
            result = Items.WOODEN_SHOVEL.useOn(context);
            if (result.consumesAction()) {
                return result;
            }
            return Items.WOODEN_HOE.useOn(context);
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (kind == ToolKind.SWASHER) {
            return Items.SHEARS.interactLivingEntity(stack, player, target, hand);
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    /** Returns the bounded fluid stored by a tool, including vanilla fluids. */
    public static FluidStack toolFluid(ItemStack stack) {
        if (stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag();
        String id = tag.getString(TOOL_FLUID_KEY);
        int amount = Math.max(0, Math.min(TOOL_TANK_CAPACITY,
                tag.getInt(TOOL_FLUID_AMOUNT_KEY)));
        if (id.isEmpty() || amount <= 0) {
            return FluidStack.EMPTY;
        }
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(location);
        return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    /**
     * Returns whether this stack owns a real Tinkers fluid tank. The item
     * capability is registered by item type so it can survive modifier
     * application; this stack-level check keeps ordinary tools from exposing
     * storage they do not have.
     */
    public static boolean hasFluidTank(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof TinkersToolItem tool) {
            return tool.kind == ToolKind.SWASHER || tool.kind == ToolKind.MELTING_PAN
                    || hasModifier(stack, "tank")
                    || hasModifier(stack, "spilling")
                    || hasModifier(stack, "spitting");
        }
        return (TinkersArmorItem.isArmor(stack) || stack.getItem() instanceof TinkersShieldItem)
                && (hasModifier(stack, "tank")
                || hasModifier(stack, "spilling")
                || hasModifier(stack, "spitting"));
    }

    /** Simulates or fills the compact tool tank with one compatible fluid. */
    public static int fillToolFluid(ItemStack stack, FluidStack fluid,
                                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
        if (stack.isEmpty() || fluid == null || fluid.isEmpty() || fluid.getFluid() == Fluids.EMPTY) {
            return 0;
        }
        ResourceLocation location = net.minecraft.core.registries.BuiltInRegistries.FLUID
                .getKey(fluid.getFluid());
        if (location == null) {
            return 0;
        }
        FluidStack stored = toolFluid(stack);
        if (!stored.isEmpty() && stored.getFluid() != fluid.getFluid()) {
            return 0;
        }
        int accepted = Math.min(fluid.getAmount(), TOOL_TANK_CAPACITY - stored.getAmount());
        if (accepted <= 0) {
            return 0;
        }
        if (action.execute()) {
            int finalAmount = stored.getAmount() + accepted;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putString(TOOL_FLUID_KEY, location.toString());
                tag.putInt(TOOL_FLUID_AMOUNT_KEY, finalAmount);
            });
        }
        return accepted;
    }

    /** Drains at most the requested amount from the compact tool tank. */
    public static FluidStack drainToolFluid(ItemStack stack, int amount,
                                             net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
        FluidStack stored = toolFluid(stack);
        if (stored.isEmpty() || amount <= 0) {
            return FluidStack.EMPTY;
        }
        int drained = Math.min(amount, stored.getAmount());
        FluidStack result = stored.copy();
        result.setAmount(drained);
        if (action.execute()) {
            int remaining = stored.getAmount() - drained;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                if (remaining <= 0) {
                    tag.remove(TOOL_FLUID_KEY);
                    tag.remove(TOOL_FLUID_AMOUNT_KEY);
                } else {
                    tag.putInt(TOOL_FLUID_AMOUNT_KEY, remaining);
                }
            });
        }
        return result;
    }

    /** Handles bucket transfer for any stack with the compact tool tank payload. */
    public static InteractionResult useBucketInteraction(ItemStack tool, Player player,
                                                         InteractionHand hand) {
        InteractionHand containerHand = hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack container = player.getItemInHand(containerHand);
        Fluid bucketFluid = bucketFluid(container);
        if (bucketFluid != Fluids.EMPTY) {
            FluidStack input = new FluidStack(bucketFluid, 1000);
            if (fillToolFluid(tool, input,
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)
                    != 1000) {
                return InteractionResult.PASS;
            }
            fillToolFluid(tool, input,
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            if (!player.getAbilities().instabuild) {
                replaceContainer(player, containerHand, container, Items.BUCKET);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        if (!container.is(Items.BUCKET)) {
            return InteractionResult.PASS;
        }
        FluidStack stored = toolFluid(tool);
        if (stored.getAmount() < 1000) {
            return InteractionResult.PASS;
        }
        Item bucket = bucketFor(stored.getFluid());
        if (bucket == Items.AIR) {
            return InteractionResult.PASS;
        }
        drainToolFluid(tool, 1000,
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (!player.getAbilities().instabuild) {
            replaceContainer(player, containerHand, container, bucket);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private static Fluid bucketFluid(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) {
            return Fluids.WATER;
        }
        if (stack.is(Items.LAVA_BUCKET)) {
            return Fluids.LAVA;
        }
        for (MaterialFluids.FluidSet set : MaterialFluids.all()) {
            if (set.bucket().isBound() && stack.is(set.bucket().get())) {
                return set.source().isBound() ? set.source().get() : Fluids.EMPTY;
            }
        }
        return Fluids.EMPTY;
    }

    private static Item bucketFor(Fluid fluid) {
        if (fluid == Fluids.WATER) {
            return Items.WATER_BUCKET;
        }
        if (fluid == Fluids.LAVA) {
            return Items.LAVA_BUCKET;
        }
        for (MaterialFluids.FluidSet set : MaterialFluids.all()) {
            if (set.source().isBound() && fluid == set.source().get()
                    && set.bucket().isBound()) {
                return set.bucket().get();
            }
        }
        return Items.AIR;
    }

    private static void replaceContainer(Player player, InteractionHand hand,
                                         ItemStack container, Item replacement) {
        if (container.getCount() == 1) {
            player.setItemInHand(hand, new ItemStack(replacement));
            return;
        }
        container.shrink(1);
        ItemStack result = new ItemStack(replacement);
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
    }

    /** Fires one charged fluid interaction from a stack carrying the spitting modifier. */
    public static void spitFluid(ItemStack tool, Player player, InteractionHand hand,
                                 int chargeTime) {
        if (chargeTime <= 0) {
            return;
        }
        FluidStack fluid = toolFluid(tool);
        if (fluid.isEmpty()) {
            return;
        }
        LivingEntity target = player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().expandTowards(player.getLookAngle().scale(8.0D))
                                .inflate(1.0D), entity -> entity != player && entity.isPickable())
                .stream()
                .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
        float strength = Math.min(1.0F, chargeTime / 20.0F);
        if (target != null) {
            applyFluidEffect(fluid, target, player, strength);
        }
        int shots = Math.max(1, modifierLevel(tool, "spitting"));
        drainToolFluid(tool, Math.min(100 * shots, fluid.getAmount()),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        damageTool(tool, shots, player, handSlot(hand));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    public static void applyFluidEffect(FluidStack fluid, LivingEntity target,
                                        LivingEntity attacker, float strength) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.FLUID
                .getKey(fluid.getFluid());
        String path = id == null ? "" : id.getPath();
        if (fluid.getFluid() == Fluids.WATER) {
            target.clearFire();
            return;
        }
        if (path.contains("ender")) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING, 60, 0));
        } else if (path.contains("honey")) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        } else if (path.contains("ichor")) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.POISON, 60, 0));
        } else if (path.contains("meat")) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.HUNGER, 80, 0));
        } else {
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(),
                    (int) (40.0F * Math.max(0.5F, strength))));
        }
    }

    private static ItemStack findArrow(Player player) {
        if (player.getMainHandItem().getItem() instanceof ArrowItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof ArrowItem) {
            return player.getOffhandItem();
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ArrowItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findArrow(Player player, ItemStack requested) {
        if (requested.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (player.getMainHandItem().getItem() instanceof ArrowItem
                && ItemStack.isSameItemSameComponents(player.getMainHandItem(), requested)) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof ArrowItem
                && ItemStack.isSameItemSameComponents(player.getOffhandItem(), requested)) {
            return player.getOffhandItem();
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ArrowItem
                    && ItemStack.isSameItemSameComponents(stack, requested)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack drawnAmmo(ItemStack stack, Level level) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag();
        if (!tag.contains(DRAWN_AMMO_KEY, CompoundTag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parse(level.registryAccess(), tag.getCompound(DRAWN_AMMO_KEY))
                .orElse(ItemStack.EMPTY);
    }

    private static void captureDrawnAmmo(ItemStack stack, ItemStack ammo, Level level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.put(DRAWN_AMMO_KEY,
                        ammo.copyWithCount(1).save(level.registryAccess())));
    }

    private static ItemStack loadedAmmo(ItemStack stack, Level level) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag();
        if (!tag.contains(LOADED_AMMO_KEY, CompoundTag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parse(level.registryAccess(), tag.getCompound(LOADED_AMMO_KEY))
                .orElse(ItemStack.EMPTY);
    }

    private static void setLoadedAmmo(ItemStack stack, ItemStack ammo, Level level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (ammo.isEmpty()) {
                tag.remove(LOADED_AMMO_KEY);
            } else {
                tag.put(LOADED_AMMO_KEY,
                        ammo.copyWithCount(1).save(level.registryAccess()));
            }
        });
    }

    private static void clearLoadedAmmo(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(LOADED_AMMO_KEY));
    }

    private static EquipmentSlot handSlot(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }

    private static void clearDrawnAmmo(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(DRAWN_AMMO_KEY));
    }

    public static boolean hasArrow(Player player) {
        return !findArrow(player).isEmpty();
    }

    public static int warCharge(ItemStack stack) {
        return Math.max(0, Math.min(25, stack.getOrDefault(DataComponents.CUSTOM_DATA,
                CustomData.EMPTY).copyTag().getInt(WAR_CHARGE_KEY)));
    }

    public static void addWarCharge(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkersToolItem tool)
                || tool.kind != ToolKind.WAR_PICK) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(WAR_CHARGE_KEY, Math.min(25,
                        tag.getInt(WAR_CHARGE_KEY) + 1)));
    }

    private static void setWarCharge(ItemStack stack, int charge) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(WAR_CHARGE_KEY, Math.max(0, Math.min(25, charge))));
    }

    public static boolean isTool(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof TinkersToolItem;
    }

    /**
     * Returns the material portions represented by an assembled tool.  Keeping
     * the parts separate matters for mixed-material tools: the melter must not
     * silently turn a handle or binding into the head material.
     */
    public static List<MaterialAmount> meltingMaterials(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkersToolItem tool)) {
            return List.of();
        }
        Map<String, Integer> amounts = new LinkedHashMap<>();
        for (int index = 0; index < tool.kind.parts.length; index++) {
            addMaterial(amounts, partMaterial(stack, tool.kind, index),
                    tool.kind.parts[index]);
        }
        List<MaterialAmount> result = new ArrayList<>();
        amounts.forEach((material, units) -> result.add(new MaterialAmount(material, units)));
        return result;
    }

    private static void addMaterial(Map<String, Integer> amounts, String material, String partId) {
        int units = MaterialPartItem.partUnits(partId);
        if (!material.isEmpty() && units > 0) {
            amounts.merge(material, units, Integer::sum);
        }
    }

    public static String material(ItemStack stack, String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(key);
    }

    /** Returns the material at an assembled tool slot, including legacy stacks. */
    public static String partMaterial(ItemStack stack, ToolKind kind, int index) {
        if (index < 0 || index >= kind.parts.length) {
            return "";
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag();
        CompoundTag parts = tag.getCompound(PARTS_KEY);
        String indexed = parts.getString(Integer.toString(index));
        if (!indexed.isEmpty()) {
            return indexed;
        }
        return switch (index) {
            case 0 -> tag.getString(HEAD_KEY);
            case 1 -> tag.getString(HANDLE_KEY);
            case 2 -> tag.getString(BINDING_KEY);
            default -> "";
        };
    }

    public static int modifierLevel(ItemStack stack, String modifier) {
        int stored = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompound(MODIFIERS_KEY).getInt(modifier);
        if (!(stack.getItem() instanceof TinkersToolItem tool)) {
            return stored;
        }
        return stored + tool.kind.builtinModifierLevel(modifier);
    }

    public static int modifierSlots(ItemStack stack) {
        int slots = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(MODIFIER_SLOTS_KEY);
        return slots > 0 ? slots : DEFAULT_MODIFIER_SLOTS;
    }

    public static int modifierSlotsUsed(ItemStack stack) {
        CompoundTag modifiers = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompound(MODIFIERS_KEY);
        int used = 0;
        for (String key : modifiers.getAllKeys()) {
            if ("modifier_crystal".equals(key)) {
                continue;
            }
            used += Math.max(0, modifiers.getInt(key)) * ModifierManager.slotCost(key);
        }
        return used;
    }

    public static boolean hasModifierCapacity(ItemStack stack) {
        return modifierSlotsUsed(stack) < modifierSlots(stack);
    }

    public static boolean hasModifierCapacity(ItemStack stack, String modifier) {
        return modifierSlotsUsed(stack) + ModifierManager.slotCost(modifier)
                <= modifierSlots(stack);
    }

    /** Maximum overslime from slimy materials and slimesteel reinforcement. */
    public static int overslimeCapacity(ItemStack stack) {
        int capacity = modifierLevel(stack, "slimesteel_reinforcement")
                * OVERSLIME_PER_REINFORCEMENT;
        if (hasMaterialTrait(stack, "slimey") || hasMaterialTrait(stack, "slime")) {
            capacity += 50;
        }
        return Math.max(0, capacity);
    }

    /** Current overslime, clamped to the capacity granted by the stack. */
    public static int overslimeAmount(ItemStack stack) {
        int capacity = overslimeCapacity(stack);
        if (capacity <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(capacity, stack.getOrDefault(DataComponents.CUSTOM_DATA,
                CustomData.EMPTY).copyTag().getInt(OVERSLIME_KEY)));
    }

    public static void addOverslime(ItemStack stack, int amount) {
        if (amount > 0 && overslimeCapacity(stack) > 0) {
            setOverslime(stack, overslimeAmount(stack) + amount);
        }
    }

    private static void setOverslime(ItemStack stack, int amount) {
        int capacity = overslimeCapacity(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            int clamped = Math.max(0, Math.min(capacity, amount));
            if (clamped == 0) {
                tag.remove(OVERSLIME_KEY);
            } else {
                tag.putInt(OVERSLIME_KEY, clamped);
            }
        });
    }

    /** Damages a modifiable stack after consuming overslime as a durability shield. */
    public static int damageTool(ItemStack stack, int amount, LivingEntity user,
                                 EquipmentSlot slot) {
        if (amount <= 0 || stack.isEmpty()) {
            return 0;
        }
        int shield = overslimeAmount(stack);
        int absorbed = Math.min(shield, amount);
        if (absorbed > 0) {
            setOverslime(stack, shield - absorbed);
        }
        int remaining = amount - absorbed;
        if (remaining > 0) {
            if (user != null) {
                stack.hurtAndBreak(remaining, user, slot);
            } else if (stack.getMaxDamage() > 0) {
                int damage = stack.getDamageValue() + remaining;
                if (damage >= stack.getMaxDamage()) {
                    stack.shrink(1);
                } else {
                    stack.setDamageValue(damage);
                }
            }
        }
        return remaining;
    }

    /** Removes overslime without invoking ItemStack.hurtAndBreak recursively. */
    public static int consumeOverslime(ItemStack stack, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int absorbed = Math.min(overslimeAmount(stack), amount);
        if (absorbed > 0) {
            setOverslime(stack, overslimeAmount(stack) - absorbed);
        }
        return absorbed;
    }

    /** Effective overshield level, including armor material traits. */
    public static int overshieldLevel(ItemStack stack) {
        int level = modifierLevel(stack, "overshield");
        return level + (hasMaterialTrait(stack, "overshield") ? 1 : 0);
    }

    public static boolean hasMaterialTrait(ItemStack stack, String trait) {
        if (stack.getItem() instanceof TinkersToolItem tool) {
            for (int index = 0; index < tool.kind.parts.length; index++) {
                if (MaterialManager.hasTrait(partMaterial(stack, tool.kind, index), trait)) {
                    return true;
                }
            }
            return false;
        }
        if (TinkersArmorItem.isArmor(stack)) {
            return MaterialManager.hasTrait(TinkersArmorItem.material(stack), trait)
                    || MaterialManager.hasTrait(
                    stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                            .copyTag().getString(TinkersArmorItem.MAILLE_KEY), trait)
                    || MaterialManager.hasTrait(TinkersArmorItem.slimeMaterial(stack), trait);
        }
        if (stack.getItem() instanceof TinkersShieldItem) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag();
            return MaterialManager.hasTrait(tag.getString(TinkersShieldItem.MATERIAL_KEY), trait)
                    || MaterialManager.hasTrait(tag.getString(TinkersShieldItem.CORE_KEY), trait)
                    || MaterialManager.hasTrait(tag.getString(TinkersShieldItem.PLATING_KEY), trait)
                    || MaterialManager.hasTrait(tag.getString(TinkersShieldItem.HANDLE_KEY), trait);
        }
        if (stack.getItem() instanceof TinkersArrowItem) {
            return MaterialManager.hasTrait(
                    stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                            .copyTag().getString(TinkersArrowItem.HEAD_KEY), trait);
        }
        return false;
    }

    public static boolean hasModifier(ItemStack stack, String modifier) {
        return modifierLevel(stack, modifier) > 0;
    }

    /** Applies one bounded fluid splash from a spilling tool or armor piece. */
    public static boolean spillFluid(ItemStack stack, LivingEntity target,
                                     LivingEntity attacker, float strength) {
        if (target == null || attacker == null || target == attacker
                || !hasModifier(stack, "spilling")) {
            return false;
        }
        FluidStack fluid = toolFluid(stack);
        if (fluid.isEmpty()) {
            return false;
        }
        applyFluidEffect(fluid, target, attacker, strength);
        drainToolFluid(stack, Math.min(100 * modifierLevel(stack, "spilling"), fluid.getAmount()),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    /** Returns every material represented by a tool part and accepted for repair. */
    public static List<String> repairMaterials(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkersToolItem tool)) {
            return List.of();
        }
        java.util.LinkedHashSet<String> materials = new java.util.LinkedHashSet<>();
        for (int index = 0; index < tool.kind.parts.length; index++) {
            String material = partMaterial(stack, tool.kind, index);
            if (!material.isEmpty()) {
                materials.add(material);
            }
        }
        return List.copyOf(materials);
    }

    /** Applies one level only when the complete modifier contract still holds. */
    public static boolean addModifier(ItemStack stack, String modifier) {
        if (!ModifierManager.isKnown(modifier)
                || !ModifierManager.canApply(modifier, stack)
                || modifierLevel(stack, modifier) >= ModifierManager.maxLevel(modifier)
                || !hasModifierCapacity(stack, modifier)) {
            return false;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag modifiers = tag.getCompound(MODIFIERS_KEY);
            modifiers.putInt(modifier, modifiers.getInt(modifier) + 1);
            tag.put(MODIFIERS_KEY, modifiers);
        });
        if (stack.getItem() instanceof TinkersToolItem) {
            refreshDerivedComponents(stack);
        } else if (TinkersArmorItem.isArmor(stack)) {
            TinkersArmorItem.refreshDerivedComponents(stack);
        } else if (stack.getItem() instanceof TinkersShieldItem) {
            TinkersShieldItem.refreshDerivedComponents(stack);
        }
        if ("slimesteel_reinforcement".equals(modifier)) {
            addOverslime(stack, OVERSLIME_PER_REINFORCEMENT);
        }
        return true;
    }

    /** Removes one stored level; built-in tool traits are never removable here. */
    public static boolean removeModifier(ItemStack stack, String modifier) {
        if (!ModifierManager.isKnown(modifier)) {
            return false;
        }
        CompoundTag stored = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompound(MODIFIERS_KEY);
        int current = stored.getInt(modifier);
        if (current <= 0) {
            return false;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag modifiers = tag.getCompound(MODIFIERS_KEY);
            int remaining = modifiers.getInt(modifier) - 1;
            if (remaining > 0) {
                modifiers.putInt(modifier, remaining);
            } else {
                modifiers.remove(modifier);
            }
            tag.put(MODIFIERS_KEY, modifiers);
        });
        if (stack.getItem() instanceof TinkersToolItem) {
            refreshDerivedComponents(stack);
        } else if (TinkersArmorItem.isArmor(stack)) {
            TinkersArmorItem.refreshDerivedComponents(stack);
        } else if (stack.getItem() instanceof TinkersShieldItem) {
            TinkersShieldItem.refreshDerivedComponents(stack);
        }
        if ("slimesteel_reinforcement".equals(modifier)) {
            setOverslime(stack, overslimeAmount(stack));
        }
        return true;
    }

    /** Rebuilds stack components after a modifier or datapack rule changes. */
    public static void refreshDerivedComponents(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkersToolItem tool)) {
            return;
        }
        List<MaterialDefinition> definitions = new ArrayList<>();
        for (int index = 0; index < tool.kind.parts.length; index++) {
            MaterialDefinition definition = MaterialManager.get(
                    partMaterial(stack, tool.kind, index));
            if (definition == null) {
                return;
            }
            definitions.add(definition);
        }

        float miningSpeed = tool.kind.fixedMiningSpeed();
        float attackDamage = tool.kind.baseAttackDamage;
        float attackSpeed = tool.kind.baseAttackSpeed;
        int durability = tool.kind.fixedDurability();
        if (!definitions.isEmpty()) {
            MaterialDefinition head = definitions.get(0);
            miningSpeed = head.miningSpeed();
            attackDamage += head.headAttackDamage();
            durability = head.headDurability();
            for (int index = 1; index < definitions.size(); index++) {
                MaterialDefinition handle = definitions.get(index);
                miningSpeed *= 1.0F + handle.handleMiningSpeed();
                attackDamage += handle.handleAttackDamage();
                attackSpeed += handle.handleAttackSpeed();
                durability = Math.round(durability * (1.0F + handle.handleDurability()));
            }
            miningSpeed = Math.max(0.1F, miningSpeed);
        }
        if (!definitions.isEmpty()) {
            miningSpeed *= tool.kind.miningSpeedMultiplier();
            attackDamage *= tool.kind.attackDamageMultiplier();
            durability = Math.round(durability * tool.kind.durabilityMultiplier());
        }
        if (durability <= 0) {
            return;
        }
        stack.set(DataComponents.MAX_DAMAGE, Math.max(1, durability));
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ResourceLocation.fromNamespaceAndPath(
                                ModernTinkers.MOD_ID, tool.kind.id + "_damage"),
                                attackDamage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(ResourceLocation.fromNamespaceAndPath(
                                ModernTinkers.MOD_ID, tool.kind.id + "_speed"),
                                attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build());
        TagKey<Block> effectiveTag = effectiveTag(tool.kind);
        stack.set(DataComponents.TOOL, new Tool(
                effectiveTag == null ? List.of()
                        : List.of(Tool.Rule.overrideSpeed(effectiveTag, miningSpeed)),
                miningSpeed, 1));
    }

    public static void addStoredExperience(ItemStack stack, int amount) {
        if (amount <= 0) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(STORED_EXPERIENCE_KEY,
                        tag.getInt(STORED_EXPERIENCE_KEY) + amount));
    }

    public static ItemStack assemble(Item item, ToolKind kind, ItemStack... parts) {
        if (parts.length != kind.parts.length) {
            return ItemStack.EMPTY;
        }
        List<String> materials = new ArrayList<>();
        for (int index = 0; index < parts.length; index++) {
            ItemStack part = parts[index];
            if (!MaterialPartItem.isPart(part)
                    || !kind.parts[index].equals(MaterialPartItem.getPartId(part))) {
                return ItemStack.EMPTY;
            }
            String material = MaterialPartItem.getMaterial(part);
            if (MaterialManager.get(material) == null || material.isEmpty()) {
                return ItemStack.EMPTY;
            }
            materials.add(material);
        }

        ItemStack result = new ItemStack(item);
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
            tag.putString(KIND_KEY, kind.id);
            CompoundTag indexed = new CompoundTag();
            for (int index = 0; index < materials.size(); index++) {
                indexed.putString(Integer.toString(index), materials.get(index));
            }
            tag.put(PARTS_KEY, indexed);
            if (!materials.isEmpty()) {
                tag.putString(HEAD_KEY, materials.get(0));
            }
            if (materials.size() > 1) {
                tag.putString(HANDLE_KEY, materials.get(1));
            }
            if (materials.size() > 2) {
                tag.putString(BINDING_KEY, materials.get(2));
            }
            tag.putInt(MODIFIER_SLOTS_KEY, DEFAULT_MODIFIER_SLOTS);
        });
        refreshDerivedComponents(result);
        result.set(DataComponents.MAX_DAMAGE, Math.max(1, result.getMaxDamage()));
        result.set(DataComponents.DAMAGE, 0);
        return result;
    }

    /** Initializes zero-part tools produced by vanilla crafting recipes. */
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (kind.parts.length != 0
                || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().contains(KIND_KEY)) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(KIND_KEY, kind.id);
            tag.put(PARTS_KEY, new CompoundTag());
            tag.putInt(MODIFIER_SLOTS_KEY, DEFAULT_MODIFIER_SLOTS);
        });
        refreshDerivedComponents(stack);
        stack.set(DataComponents.MAX_DAMAGE, Math.max(1, kind.fixedDurability()));
        stack.set(DataComponents.DAMAGE, 0);
    }

    @Override
    public Component getName(ItemStack stack) {
        String material = material(stack, HEAD_KEY);
        return material.isEmpty()
                ? super.getName(stack)
                : Component.translatable("item.moderntinkers.tool_name",
                        Component.translatable("material.moderntinkers." + material),
                        Component.translatable("item.moderntinkers." + kind.id));
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        TagKey<Block> effectiveTag = effectiveTag(kind);
        if (effectiveTag == null || !state.is(effectiveTag)) {
            return 1.0F;
        }
        MaterialDefinition head = MaterialManager.get(partMaterial(stack, kind, 0));
        if (head == null) {
            return kind.fixedMiningSpeed();
        }
        float speed = head.miningSpeed();
        for (int index = 1; index < kind.parts.length; index++) {
            MaterialDefinition handle = MaterialManager.get(partMaterial(stack, kind, index));
            if (handle != null) {
                speed *= 1.0F + handle.handleMiningSpeed();
            }
        }
        return Math.max(0.1F, speed
                        * (1.0F + 0.10F * modifierLevel(stack, "redstone"))
                        * (hasMaterialTrait(stack, "lightspeed") ? 1.10F : 1.0F)
                        * (hasMaterialTrait(stack, "soulspeed")
                        && state.is(BlockTags.SOUL_SPEED_BLOCKS) ? 1.35F : 1.0F));
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        TagKey<Block> effectiveTag = effectiveTag(kind);
        if (effectiveTag == null || !state.is(effectiveTag)) {
            return false;
        }
        MaterialDefinition head = MaterialManager.get(partMaterial(stack, kind, 0));
        if (head == null) {
            return kind.fixedDurability() > 0;
        }
        int requiredTier = state.is(BlockTags.NEEDS_DIAMOND_TOOL) ? 3
                : state.is(BlockTags.NEEDS_IRON_TOOL) ? 2
                : state.is(BlockTags.NEEDS_STONE_TOOL) ? 1 : 0;
        return head.tier() >= requiredTier;
    }

    private static TagKey<Block> effectiveTag(ToolKind kind) {
        if (kind == ToolKind.MATTOCK) {
            return MINEABLE_MATTOCK;
        }
        if (kind.effectiveTag != null) {
            return kind.effectiveTag;
        }
        return switch (kind) {
            case SKY_STAFF, EARTH_STAFF, ICHOR_STAFF, ENDER_STAFF ->
                    BlockTags.MINEABLE_WITH_PICKAXE;
            default -> null;
        };
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        applyProjectileEffects(stack, target, attacker);
        if (kind == ToolKind.SWASHER && !attacker.level().isClientSide) {
            FluidStack fluid = toolFluid(stack);
            if (!fluid.isEmpty()) {
                applyFluidEffect(fluid, target, attacker, 1.0F);
                drainToolFluid(stack, Math.min(100, fluid.getAmount()),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            }
        }
        if (kind == ToolKind.MINOTAUR_AXE && attacker.isSprinting()) {
            target.hurt(attacker.damageSources().mobAttack(attacker), 7.0F);
        }
        if (kind == ToolKind.BATTLESIGN) {
            target.knockback(0.45D, attacker.getX() - target.getX(),
                    attacker.getZ() - target.getZ());
        }
        if (!avoidsDamage(stack, attacker)) {
            damageTool(stack, 1, attacker, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    /** Applies modifier/material hit effects without spending projectile ammo or durability. */
    public void applyProjectileEffects(ItemStack stack, LivingEntity target,
                                       LivingEntity attacker) {
        int fiery = modifierLevel(stack, "fiery");
        if (fiery > 0) {
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), (2 + fiery * 2) * 20));
        }
        int necrotic = modifierLevel(stack, "necrotic");
        if (necrotic > 0) {
            attacker.heal(0.5F * necrotic);
        }
        int knockback = modifierLevel(stack, "knockback");
        if (knockback > 0) {
            target.knockback(0.2D * knockback,
                    attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        }
        int quartz = modifierLevel(stack, "quartz");
        if (quartz > 0) {
            target.hurt(attacker.damageSources().mobAttack(attacker), 0.5F * quartz);
        }
        int sharpness = modifierLevel(stack, "sharpness");
        if (sharpness > 0) {
            target.hurt(attacker.damageSources().mobAttack(attacker), 0.5F * sharpness);
        }
        int smite = modifierLevel(stack, "smite");
        if (smite > 0 && target.getType().is(EntityTypeTags.UNDEAD)) {
            target.hurt(attacker.damageSources().mobAttack(attacker), 1.5F * smite);
        }
        int bane = modifierLevel(stack, "bane_of_arthropods");
        if (bane > 0 && target.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
            target.hurt(attacker.damageSources().mobAttack(attacker), 1.5F * bane);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    20 * bane, Math.max(0, bane - 1)));
        }
        if (hasMaterialTrait(stack, "flamewake")) {
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 40));
        }
        if (hasMaterialTrait(stack, "spiny")) {
            attacker.hurt(attacker.damageSources().thorns(target), 0.5F);
        }
        if (hasMaterialTrait(stack, "necrotic")) {
            attacker.heal(1.0F);
        }
        if (hasMaterialTrait(stack, "insatiable") && stack.getDamageValue() > 0) {
            target.hurt(attacker.damageSources().mobAttack(attacker),
                    Math.min(3.0F, stack.getDamageValue() / 100.0F));
        }
        spillFluid(stack, target, attacker, 1.0F);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                             net.minecraft.core.BlockPos pos, LivingEntity user) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
            if (!avoidsDamage(stack, user)) {
                damageTool(stack, 1, user, EquipmentSlot.MAINHAND);
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        String head = material(stack, HEAD_KEY);
        String handle = material(stack, HANDLE_KEY);
        if (!head.isEmpty()) {
            tooltip.add(Component.translatable("item.moderntinkers.tool.head",
                    Component.translatable("material.moderntinkers." + head)));
        }
        if (!handle.isEmpty()) {
            tooltip.add(Component.translatable("item.moderntinkers.tool.handle",
                    Component.translatable("material.moderntinkers." + handle)));
        }
        MaterialDefinition definition = MaterialManager.get(head);
        if (definition != null) {
            tooltip.add(Component.translatable("item.moderntinkers.tool.stats",
                    definition.headDurability(),
                    String.format(java.util.Locale.ROOT, "%.1f", definition.miningSpeed())));
            for (String trait : definition.traits()) {
                tooltip.add(Component.translatable("item.moderntinkers.tool.trait", trait));
            }
        }
        CompoundTag modifiers = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompound(MODIFIERS_KEY);
        for (String modifier : modifiers.getAllKeys()) {
            tooltip.add(Component.translatable("item.moderntinkers.tool.modifier",
                    modifier, modifiers.getInt(modifier)));
        }
        tooltip.add(Component.translatable("item.moderntinkers.tool.modifier_slots",
                modifierSlotsUsed(stack), modifierSlots(stack)));
        int storedExperience = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(STORED_EXPERIENCE_KEY);
        if (storedExperience > 0) {
            tooltip.add(Component.translatable("item.moderntinkers.tool.experience",
                    storedExperience));
        }
        int overslimeCapacity = overslimeCapacity(stack);
        if (overslimeCapacity > 0) {
            tooltip.add(Component.translatable("item.moderntinkers.tool.overslime",
                    overslimeAmount(stack), overslimeCapacity));
        }
    }

    private static boolean avoidsDamage(ItemStack stack, LivingEntity user) {
        int level = modifierLevel(stack, "emerald_reinforcement");
        boolean durable = hasMaterialTrait(stack, "durable")
                && user.getRandom().nextInt(5) == 0;
        return durable || level > 0 && user.getRandom().nextInt(Math.max(2, 4 - level)) != 0;
    }

    public record MaterialAmount(String materialId, int units) {}

    public enum ToolKind {
        PICKAXE("pickaxe", BlockTags.MINEABLE_WITH_PICKAXE, 0.5F, -2.8F,
                "pick_head", "tool_handle", "tool_binding"),
        SLEDGE_HAMMER("sledge_hammer", BlockTags.MINEABLE_WITH_PICKAXE, 3.0F, -3.1F,
                "hammer_head", "tough_handle", "large_plate", "large_plate"),
        VEIN_HAMMER("vein_hammer", BlockTags.MINEABLE_WITH_PICKAXE, 3.0F, -3.0F,
                "hammer_head", "tough_handle", "tough_binding", "large_plate"),
        PICKADZE("pickadze", BlockTags.MINEABLE_WITH_SHOVEL, 0.75F, -2.9F,
                "pick_head", "tool_handle", "adze_head"),
        EXCAVATOR("excavator", BlockTags.MINEABLE_WITH_SHOVEL, 1.5F, -3.2F,
                "large_plate", "tough_handle", "tough_binding", "tough_handle"),
        HAND_AXE("hand_axe", BlockTags.MINEABLE_WITH_AXE, 6.0F, -2.8F,
                "small_axe_head", "tool_handle", "tool_binding"),
        BROAD_AXE("broad_axe", BlockTags.MINEABLE_WITH_AXE, 5.0F, -3.2F,
                "broad_axe_head", "tough_handle", "pick_head", "tough_binding"),
        THROWING_AXE("throwing_axe", BlockTags.SWORD_EFFICIENT, 0.0F, -2.6F,
                "arrow_head", "arrow_shaft"),
        MATTOCK("mattock", MINEABLE_MATTOCK, 1.5F, -2.8F,
                "small_axe_head", "tool_handle", "adze_head"),
        KAMA("kama", BlockTags.MINEABLE_WITH_HOE, 1.0F, -2.6F,
                "small_blade", "tool_handle", "tool_binding"),
        SCYTHE("scythe", BlockTags.MINEABLE_WITH_HOE, 1.0F, -3.0F,
                "broad_blade", "tough_handle", "tough_binding", "tough_handle"),
        DAGGER("dagger", BlockTags.SWORD_EFFICIENT, 3.0F, -1.8F,
                "small_blade", "tool_handle"),
        SWORD("sword", BlockTags.SWORD_EFFICIENT, 3.0F, -2.4F,
                "small_blade", "tool_handle", "tool_handle"),
        CLEAVER("cleaver", BlockTags.SWORD_EFFICIENT, 3.0F, -3.0F,
                "broad_blade", "tough_handle", "tough_handle", "large_plate"),
        LONGBOW("longbow", BlockTags.MINEABLE_WITH_AXE, 0.0F, -2.0F,
                "bow_limb", "bow_limb", "bow_grip", "bowstring"),
        CROSSBOW("crossbow", BlockTags.MINEABLE_WITH_AXE, 0.0F, -2.4F,
                "bow_limb", "bow_grip", "bowstring"),
        JAVELIN("javelin", BlockTags.SWORD_EFFICIENT, 3.0F, -2.8F,
                "small_blade", "tool_handle", "bow_limb", "bow_grip"),
        SHURIKEN("shuriken", BlockTags.SWORD_EFFICIENT, 0.0F, -1.5F,
                "arrow_head", "arrow_head"),
        FISHING_ROD("fishing_rod", BlockTags.MINEABLE_WITH_AXE, 1.0F, -2.0F,
                "bow_limb", "bowstring", "arrow_head"),
        STAFF("staff", BlockTags.MINEABLE_WITH_PICKAXE, 3.0F, -2.8F,
                "large_plate", "tool_handle", "tough_binding"),
        FLINT_AND_BRICK("flint_and_brick", null, 0.0F, -2.0F),
        SKY_STAFF("sky_staff", null, 0.0F, -2.0F),
        EARTH_STAFF("earth_staff", null, 0.0F, -2.0F),
        ICHOR_STAFF("ichor_staff", null, 0.0F, -2.0F),
        ENDER_STAFF("ender_staff", null, 0.0F, -2.0F),
        MELTING_PAN("melting_pan", BlockTags.MINEABLE_WITH_PICKAXE, 2.0F, -2.6F,
                "shield_core", "bow_limb"),
        WAR_PICK("war_pick", BlockTags.MINEABLE_WITH_PICKAXE, 0.0F, -2.4F,
                "pick_head", "bow_limb", "bowstring"),
        BATTLESIGN("battlesign", BlockTags.SWORD_EFFICIENT, 2.0F, -2.4F,
                "large_plate", "shield_core"),
        SWASHER("swasher", BlockTags.SWORD_EFFICIENT, 2.0F, -2.2F,
                "small_blade", "tool_handle", "bow_grip"),
        MINOTAUR_AXE("minotaur_axe", BlockTags.MINEABLE_WITH_AXE, 3.0F, -2.8F,
                "small_axe_head", "small_axe_head", "tool_handle");

        private final String id;
        private final String[] parts;
        private final TagKey<net.minecraft.world.level.block.Block> effectiveTag;
        private final float baseAttackDamage;
        private final float baseAttackSpeed;

        ToolKind(String id, TagKey<net.minecraft.world.level.block.Block> effectiveTag,
                 float baseAttackDamage, float baseAttackSpeed, String... parts) {
            this.id = id;
            this.parts = parts.clone();
            this.effectiveTag = effectiveTag;
            this.baseAttackDamage = baseAttackDamage;
            this.baseAttackSpeed = baseAttackSpeed;
        }

        public static ToolKind fromHead(String partId) {
            for (ToolKind value : values()) {
                if (value.parts.length > 0 && value.parts[0].equals(partId)) {
                    return value;
                }
            }
            return null;
        }

        public static ToolKind fromParts(String... requested) {
            for (ToolKind value : values()) {
                if (java.util.Arrays.equals(value.parts, requested)) {
                    return value;
                }
            }
            return null;
        }

        public boolean acceptsHandle(String partId) {
            return parts.length > 1 && parts[1].equals(partId);
        }

        public boolean acceptsBinding(String partId) {
            return parts.length > 2 && parts[2].equals(partId);
        }

        public String id() {
            return id;
        }

        public String headPart() {
            return parts.length > 0 ? parts[0] : "";
        }

        public String handlePart() {
            return parts.length > 1 ? parts[1] : "";
        }

        public String bindingPart() {
            return parts.length > 2 ? parts[2] : "";
        }

        public List<String> parts() {
            return List.of(parts);
        }

        public int partCount() {
            return parts.length;
        }

        private int fixedDurability() {
            return switch (this) {
                case FLINT_AND_BRICK -> 100;
                case SKY_STAFF -> 500;
                case EARTH_STAFF -> 800;
                case ICHOR_STAFF -> 1225;
                case ENDER_STAFF -> 1520;
                default -> 0;
            };
        }

        private float fixedMiningSpeed() {
            return this == MELTING_PAN ? 6.0F : 1.0F;
        }

        private float attackDamageMultiplier() {
            return switch (this) {
                case SLEDGE_HAMMER -> 1.35F;
                case VEIN_HAMMER -> 1.25F;
                case MATTOCK -> 1.10F;
                case PICKADZE -> 1.15F;
                case EXCAVATOR -> 1.20F;
                case BROAD_AXE -> 1.65F;
                case KAMA -> 0.50F;
                case DAGGER -> 0.65F;
                case CLEAVER -> 1.50F;
                default -> 1.0F;
            };
        }

        private float miningSpeedMultiplier() {
            return switch (this) {
                case SLEDGE_HAMMER -> 0.40F;
                case VEIN_HAMMER -> 0.30F;
                case MATTOCK -> 1.10F;
                case PICKADZE -> 0.75F;
                case EXCAVATOR, BROAD_AXE -> 0.30F;
                case DAGGER -> 0.75F;
                case SWORD -> 0.50F;
                case CLEAVER -> 0.25F;
                case SCYTHE -> 0.45F;
                default -> 1.0F;
            };
        }

        private float durabilityMultiplier() {
            return switch (this) {
                case SLEDGE_HAMMER -> 4.0F;
                case VEIN_HAMMER -> 5.0F;
                case MATTOCK -> 1.25F;
                case PICKADZE -> 1.30F;
                case EXCAVATOR -> 3.75F;
                case BROAD_AXE -> 4.25F;
                case DAGGER -> 0.75F;
                case SWORD -> 1.10F;
                case CLEAVER -> 3.50F;
                case SCYTHE -> 2.50F;
                case CROSSBOW -> 2.0F;
                case LONGBOW -> 1.50F;
                case FISHING_ROD -> 1.50F;
                default -> 1.0F;
            };
        }

        private int builtinModifierLevel(String modifier) {
            return switch (this) {
                case PICKAXE -> "pierce".equals(modifier) ? 1 : 0;
                case SLEDGE_HAMMER -> "smite".equals(modifier) ? 2 : 0;
                case VEIN_HAMMER -> "pierce".equals(modifier) ? 2 : 0;
                case FLINT_AND_BRICK -> "fiery".equals(modifier) ? 1 : 0;
                case BATTLESIGN -> "blocking".equals(modifier) ? 1 : 0;
                default -> 0;
            };
        }

        public boolean ranged() {
            return this == LONGBOW || this == CROSSBOW || this == JAVELIN
                    || this == SHURIKEN || this == THROWING_AXE || this == FISHING_ROD
                    || this == WAR_PICK;
        }

        public boolean requiresAmmo() {
            return this == LONGBOW || this == CROSSBOW || this == WAR_PICK;
        }

        public double projectileDamage() {
            return switch (this) {
                case JAVELIN -> 2.0D;
                case SHURIKEN -> 1.0D;
                case THROWING_AXE -> 2.5D;
                case WAR_PICK -> 1.5D;
                default -> 0.0D;
            };
        }
    }
}
