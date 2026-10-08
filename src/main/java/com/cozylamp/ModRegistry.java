package com.cozylamp;

import com.cozylamp.block.DeskLampBlock;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModRegistry {
    private ModRegistry() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CozyLamp.MOD_ID, path);
    }

    public static final SoundEvent LAMP_CLICK = Registry.register(
            BuiltInRegistries.SOUND_EVENT, id("lamp_click"),
            SoundEvent.createVariableRangeEvent(id("lamp_click")));

    public static final Block DESK_LAMP = Registry.register(
            BuiltInRegistries.BLOCK, id("desk_lamp"),
            new DeskLampBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.6f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(DeskLampBlock::lightLevel)));

    public static final Item DESK_LAMP_ITEM = Registry.register(
            BuiltInRegistries.ITEM, id("desk_lamp"),
            new BlockItem(DESK_LAMP, new Item.Properties()));

    public static final CreativeModeTab TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB, id("main"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup.cozylamp.main"))
                    .icon(() -> new ItemStack(DESK_LAMP_ITEM))
                    .displayItems((params, output) -> output.accept(DESK_LAMP_ITEM))
                    .build());

    /** Вызов нужен, чтобы загрузить класс и выполнить регистрацию. */
    public static void init() {}
}
