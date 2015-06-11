package de.gakai.levitator;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import de.gakai.levitator.gui.GuiHandler;

@Mod(modid = LevitatorMod.MODID, version = LevitatorMod.VERSION)
public class LevitatorMod
{

    public static final String MODID = "GakaisLevitator";
    public static final String VERSION = "0.1";
    public static final String ASSETS = "levitator";

    @Instance(MODID)
    public static LevitatorMod instance;

    public static final Map<Item, Integer> fuels = new HashMap<Item, Integer>();

    public static final Item upgradeItem = Item.getItemFromBlock(Blocks.glowstone);

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        fuels.put(Items.feather, 12000);
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        FMLCommonHandler.instance().bus().register(this);

        GameRegistry.registerBlock(BlockLevitator.instance, BlockLevitator.instance.getUnlocalizedName());
        GameRegistry.registerTileEntity(TileEntityLevitator.class, LevitatorMod.MODID);

        GameRegistry.addShapedRecipe(new ItemStack(BlockLevitator.instance), //
                "dod", //
                "ogo", //
                "dod", //
                'd', Items.diamond, //
                'o', Blocks.obsidian, //
                'g', Blocks.redstone_lamp);
    }

    public static boolean isItemFuel(ItemStack item)
    {
        return LevitatorMod.fuels.containsKey(item.getItem());
    }

    public static boolean isItemUpgrade(ItemStack item)
    {
        return item.getItem() == LevitatorMod.upgradeItem;
    }

}
