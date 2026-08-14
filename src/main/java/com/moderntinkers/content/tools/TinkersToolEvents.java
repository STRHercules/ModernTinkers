package com.moderntinkers.content.tools;

import com.moderntinkers.ModernTinkers;
import com.moderntinkers.content.fluid.MaterialFluids;
import com.moderntinkers.content.recipe.TinkerRecipeManager;
import com.moderntinkers.content.smeltery.MelterBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.common.Tags;

import net.minecraft.core.Direction;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Server-side hooks for tool modifiers whose behavior is outside Item APIs. */
public final class TinkersToolEvents {
    private static final ThreadLocal<Boolean> AREA_BREAK = ThreadLocal.withInitial(() -> false);
    private static final TagKey<net.minecraft.world.damagesource.DamageType> MAGIC_PROTECTION =
            TagKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(ModernTinkers.MOD_ID,
                            "magic_protection"));

    private TinkersToolEvents() {}

    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (TinkersToolItem.isAssembled(tool) && tool.getItem() instanceof TinkersToolItem tinkerTool
                && tinkerTool.kind() == TinkersToolItem.ToolKind.MELTING_PAN
                && tinkerTool.isCorrectToolForDrops(tool, event.getState())) {
            meltDropsIntoTool(tool, event);
        }
        if (TinkersToolItem.isAssembled(tool) && tool.getItem() instanceof TinkersToolItem tinkerTool
                && tinkerTool.kind() == TinkersToolItem.ToolKind.WAR_PICK
                && event.getBreaker() instanceof Player player
                && TinkersToolItem.hasArrow(player)) {
            // BlockDropsEvent is after the break, so canceled or failed breaks
            // cannot grant the war-pick's charge.
            TinkersToolItem.addWarCharge(tool);
        }
        if (!TinkersToolItem.isAssembled(tool)) {
            return;
        }
        if (TinkersToolItem.hasModifier(tool, "silky_cloth")) {
            ItemStack blockItem = event.getState().getBlock().asItem().getDefaultInstance();
            if (blockItem.isEmpty() || blockItem.is(Items.AIR)) {
                return;
            }
            event.getDrops().clear();
            event.getDrops().add(new ItemEntity(event.getLevel(),
                    event.getPos().getX() + 0.5D,
                    event.getPos().getY() + 0.5D,
                    event.getPos().getZ() + 0.5D,
                    blockItem));
            event.setDroppedExperience(0);
            return;
        }
        if (TinkersToolItem.hasModifier(tool, "autosmelt")) {
            for (ItemEntity drop : event.getDrops()) {
                ItemStack input = drop.getItem();
                ItemStack result = smelted(input.copyWithCount(1), event.getLevel());
                if (!result.isEmpty()) {
                    long count = (long) input.getCount() * result.getCount();
                    drop.setItem(result.copyWithCount((int) Math.min(Integer.MAX_VALUE, count)));
                }
            }
        }
        int fortune = TinkersToolItem.modifierLevel(tool, "fortune");
        if (fortune > 0 && !event.getDrops().isEmpty()) {
            java.util.List<ItemEntity> extra = new java.util.ArrayList<>();
            for (ItemEntity drop : event.getDrops()) {
                int copies = 0;
                while (copies < fortune && event.getLevel().getRandom().nextBoolean()) {
                    extra.add(new ItemEntity(event.getLevel(), drop.getX(), drop.getY(), drop.getZ(),
                            drop.getItem().copy()));
                    copies++;
                }
            }
            event.getDrops().addAll(extra);
        }
    }

    /** Melting-pan equivalent of the reference forced-melting loot hook. */
    private static void meltDropsIntoTool(ItemStack tool, BlockDropsEvent event) {
        Iterator<ItemEntity> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemEntity entity = iterator.next();
            ItemStack drop = entity.getItem();
            FluidStack perItem = meltingOutput(drop.copyWithCount(1));
            if (perItem.isEmpty() || perItem.getAmount() <= 0) {
                continue;
            }
            int possible = Math.min(drop.getCount(),
                    TinkersToolItem.toolTankCapacity(tool) / perItem.getAmount());
            if (possible <= 0) {
                continue;
            }
            FluidStack total = perItem.copy();
            total.setAmount(perItem.getAmount() * possible);
            int accepted = TinkersToolItem.fillToolFluid(tool, total,
                    IFluidHandler.FluidAction.SIMULATE);
            int copies = Math.min(possible, accepted / perItem.getAmount());
            if (copies <= 0) {
                continue;
            }
            total.setAmount(perItem.getAmount() * copies);
            TinkersToolItem.fillToolFluid(tool, total, IFluidHandler.FluidAction.EXECUTE);
            drop.shrink(copies);
            if (drop.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private static FluidStack meltingOutput(ItemStack stack) {
        TinkerRecipeManager.MeltingRecipe recipe = TinkerRecipeManager.findMelting(stack);
        if (recipe != null && recipe.inputCount() == 1) {
            MaterialFluids.FluidSet fluid = MaterialFluids.get(recipe.fluidId());
            if (fluid != null && fluid.source().isBound()) {
                return new FluidStack(fluid.source().get(), recipe.amount());
            }
        }
        List<FluidStack> outputs = MelterBlockEntity.meltingOutputs(stack, 1);
        return outputs.size() == 1 ? outputs.get(0) : FluidStack.EMPTY;
    }

    /** Breaks the compact 3x3 mining plane, vein, or tree owned by the tool. */
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || AREA_BREAK.get()) {
            return;
        }
        LivingTool livingTool = LivingTool.from(event.getPlayer().getMainHandItem());
        if (livingTool == null || !livingTool.areaTool()) {
            return;
        }
        Level level = event.getPlayer().level();
        if (level.isClientSide) {
            return;
        }
        AREA_BREAK.set(true);
        try {
            switch (livingTool.kind()) {
                case SLEDGE_HAMMER, EXCAVATOR -> {
                    int expanded = TinkersToolItem.modifierLevel(livingTool.stack(), "expanded");
                    breakPlane(level, event.getPlayer(), event.getPos(), livingTool,
                            1 + (expanded + 1) / 2, 1 + expanded / 2);
                }
                case VEIN_HAMMER ->
                        breakVein(level, event.getPlayer(), event.getPos(), livingTool,
                                2 + TinkersToolItem.modifierLevel(livingTool.stack(), "expanded"));
                case HAND_AXE, KAMA, WAR_PICK, SKY_STAFF ->
                        breakCircle(level, event.getPlayer(), event.getPos(), livingTool,
                                1 + TinkersToolItem.modifierLevel(livingTool.stack(), "expanded"),
                                livingTool.kind() == TinkersToolItem.ToolKind.KAMA);
                case PICKADZE -> breakExpandedBox(level, event.getPlayer(), event.getPos(),
                        livingTool, 0, TinkersToolItem.modifierLevel(livingTool.stack(), "expanded"), 0);
                case SCYTHE -> {
                    int expanded = TinkersToolItem.modifierLevel(livingTool.stack(), "expanded");
                    int expansionCycle = (expanded + 1) / 2;
                    breakBox(level, event.getPlayer(), event.getPos(), livingTool,
                            1 + expansionCycle, 1 + expansionCycle,
                            2 + (expanded / 2) * 2);
                }
                case EARTH_STAFF -> breakPitchBox(level, event.getPlayer(), event.getPos(),
                        livingTool, TinkersToolItem.modifierLevel(livingTool.stack(), "expanded"));
                case ICHOR_STAFF -> breakVein(level, event.getPlayer(), event.getPos(), livingTool,
                        TinkersToolItem.modifierLevel(livingTool.stack(), "expanded"));
                case ENDER_STAFF -> breakEnderBox(level, event.getPlayer(), event.getPos(), livingTool);
                case BROAD_AXE -> {
                    int expanded = TinkersToolItem.modifierLevel(livingTool.stack(), "expanded");
                    breakTree(level, event.getPlayer(), event.getPos(), livingTool,
                            (expanded + 1) / 2, expanded / 2);
                }
                case MINOTAUR_AXE -> breakColumn(level, event.getPlayer(), event.getPos(), livingTool);
                case MATTOCK -> { }
                case MELTING_PAN -> breakVein(level, event.getPlayer(), event.getPos(), livingTool,
                        TinkersToolItem.modifierLevel(livingTool.stack(), "expanded"));
                default -> { }
            }
        } finally {
            AREA_BREAK.set(false);
        }
    }

    private static void breakPlane(Level level, Player player, BlockPos origin,
                                   LivingTool tool, int widthRadius, int heightRadius) {
        Direction look = Direction.getNearest(player.getLookAngle().x,
                player.getLookAngle().y, player.getLookAngle().z);
        for (int first = -widthRadius; first <= widthRadius; first++) {
            for (int second = -heightRadius; second <= heightRadius; second++) {
                if (first == 0 && second == 0) {
                    continue;
                }
                BlockPos target = switch (look.getAxis()) {
                    case X -> origin.offset(0, first, second);
                    case Y -> origin.offset(first, 0, second);
                    case Z -> origin.offset(first, second, 0);
                };
                breakAdditional(level, player, target, tool);
            }
        }
    }

    private static void breakCircle(Level level, Player player, BlockPos origin,
                                    LivingTool tool, int diameter, boolean is3D) {
        if (diameter <= 1) {
            return;
        }
        Direction look = Direction.getNearest(player.getLookAngle().x,
                player.getLookAngle().y, player.getLookAngle().z);
        int radius = diameter / 2;
        int radiusSquared = diameter * diameter / 4;
        for (int first = -radius; first <= radius && !tool.stack().isEmpty(); first++) {
            for (int second = -radius; second <= radius && !tool.stack().isEmpty(); second++) {
                if (first * first + second * second > radiusSquared) {
                    continue;
                }
                BlockPos target = planeOffset(origin, look, first, second);
                if (is3D) {
                    for (int depth = 0; depth <= radius && !tool.stack().isEmpty(); depth++) {
                        BlockPos depthTarget = target.relative(look, depth);
                        if (!depthTarget.equals(origin)) {
                            breakAdditional(level, player, depthTarget, tool);
                        }
                    }
                } else if (!target.equals(origin)) {
                    breakAdditional(level, player, target, tool);
                }
            }
        }
    }

    private static void breakBox(Level level, Player player, BlockPos origin,
                                 LivingTool tool, int widthRadius, int heightRadius, int depth) {
        breakExpandedBox(level, player, origin, tool, widthRadius, heightRadius, depth);
    }

    private static void breakExpandedBox(Level level, Player player, BlockPos origin,
                                         LivingTool tool, int widthRadius, int heightRadius,
                                         int depth) {
        Direction look = Direction.getNearest(player.getLookAngle().x,
                player.getLookAngle().y, player.getLookAngle().z);
        for (int distance = 0; distance <= depth && !tool.stack().isEmpty(); distance++) {
            for (int first = -widthRadius; first <= widthRadius && !tool.stack().isEmpty(); first++) {
                for (int second = -heightRadius; second <= heightRadius && !tool.stack().isEmpty(); second++) {
                    if (distance == 0 && first == 0 && second == 0) {
                        continue;
                    }
                    BlockPos target = planeOffset(origin, look, first, second)
                            .relative(look, distance);
                    breakAdditional(level, player, target, tool);
                }
            }
        }
    }

    private static void breakPitchBox(Level level, Player player, BlockPos origin,
                                      LivingTool tool, int expanded) {
        if (expanded <= 0) {
            return;
        }
        int depth = 2 * ((expanded + 1) / 2);
        int height = expanded / 2;
        Direction look = player.getDirection();
        Direction width = look.getClockWise();
        Direction heightDirection = Direction.UP;
        Direction depthDirection = look;
        float pitch = player.getXRot();
        if (pitch < -60.0F) {
            depthDirection = Direction.UP;
            heightDirection = look;
        } else if (pitch > 60.0F) {
            depthDirection = Direction.DOWN;
            heightDirection = look;
        }
        breakOrientedBox(level, player, origin, tool, width, heightDirection,
                depthDirection, 0, height, depth, true);
    }

    private static void breakEnderBox(Level level, Player player, BlockPos origin,
                                      LivingTool tool) {
        int expanded = TinkersToolItem.modifierLevel(tool.stack(), "expanded");
        if (expanded <= 0) {
            return;
        }
        int width = (expanded + 1) / 2;
        int height = (expanded + 1) / 2;
        int depth = (expanded / 2) * 2;
        breakExpandedBox(level, player, origin, tool, width, height, depth);
    }

    private static void breakOrientedBox(Level level, Player player, BlockPos origin,
                                         LivingTool tool, Direction widthDirection,
                                         Direction heightDirection, Direction depthDirection,
                                         int widthRadius, int heightRadius, int depth,
                                         boolean traverseDown) {
        for (int distance = 0; distance <= depth && !tool.stack().isEmpty(); distance++) {
            for (int width = -widthRadius; width <= widthRadius && !tool.stack().isEmpty(); width++) {
                int minHeight = traverseDown ? -heightRadius : 0;
                for (int height = minHeight; height <= heightRadius && !tool.stack().isEmpty(); height++) {
                    if (distance == 0 && width == 0 && height == 0) {
                        continue;
                    }
                    BlockPos target = origin.relative(widthDirection, width)
                            .relative(heightDirection, height)
                            .relative(depthDirection, distance);
                    breakAdditional(level, player, target, tool);
                }
            }
        }
    }

    private static BlockPos planeOffset(BlockPos origin, Direction look,
                                        int first, int second) {
        return switch (look.getAxis()) {
            case X -> origin.offset(0, first, second);
            case Y -> origin.offset(first, 0, second);
            case Z -> origin.offset(first, second, 0);
        };
    }

    private static void breakVein(Level level, Player player, BlockPos origin,
                                  LivingTool tool, int maxDistance) {
        if (maxDistance <= 0) {
            return;
        }
        var source = level.getBlockState(origin);
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);
        int broken = 0;
        while (!queue.isEmpty() && broken < 64 && !tool.stack().isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!current.equals(origin)) {
                if (!breakAdditional(level, player, current, tool)) {
                    continue;
                }
                broken++;
            }
            if (current.equals(origin)) {
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);
                    if (visited.add(next) && level.getBlockState(next).is(source.getBlock())) {
                        queue.addLast(next);
                    }
                }
            } else {
                int distance = current.distManhattan(origin);
                if (distance < maxDistance) {
                    for (Direction direction : Direction.values()) {
                        BlockPos next = current.relative(direction);
                        if (visited.add(next) && level.getBlockState(next).is(source.getBlock())
                                && next.distManhattan(origin) <= maxDistance) {
                            queue.addLast(next);
                        }
                    }
                }
            }
        }
    }

    private static void breakTree(Level level, Player player, BlockPos origin, LivingTool tool,
                                  int extraWidth, int extraDepth) {
        BlockState source = level.getBlockState(origin);
        if (!source.is(BlockTags.LOGS)) {
            // Broad axes still provide their normal fallback box when used on
            // planks, stems, or other axe-effective blocks.  The tree walker
            // is deliberately reserved for actual logs.
            breakExpandedBox(level, player, origin, tool,
                    1 + extraWidth, 1 + extraWidth, 1 + extraDepth);
            return;
        }

        Block sourceBlock = source.getBlock();
        Direction depthDirection = player.getDirection();
        Direction widthDirection = depthDirection.getClockWise();
        ArrayDeque<TreeNode> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.addLast(new TreeNode(origin, false));
        visited.add(origin);

        // These bounds describe the initial trunk/expanded footprint.  A
        // branch is allowed to leave that footprint only within the same
        // bounded horizontal distance as the reference TreeAOEIterator.
        int minX = origin.getX();
        int maxX = minX;
        int minZ = origin.getZ();
        int maxZ = minZ;
        for (int depth = 0; depth <= extraDepth; depth++) {
            for (int width = -extraWidth; width <= extraWidth; width++) {
                if (depth == 0 && width == 0) {
                    continue;
                }
                BlockPos target = origin.relative(depthDirection, depth)
                        .relative(widthDirection, width);
                if (level.getBlockState(target).getBlock() != sourceBlock) {
                    continue;
                }
                queue.addLast(new TreeNode(target, false));
                visited.add(target);
                minX = Math.min(minX, target.getX());
                maxX = Math.max(maxX, target.getX());
                minZ = Math.min(minZ, target.getZ());
                maxZ = Math.max(maxZ, target.getZ());
            }
        }

        int broken = 0;
        while (!queue.isEmpty() && broken < 128 && !tool.stack().isEmpty()) {
            TreeNode node = queue.removeFirst();
            BlockPos current = node.pos();
            if (!current.equals(origin) && breakAdditional(level, player, current, tool)) {
                broken++;
            }

            // Trees grow upward and out through horizontal branches.  Never
            // flood downward into connected logs or roots.
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                enqueueTreeLog(level, queue, visited, sourceBlock, current,
                        direction, node.branch(), minX, maxX, minZ, maxZ);
            }
            enqueueTreeLog(level, queue, visited, sourceBlock, current,
                    Direction.UP, node.branch(), minX, maxX, minZ, maxZ);
        }
    }

    private static void enqueueTreeLog(Level level, ArrayDeque<TreeNode> queue,
                                       Set<BlockPos> visited, Block sourceBlock,
                                       BlockPos current, Direction direction,
                                       boolean currentBranch, int minX, int maxX,
                                       int minZ, int maxZ) {
        BlockPos next = current.relative(direction);
        if (!visited.add(next) || level.getBlockState(next).getBlock() != sourceBlock
                || next.getY() < current.getY()) {
            return;
        }
        boolean outsideTrunk = next.getX() < minX || next.getX() > maxX
                || next.getZ() < minZ || next.getZ() > maxZ;
        if (!outsideTrunk) {
            queue.addLast(new TreeNode(next, currentBranch));
            return;
        }
        int deltaX = Math.min(Math.abs(next.getX() - minX), Math.abs(next.getX() - maxX));
        int deltaZ = Math.min(Math.abs(next.getZ() - minZ), Math.abs(next.getZ() - maxZ));
        if (deltaX + deltaZ > 10) {
            return;
        }
        // A new branch must emerge into open space; once established it may
        // continue upward or sideways through the branch canopy.
        if (!currentBranch && level.getBlockState(next.below()).canOcclude()) {
            return;
        }
        queue.addLast(new TreeNode(next, true));
    }

    private record TreeNode(BlockPos pos, boolean branch) {}

    private static void breakColumn(Level level, Player player, BlockPos origin,
                                    LivingTool tool) {
        int expanded = TinkersToolItem.modifierLevel(tool.stack(), "expanded");
        int width = expanded >= 1 ? 1 : 0;
        int depth = expanded >= 2 ? 1 : 0;
        for (int offset = 1; offset <= 5 && !tool.stack().isEmpty(); offset++) {
            for (int x = -width; x <= width && !tool.stack().isEmpty(); x++) {
                for (int z = -depth; z <= depth && !tool.stack().isEmpty(); z++) {
                    BlockPos target = origin.offset(x, offset, z);
                if (!level.getBlockState(target).is(BlockTags.LOGS)
                        || !breakAdditional(level, player, target, tool)) {
                        continue;
                    }
                }
            }
        }
    }

    private static boolean breakAdditional(Level level, Player player, BlockPos target,
                                           LivingTool tool) {
        var state = level.getBlockState(target);
        if (state.isAir() || state.hasBlockEntity()
                || state.getDestroySpeed(level, target) < 0.0F
                || !tool.tool().isCorrectToolForDrops(tool.stack(), state)) {
            return false;
        }
        if (!level.destroyBlock(target, true, player)) {
            return false;
        }
        TinkersToolItem.damageTool(tool.stack(), 1, player, EquipmentSlot.MAINHAND);
        return true;
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        if (!TinkersToolItem.isAssembled(tool)) {
            return;
        }
        int haste = TinkersToolItem.modifierLevel(tool, "haste");
        if (haste > 0) {
            event.setNewSpeed(event.getNewSpeed() * (1.0F + haste * 0.25F));
        }
        if (TinkersToolItem.hasMaterialTrait(tool, "aquatic")
                && event.getEntity().isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) {
            event.setNewSpeed(event.getNewSpeed() * 1.25F);
        }
        if (TinkersToolItem.hasMaterialTrait(tool, "soulspeed")
                && event.getState().is(BlockTags.SOUL_SPEED_BLOCKS)) {
            event.setNewSpeed(event.getNewSpeed() * 1.35F);
        }
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (!TinkersToolItem.isAssembled(tool)) {
            return;
        }
        int looting = TinkersToolItem.modifierLevel(tool, "looting");
        if (looting > 0 && !event.getDrops().isEmpty()) {
            java.util.List<ItemEntity> extra = new java.util.ArrayList<>();
            for (ItemEntity drop : event.getDrops()) {
                for (int count = 0; count < looting; count++) {
                    if (event.getEntity().getRandom().nextBoolean()) {
                        extra.add(new ItemEntity(event.getEntity().level(), drop.getX(), drop.getY(),
                                drop.getZ(), drop.getItem().copy()));
                    }
                }
            }
            event.getDrops().addAll(extra);
        }
        int beheading = TinkersToolItem.modifierLevel(tool, "beheading");
        if (beheading > 0 && event.getEntity().getRandom().nextInt(3) < beheading) {
            ItemStack skull = event.getEntity().getPickResult();
            if (!isHeadDrop(skull)) {
                skull = ItemStack.EMPTY;
            }
            EntityType<?> type = event.getEntity().getType();
            if (skull.isEmpty() && type == EntityType.WITHER_SKELETON) {
                skull = new ItemStack(Items.WITHER_SKELETON_SKULL);
            } else if (skull.isEmpty() && type == EntityType.SKELETON) {
                skull = new ItemStack(Items.SKELETON_SKULL);
            } else if (skull.isEmpty() && type == EntityType.ZOMBIE) {
                skull = new ItemStack(Items.ZOMBIE_HEAD);
            } else if (skull.isEmpty() && type == EntityType.CREEPER) {
                skull = new ItemStack(Items.CREEPER_HEAD);
            } else if (skull.isEmpty() && type == EntityType.ENDER_DRAGON) {
                skull = new ItemStack(Items.DRAGON_HEAD);
            }
            if (!skull.isEmpty()) {
                event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(),
                        event.getEntity().getY(), event.getEntity().getZ(), skull));
            }
        }
    }

    public static void onLivingExperience(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null) {
            return;
        }
        int dropped = event.getDroppedExperience();
        int remaining = dropped;
        for (ItemStack stack : player.getAllSlots()) {
            if (!isModifiable(stack)) {
                continue;
            }
            int experience = TinkersToolItem.modifierLevel(stack, "experience");
            if (experience > 0) {
                TinkersToolItem.addStoredExperience(stack, dropped * experience);
            }
            int mending = TinkersToolItem.modifierLevel(stack, "mending");
            if (mending > 0 && stack.isDamaged() && remaining > 0) {
                int repair = Math.min(stack.getDamageValue(), remaining * 2 * mending);
                stack.setDamageValue(stack.getDamageValue() - repair);
                remaining = Math.max(0, remaining - (repair + 1) / 2);
            }
        }
        event.setDroppedExperience(remaining);
    }

    public static void onArmorHurt(ArmorHurtEvent event) {
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack armor = event.getArmorItemStack(slot);
            if (!TinkersArmorItem.isAssembled(armor)) {
                continue;
            }
            float reduction = 0.05F * TinkersToolItem.modifierLevel(armor, "cobalt_reinforcement");
            if (event.getDamageSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                reduction += 0.20F * TinkersToolItem.modifierLevel(armor, "seared_reinforcement");
            }
            if (event.getDamageSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
                reduction += 0.20F * TinkersToolItem.modifierLevel(armor, "iron_reinforcement");
            }
            if (event.getDamageSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
                reduction += 0.20F * TinkersToolItem.modifierLevel(armor, "obsidian_reinforcement");
            }
            if (event.getDamageSource().is(Tags.DamageTypes.IS_MAGIC)
                    || event.getDamageSource().is(MAGIC_PROTECTION)) {
                reduction += 0.10F * TinkersToolItem.modifierLevel(armor,
                        "gold_reinforcement");
            }
            if (isAirborne(event.getEntity())) {
                reduction += 0.10F * TinkersToolItem.modifierLevel(armor, "dragonborn");
            }
            Float original = event.getOriginalDamage(slot);
            if (original != null && reduction > 0.0F) {
                event.setNewDamage(slot, original * Math.max(0.0F, 1.0F - Math.min(0.8F, reduction)));
            }
        }
    }

    public static void onShieldBlock(LivingShieldBlockEvent event) {
        ItemStack shield = event.getEntity().getUseItem();
        boolean tinkerShield = TinkersShieldItem.isAssembled(shield);
        boolean battlesign = TinkersToolItem.isAssembled(shield)
                && shield.getItem() instanceof TinkersToolItem tool
                && tool.kind() == TinkersToolItem.ToolKind.BATTLESIGN;
        if (!tinkerShield && !battlesign) {
            return;
        }
        int reinforcement = TinkersToolItem.modifierLevel(shield, "cobalt_reinforcement");
        if (reinforcement > 0) {
            event.setShieldDamage(Math.max(0.0F, event.shieldDamage() - reinforcement));
        }
    }

    /** Battlesigns are tools rather than ShieldItems, so they need their own block path. */
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living
                ? living : null;
        if (attacker != null && attacker != entity && isAirborne(attacker)
                && !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
            int dragonborn = highestModifier(attacker, "dragonborn");
            if (dragonborn > 0) {
                event.setAmount(event.getAmount() * (1.0F + 0.05F * dragonborn));
            }
        }
        if (!entity.level().isClientSide && event.getAmount() > 0.0F) {
            if (attacker != null && attacker != entity) {
                for (ItemStack armor : entity.getArmorSlots()) {
                    TinkersToolItem.spillFluid(armor, attacker, entity, 1.0F);
                }
                TinkersToolItem.spillFluid(entity.getOffhandItem(), attacker, entity, 1.0F);
            }
        }
        ItemStack shield = entity.getUseItem();
        if (TinkersToolItem.isAssembled(shield)
                && shield.getItem() instanceof TinkersToolItem tool
                && tool.kind() == TinkersToolItem.ToolKind.BATTLESIGN
                && canBattlesignBlock(entity, event.getSource())
                && event.getAmount() > 0.0F) {
            float blocked = Math.min(50.0F, event.getAmount());
            event.setAmount(event.getAmount() - blocked);
            if (!entity.level().isClientSide) {
                TinkersToolItem.damageTool(shield, 1, entity, EquipmentSlot.MAINHAND);
            }
        }

        if (event.getAmount() <= 0.0F
                || event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }
        float remaining = event.getAmount();
        for (ItemStack protectedStack : protectedEquipment(entity)) {
            int level = TinkersToolItem.overshieldLevel(protectedStack);
            int capacity = TinkersToolItem.overslimeAmount(protectedStack);
            if (level <= 0 || capacity <= 0) {
                continue;
            }
            int target = Math.max(1, 2 * level);
            int consumed = Math.min(capacity, target);
            remaining = Math.max(0.0F,
                    remaining - 1.25F * level * consumed / (float) target);
            if (!entity.level().isClientSide) {
                TinkersToolItem.consumeOverslime(protectedStack, consumed);
            }
            if (remaining <= 0.0F) {
                break;
            }
        }
        event.setAmount(remaining);
    }

    private static boolean isAirborne(LivingEntity entity) {
        return !entity.onGround() && !entity.isInWater() && !entity.onClimbable();
    }

    private static int highestModifier(LivingEntity entity, String modifier) {
        int highest = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            if (TinkersArmorItem.isAssembled(stack)) {
                highest = Math.max(highest, TinkersToolItem.modifierLevel(stack, modifier));
            }
        }
        return highest;
    }

    private static List<ItemStack> protectedEquipment(LivingEntity entity) {
        List<ItemStack> result = new java.util.ArrayList<>();
        for (ItemStack stack : entity.getArmorSlots()) {
            if (TinkersArmorItem.isAssembled(stack)) {
                result.add(stack);
            }
        }
        ItemStack offhand = entity.getOffhandItem();
        if (TinkersShieldItem.isAssembled(offhand)) {
            result.add(offhand);
        }
        return result;
    }

    private static boolean canBattlesignBlock(LivingEntity entity, DamageSource source) {
        if (!entity.isUsingItem() || source.is(DamageTypeTags.BYPASSES_SHIELD)) {
            return false;
        }
        Vec3 origin = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null && source.getEntity() != null) {
            sourcePosition = source.getEntity().position();
        }
        if (sourcePosition == null) {
            return true;
        }
        Vec3 toSource = sourcePosition.subtract(origin);
        Vec3 look = entity.getLookAngle();
        Vec3 horizontalSource = new Vec3(toSource.x, 0.0D, toSource.z);
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalSource.lengthSqr() < 1.0E-6D
                || horizontalLook.lengthSqr() < 1.0E-6D) {
            return true;
        }
        return horizontalLook.normalize().dot(horizontalSource.normalize())
                >= Math.cos(Math.toRadians(60.0D));
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (TinkersToolItem.hasMaterialTrait(tool, "bloodbound")) {
            player.heal(0.5F + TinkersToolItem.modifierLevel(tool, "necrotic") * 0.25F);
        }
    }

    private static boolean isModifiable(ItemStack stack) {
        return TinkersToolItem.isAssembled(stack) || TinkersArmorItem.isAssembled(stack)
                || TinkersShieldItem.isAssembled(stack);
    }

    private static boolean isHeadDrop(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        return path.endsWith("_head") || path.endsWith("_skull")
                || path.equals("player_head") || path.equals("player_wall_head");
    }

    public static void onItemFished(ItemFishedEvent event) {
        Player player = event.getHookEntity().getPlayerOwner();
        if (player == null) {
            return;
        }
        if (!TinkersToolItem.isAssembled(player.getMainHandItem())) {
            return;
        }
        int luck = TinkersToolItem.modifierLevel(player.getMainHandItem(), "luck");
        if (luck <= 0) {
            return;
        }
        for (int count = 0; count < luck; count++) {
            if (player.getRandom().nextBoolean() && !event.getDrops().isEmpty()) {
                event.getDrops().add(event.getDrops().get(player.getRandom().nextInt(
                        event.getDrops().size())).copy());
            }
        }
    }

    /** Applies the small set of equipment traits that need a living tick. */
    public static void onLivingTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide || event.getEntity().tickCount % 20 != 0) {
            return;
        }
        boolean fireResistant = false;
        boolean aquatic = false;
        for (ItemStack stack : event.getEntity().getArmorSlots()) {
            if (!TinkersArmorItem.isAssembled(stack)) {
                continue;
            }
            fireResistant |= TinkersToolItem.hasMaterialTrait(stack, "flamewake")
                    || TinkersToolItem.hasModifier(stack, "fiery");
            aquatic |= TinkersToolItem.hasMaterialTrait(stack, "aquatic");
        }
        if (fireResistant) {
            event.getEntity().addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 40, 0, true, false));
        }
        if (aquatic && event.getEntity().isInWater()) {
            event.getEntity().addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WATER_BREATHING, 40, 0, true, false));
        }
        List<ItemStack> equipment = new java.util.ArrayList<>();
        event.getEntity().getArmorSlots().forEach(equipment::add);
        equipment.add(event.getEntity().getMainHandItem());
        equipment.add(event.getEntity().getOffhandItem());
        for (ItemStack stack : equipment) {
            if (TinkersToolItem.overslimeCapacity(stack)
                    > TinkersToolItem.overslimeAmount(stack)
                    && (TinkersToolItem.hasMaterialTrait(stack, "slimey")
                    || TinkersToolItem.hasMaterialTrait(stack, "slime"))) {
                TinkersToolItem.addOverslime(stack, 1);
            }
        }
    }

    private record LivingTool(ItemStack stack, TinkersToolItem tool,
                              TinkersToolItem.ToolKind kind) {
        private static LivingTool from(ItemStack stack) {
            return TinkersToolItem.isAssembled(stack)
                    && stack.getItem() instanceof TinkersToolItem tool
                    ? new LivingTool(stack, tool, tool.kind()) : null;
        }

        private boolean areaTool() {
            return kind == TinkersToolItem.ToolKind.SLEDGE_HAMMER
                    || kind == TinkersToolItem.ToolKind.VEIN_HAMMER
                    || kind == TinkersToolItem.ToolKind.PICKADZE
                    || kind == TinkersToolItem.ToolKind.EXCAVATOR
                    || kind == TinkersToolItem.ToolKind.HAND_AXE
                    || kind == TinkersToolItem.ToolKind.BROAD_AXE
                    || kind == TinkersToolItem.ToolKind.KAMA
                    || kind == TinkersToolItem.ToolKind.SCYTHE
                    || kind == TinkersToolItem.ToolKind.MELTING_PAN
                    || kind == TinkersToolItem.ToolKind.SKY_STAFF
                    || kind == TinkersToolItem.ToolKind.EARTH_STAFF
                    || kind == TinkersToolItem.ToolKind.ICHOR_STAFF
                    || kind == TinkersToolItem.ToolKind.ENDER_STAFF
                    || kind == TinkersToolItem.ToolKind.MINOTAUR_AXE
                    || kind == TinkersToolItem.ToolKind.WAR_PICK;
        }
    }

    private static ItemStack smelted(ItemStack input, Level level) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,
                        new SingleRecipeInput(input), level)
                .map(RecipeHolder::value)
                .map(recipe -> recipe.getResultItem(level.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }
}
