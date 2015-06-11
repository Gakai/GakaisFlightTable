package de.gakai.levitator.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;
import de.gakai.levitator.TileEntityLevitator;

public class GuiHandler implements IGuiHandler
{

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
	{
		TileEntity entity = world.getTileEntity(x, y, z);
		if (entity instanceof TileEntityLevitator)
			return new ContainerLevitator(player.inventory, (TileEntityLevitator) entity);
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
	{
		TileEntity entity = world.getTileEntity(x, y, z);
		if (entity instanceof TileEntityLevitator)
			return new GuiLevitator(player.inventory, (TileEntityLevitator) entity);
		return null;
	}

}
