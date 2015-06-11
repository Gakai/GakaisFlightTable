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
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import de.gakai.levitator.gui.GuiHandler;

@Mod(modid = LevitatorMod.MODID, version = LevitatorMod.VERSION)
public class LevitatorMod
{
	
	// TO-DO-List:
	//
	
	public static final String MODID = "GakaisLevitator";
	public static final String VERSION = "0.1";
	public static final String ASSETS = "levitator";
	
	public static final boolean debug = true;
	
	@Instance(MODID)
	public static LevitatorMod instance;
	
	public static final Block levitator = new BlockLevitator().setHardness(5f).setResistance(2000f).setStepSound(Block.soundTypePiston)
			.setBlockName("levitator").setCreativeTab(CreativeTabs.tabTransport).setBlockTextureName(ASSETS + ":levitator_side");
	
	public static final Map<Item, Integer> fuels = new HashMap<Item, Integer>();
	public static final Item upgradeItem = Item.getItemFromBlock(Blocks.glowstone);
	
	@SidedProxy(clientSide = "de.gakai.levitator.ClientProxy", serverSide = "de.gakai.levitator.CommonProxy")
	public static CommonProxy proxy;
	
	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		fuels.put(Items.feather, 12000);
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
		
		FMLCommonHandler.instance().bus().register(this);
		
		GameRegistry.registerBlock(levitator, levitator.getUnlocalizedName());
		GameRegistry.registerTileEntity(TileEntityLevitator.class, LevitatorMod.MODID);
		
		GameRegistry.addShapedRecipe(new ItemStack(levitator), //
		        "dod", //
		        "ogo", //
		        "dod", //
		        'd', Items.diamond, 'o', Blocks.obsidian, 'g', Blocks.redstone_lamp);
	}
	
	public static void debugLog(String s)
	{
		if (debug)
			System.out.println(s);
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
