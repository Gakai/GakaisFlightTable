package de.gakai.levitator;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
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

    public static final Block levitator = new BlockLevitator();

    public static final Item redstoneFeather = new Item() //
            .setFull3D() //
            .setUnlocalizedName("redstoneFeather") //
            .setCreativeTab(CreativeTabs.tabTransport) //
            .setTextureName(ASSETS + ":redstoneFeather");

    public static final Item creativeFeather = new Item() //
            .setFull3D() //
            .setUnlocalizedName("creativeFeather") //
            .setCreativeTab(CreativeTabs.tabTransport) //
            .setTextureName(ASSETS + ":creativeFeather") //
            .setMaxStackSize(1);

    public static final Map<Item, Integer> fuels = new HashMap<Item, Integer>();

    public static final Item upgradeItem = Item.getItemFromBlock(Blocks.glowstone);

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        FMLCommonHandler.instance().bus().register(this);

        GameRegistry.registerItem(redstoneFeather, "redstoneFeather");
        GameRegistry.registerItem(creativeFeather, "creativeFeather");
        GameRegistry.registerBlock(levitator, "levitator");
        GameRegistry.registerTileEntity(TileEntityLevitator.class, LevitatorMod.MODID);

        GameRegistry.addShapedRecipe(new ItemStack(levitator), //
                "dod", //
                "ogo", //
                "dod", //
                'd', Items.diamond, //
                'o', Blocks.obsidian, //
                'g', Blocks.redstone_lamp);

        GameRegistry.addShapedRecipe(new ItemStack(redstoneFeather), //
                " r ", //
                "rfr", //
                " r ", //
                'f', Items.feather, //
                'r', Items.redstone);

        fuels.put(Items.feather, 12000);
        fuels.put(redstoneFeather, 12000 * 4);
        fuels.put(creativeFeather, 1200);
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
