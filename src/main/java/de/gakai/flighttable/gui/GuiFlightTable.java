package de.gakai.flighttable.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import de.gakai.flighttable.FlightTableMod;
import de.gakai.flighttable.blocks.TileEntityFlightTable;

public class GuiFlightTable extends GuiContainer
{

    /** constants ********************************************************************************/

    private static final int POWER_Y = 14;
    private static final int POWER_X = 15;
    private static final int POWER_U = 176;
    private static final int POWER_V = 31;
    private static final int POWER_WIDTH = 10;
    private static final int POWER_HEIGHT = 42;
    private static final int BOTTOM_POWER_Y = POWER_Y + POWER_HEIGHT;

    /** fields ***********************************************************************************/

    private final TileEntityFlightTable entity;
    private final ResourceLocation texture;

    /** constructor ******************************************************************************/

    public GuiFlightTable(EntityPlayer player, TileEntityFlightTable entity)
    {
        super(entity.createContainer(player.inventory, player));
        this.entity = entity;
        texture = new ResourceLocation(FlightTableMod.MODID, "textures/gui/flight_table.png");
    }

    /** GuiContainer *****************************************************************************/

    @Override
    protected void drawGuiContainerForegroundLayer(int param1, int param2)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(texture);

        int percent = (int) Math.min(100, Math.round(100 * entity.getPower() / (double) TileEntityFlightTable.MAX_POWER));

        int powerScaled = percent * POWER_HEIGHT / 100;
        drawTexturedModalRect(POWER_X, BOTTOM_POWER_Y - powerScaled, POWER_U, POWER_V, POWER_WIDTH, powerScaled);

        String header = StatCollector.translateToLocal("gui.flight_table");
        fontRendererObj.drawString(header, (xSize - fontRendererObj.getStringWidth(header)) / 2 + 10, 6, 4210752);

        String energy = String.format(StatCollector.translateToLocal("gui.flight_table.percent"), percent);
        fontRendererObj.drawString(energy, 30, 15, 4210752);

        String range = String.format("%.1f", entity.getRadius());
        fontRendererObj.drawString(range, 32, 65, 4210752);

        // fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2,
        // 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float param1, int param2, int param3)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(texture);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
    }

}
