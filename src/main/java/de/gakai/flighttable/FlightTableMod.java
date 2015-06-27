package de.gakai.flighttable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

import org.apache.commons.lang3.StringUtils;

import de.gakai.flighttable.gui.GuiHandler;

@Mod(modid = FlightTableMod.MODID, version = FlightTableMod.VERSION)
public class FlightTableMod
{
    // XXX: Shift-right clicking with a feather adds it into the container, if it fits

    /** constants ********************************************************************************/

    public static final String MODID = "GakaisFlightTable";
    public static final String VERSION = "1.1";
    public static final String CONF_CAT = "FlightTable";
    private static final String SHAPES_HELP = "Available shapes: " + StringUtils.join(Shape.values(), ", ");

    @Instance(MODID)
    public static FlightTableMod instance;

    public static final Block flightTable = new BlockFlightTable();

    public static final Item redstoneFeather = new Item() //
            .setFull3D() //
            .setUnlocalizedName("redstone_feather") //
            .setCreativeTab(CreativeTabs.tabTransport);

    public static final Item creativeFeather = new Item() {
        @Override
        public boolean hasEffect(ItemStack par1ItemStack)
        {
            return true;
        };
    }.setFull3D() //
            .setUnlocalizedName("creative_feather") //
            .setCreativeTab(CreativeTabs.tabTransport) //
            .setMaxStackSize(1);

    private static final Map<Item, Integer> fuels = new HashMap<Item, Integer>();

    private static final Item upgradeItem = Item.getItemFromBlock(Blocks.glowstone);

    @SidedProxy(serverSide = "de.gakai.flighttable.CommonProxy", clientSide = "de.gakai.flighttable.ClientProxy")
    private static CommonProxy proxy;

    /** init *************************************************************************************/

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        Configuration config = new Configuration(new File("config/FlightTable.cfg"), true);
        TileEntityFlightTable.POWER_PER_PLAYER = config.get(CONF_CAT, "PowerPerPlayer", 10).getInt();
        TileEntityFlightTable.POWER_PER_TICK = config.get(CONF_CAT, "PowerPerTick", 1).getInt();
        TileEntityFlightTable.POWER_PER_UPGRADE = config.get(CONF_CAT, "PowerPerUpgrade", 0.0625).getDouble();
        TileEntityFlightTable.RANGE_BASE = config.get(CONF_CAT, "BaseRange", 8).getInt();
        TileEntityFlightTable.RANGE_PER_UPGRADE = config.get(CONF_CAT, "RangePerUpgrade", 0.5).getDouble();
        TileEntityFlightTable.SHAPE = Shape.valueOf(config.get(CONF_CAT, "shape", Shape.SPHERE.toString(), SHAPES_HELP).getString().toUpperCase());
        config.save();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        FMLCommonHandler.instance().bus().register(this);

        GameRegistry.registerTileEntity(TileEntityFlightTable.class, FlightTableMod.MODID);
        GameRegistry.registerItem(redstoneFeather, "redstone_feather");
        GameRegistry.registerItem(creativeFeather, "creative_feather");
        GameRegistry.registerBlock(flightTable, "flight_table");

        GameRegistry.addShapedRecipe(new ItemStack(flightTable), //
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
        fuels.put(redstoneFeather, 48000);
        fuels.put(creativeFeather, 1200);

        proxy.init();
    }

    /** getter ***********************************************************************************/

    public static boolean isItemFuel(ItemStack item)
    {
        return FlightTableMod.fuels.containsKey(item.getItem());
    }

    public static Integer getFuelValue(ItemStack fuelStack)
    {
        return fuelStack == null ? null : fuels.get(fuelStack.getItem());
    }

    public static boolean isItemUpgrade(ItemStack item)
    {
        return item.getItem() == FlightTableMod.upgradeItem;
    }

}
