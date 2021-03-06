package dev.mrsterner.besmirchment.common.registry;

import dev.mrsterner.besmirchment.common.Besmirchment;
import dev.mrsterner.besmirchment.common.block.PhylacteryBlock;
import dev.mrsterner.besmirchment.common.entity.LichGemItem;
import dev.mrsterner.besmirchment.common.item.DemonicDeedItem;
import dev.mrsterner.besmirchment.common.item.ScrollOfTormentItem;
import dev.mrsterner.besmirchment.common.item.VampireSunscreenItem;
import dev.mrsterner.besmirchment.common.item.WitchyDyeItem;
import moriyashiine.bewitchment.api.item.BroomItem;
import moriyashiine.bewitchment.common.block.CoffinBlock;
import moriyashiine.bewitchment.common.registry.BWObjects;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.DyeColor;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class BSMObjects {
    public static final Block ELITE_COFFIN = new CoffinBlock(DyeColor.CYAN, FabricBlockSettings.copyOf(BWObjects.BLACK_COFFIN).nonOpaque().luminance(state -> state.get(CoffinBlock.OCCUPIED) ? 3 : 10));
    public static final Block PHYLACTERY = new PhylacteryBlock();

    public static final Item FINAL_BROOM = new BroomItem(new FabricItemSettings().maxCount(1).group(Besmirchment.BESMIRCHMENT), BSMEntityTypes.FINAL_BROOM);
    public static final WitchyDyeItem WITCHY_DYE = new WitchyDyeItem(new FabricItemSettings().maxCount(16).group(Besmirchment.BESMIRCHMENT));
    public static final Item SCROLL_OF_TORMENT = new ScrollOfTormentItem();
    public static final Item VAMPIRE_SUNSCREEN = new VampireSunscreenItem();
    public static final Item DEMONIC_DEED = new DemonicDeedItem();
    public static final Item LICH_GEM = new LichGemItem();

    public static final Item WEREPYRE_SPAWN_EGG = new SpawnEggItem(BSMEntityTypes.WEREPYRE, 0x844400, 0x880000, new FabricItemSettings().group(Besmirchment.BESMIRCHMENT));
    public static final Item BEELZEBUB_SPAWN_EGG = new SpawnEggItem(BSMEntityTypes.BEELZEBUB, 0x1E0000, 0xEC0000, new FabricItemSettings().group(Besmirchment.BESMIRCHMENT));

    public static void init(){
        BSMUtil.registerBlock("elite_coffin", ELITE_COFFIN);
        BSMUtil.register(Registry.BLOCK, "phylactery", PHYLACTERY);
        BSMUtil.register(Registry.ITEM, "phylactery", new BlockItem(PHYLACTERY, new FabricItemSettings().group(Besmirchment.BESMIRCHMENT)){
            @Override
            public void onCraft(ItemStack stack, World world, PlayerEntity player) {
                super.onCraft(stack, world, player);
                if (!world.isClient && !player.giveItemStack(new ItemStack(LICH_GEM))){
                    player.dropItem(new ItemStack(LICH_GEM), false);
                }
            }
        });
        BSMUtil.register(Registry.ITEM,"final_broom", FINAL_BROOM);
        BSMUtil.register(Registry.ITEM,"witchy_dye", WITCHY_DYE);
        BSMUtil.register(Registry.ITEM,"scroll_of_torment", SCROLL_OF_TORMENT);
        BSMUtil.register(Registry.ITEM,"vampire_sunscreen", VAMPIRE_SUNSCREEN);
        BSMUtil.register(Registry.ITEM,"demonic_deed", DEMONIC_DEED);
        BSMUtil.register(Registry.ITEM,"lich_gem", LICH_GEM);
        BSMUtil.register(Registry.ITEM,"werepyre_spawn_egg", WEREPYRE_SPAWN_EGG);
        BSMUtil.register(Registry.ITEM,"beelzebub_spawn_egg", BEELZEBUB_SPAWN_EGG);
    }
}
