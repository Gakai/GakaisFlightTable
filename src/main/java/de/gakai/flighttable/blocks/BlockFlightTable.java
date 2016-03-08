package de.gakai.flighttable.blocks;

import java.util.Random;

import de.gakai.flighttable.FlightTableMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockFlightTable extends BlockContainer
{

    /** new Material *****************************************************************************/

    public static final Material material = new Material(MapColor.obsidianColor) {
        {
            setImmovableMobility();
        }
    };

    /** constructor ******************************************************************************/

    public BlockFlightTable()
    {
        super(material);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 1.0F);
        setHardness(5f);
        setResistance(2000f);
        setStepSound(Block.soundTypePiston);
        setUnlocalizedName("flight_table");
        setCreativeTab(CreativeTabs.tabTransport);
        setLightOpacity(0);
        setLightLevel(0.5f);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public EnumWorldBlockLayer getBlockLayer()
    {
        return EnumWorldBlockLayer.SOLID;
    }

    @Override
    public int getRenderType()
    {
        return 3;
    }

    /** BlockContainer ***************************************************************************/

    @Override
    public TileEntity createNewTileEntity(World world, int metadata)
    {
        return new TileEntityFlightTable();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state)
    {
        TileEntityFlightTable entity = (TileEntityFlightTable) world.getTileEntity(pos);
        entity.onBreak();

        if (entity != null)
        {
            InventoryHelper.dropInventoryItems(world, pos, entity);
            world.updateComparatorOutputLevel(pos, this);
        }
        super.breakBlock(world, pos, state);
    }

    /** Block ************************************************************************************/

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block block)
    {
        TileEntityFlightTable entity = (TileEntityFlightTable) world.getTileEntity(pos);
        entity.setPowered(world.isBlockPowered(pos));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        TileEntity entity = world.getTileEntity(pos);
        if (entity == null || player.isSneaking())
            return false;

        player.openGui(FlightTableMod.instance, 0, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, BlockPos pos, IBlockState state, Random rand)
    {
        TileEntityFlightTable entity = (TileEntityFlightTable) world.getTileEntity(pos);
        if (entity == null || !entity.isActive())
            return;

        double px = pos.getX() + 0.5D + (rand.nextFloat() - 0.5D) * 0.2D;
        double py = pos.getY() + 1F;
        double pz = pos.getZ() + 0.5D + (rand.nextFloat() - 0.5D) * 0.2D;
        world.spawnParticle(EnumParticleTypes.REDSTONE, px, py, pz, 75f / 255, 237f / 255, 209f / 255);
    }

}
