package de.gakai.flighttable.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import de.gakai.flighttable.TileEntityFlightTable;

public class GuiHandler implements IGuiHandler
{

    /** IGuiHandler ******************************************************************************/

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        TileEntity entity = world.getTileEntity(new BlockPos(x, y, z));
        if (entity instanceof TileEntityFlightTable)
            return new ContainerFlightTable(player.inventory, (TileEntityFlightTable) entity);
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        TileEntity entity = world.getTileEntity(new BlockPos(x, y, z));
        if (entity instanceof TileEntityFlightTable)
            return new GuiFlightTable(player, (TileEntityFlightTable) entity);
        return null;
    }

}
