package de.gakai.flighttable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ClientProxy extends CommonProxy
{
    @Override
    public void init()
    {
        String[] items = { "flight_table", "redstone_feather", "creative_feather" };
        for (String itemName : items)
        {
            Item item = GameRegistry.findItem(FlightTableMod.MODID, itemName);
            Minecraft.getMinecraft().getRenderItem().getItemModelMesher()
                    .register(item, 0, new ModelResourceLocation(FlightTableMod.MODID + ":" + itemName, "inventory"));
        }
    }
}
