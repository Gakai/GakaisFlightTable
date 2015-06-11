package de.gakai.levitator.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import de.gakai.levitator.BlockLevitator;
import de.gakai.levitator.LevitatorMod;
import de.gakai.levitator.TileEntityLevitator;

public class GuiLevitator extends GuiContainer
{
	
	protected static final int POWER_Y = 14;
	protected static final int POWER_X = 15;
	protected static final int POWER_U = 176;
	protected static final int POWER_V = 31;
	protected static final int POWER_WIDTH = 10;
	protected static final int POWER_HEIGHT = 42;
	protected static final int BOTTOM_POWER_Y = POWER_Y + POWER_HEIGHT;
	
	private TileEntityLevitator entity;
	private ResourceLocation texture;
	
	public GuiLevitator(InventoryPlayer playerInventory, TileEntityLevitator levitatorEntity)
	{
		super(new ContainerLevitator(playerInventory, levitatorEntity));
		entity = levitatorEntity;
		texture = new ResourceLocation(LevitatorMod.ASSETS, "textures/gui/levitator.png");
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int param1, int param2)
	{
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(texture);
		int powerScaled = entity.getFuel() * POWER_HEIGHT / 50000;
		drawTexturedModalRect(0 + POWER_X, 0 + (POWER_Y + POWER_HEIGHT) - powerScaled, POWER_U, POWER_V, POWER_WIDTH, powerScaled);
		
		String header = StatCollector.translateToLocal("gui.levitator");
		fontRendererObj.drawString(header, (xSize - fontRendererObj.getStringWidth(header)) / 2 + 10, 6, 4210752);
		
		String energy = String.format(StatCollector.translateToLocal("gui.levitator.percent"), entity.getFuel() * 100 / BlockLevitator.MAX_POWER);
		fontRendererObj.drawString(energy, 30, 15, 4210752);
		
		String range = String.format("%.1f", entity.getRadius());
		fontRendererObj.drawString(range, 32, 65, 4210752);
		
		// fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2,
		// 4210752);
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_)
	{
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(texture);
		int x = (width - xSize) / 2;
		int y = (height - ySize) / 2;
		this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
	}
	
	public void renderPowerBar(int k, int l)
	{
		if (true)
		{
			
		}
	}
}
