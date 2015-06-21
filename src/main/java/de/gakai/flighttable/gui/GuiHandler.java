package de.gakai.flighttable.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;
import de.gakai.flighttable.TileEntityFlightTable;

public class GuiHandler implements IGuiHandler
{

    /** IGuiHandler ******************************************************************************/

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        TileEntity entity = world.getTileEntity(x, y, z);
        if (entity instanceof TileEntityFlightTable)
            return new ContainerFlightTable(player.inventory, (TileEntityFlightTable) entity);
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        TileEntity entity = world.getTileEntity(x, y, z);
        if (entity instanceof TileEntityFlightTable)
            return new GuiFlightTable(player.inventory, (TileEntityFlightTable) entity);
        return null;
    }

}
