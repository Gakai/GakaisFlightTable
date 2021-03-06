package de.gakai.flighttable;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockFlightTable extends BlockContainer
{

    /** new Material *****************************************************************************/

    public static final Material material = new Material(MapColor.obsidianColor) {
        {
            setImmovableMobility();
        }
    };

    /** textures *********************************************************************************/

    private IIcon iconTop;
    private IIcon iconBottom;

    /** constructor ******************************************************************************/

    public BlockFlightTable()
    {
        super(material);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 1.0F);
        setHardness(5f);
        setResistance(2000f);
        setStepSound(Block.soundTypePiston);
        setBlockName("flighttable");
        setCreativeTab(CreativeTabs.tabTransport);
        setBlockTextureName(FlightTableMod.ASSETS + ":flight_table_side");
        setLightOpacity(0);
        setLightLevel(0.5f);
    }

    /** BlockContainer ***************************************************************************/

    @Override
    public TileEntity createNewTileEntity(World world, int metadata)
    {
        return new TileEntityFlightTable();
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata)
    {
        TileEntityFlightTable entity = (TileEntityFlightTable) world.getTileEntity(x, y, z);
        entity.onBreak();

        if (entity != null)
        {
            Random rand = new Random();
            for (int slotIndex = 0; slotIndex < entity.getSizeInventory(); ++slotIndex)
            {
                ItemStack itemstack = entity.getStackInSlot(slotIndex);
                if (itemstack != null)
                {
                    float randX = rand.nextFloat() * 0.8F + 0.1F;
                    float randY = rand.nextFloat() * 0.8F + 0.1F;
                    float randZ = rand.nextFloat() * 0.8F + 0.1F;

                    while (itemstack.stackSize > 0)
                    {
                        int amount = rand.nextInt(21) + 10;
                        if (amount > itemstack.stackSize)
                            amount = itemstack.stackSize;
                        itemstack.stackSize -= amount;
                        EntityItem entityitem = new EntityItem(world, x + randX, y + randY, z + randZ, new ItemStack(itemstack.getItem(), amount,
                                itemstack.getItemDamage()));

                        if (itemstack.hasTagCompound())
                            entityitem.getEntityItem().setTagCompound((NBTTagCompound) itemstack.getTagCompound().copy());

                        float velocity = 0.05F;
                        entityitem.motionX = (float) rand.nextGaussian() * velocity;
                        entityitem.motionY = (float) rand.nextGaussian() * velocity + 0.2F;
                        entityitem.motionZ = (float) rand.nextGaussian() * velocity;
                        world.spawnEntityInWorld(entityitem);
                    }
                }
            }
            world.func_147453_f(x, y, z, block);
        }
        super.breakBlock(world, x, y, z, block, metadata);
    }

    /** Block ************************************************************************************/

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister)
    {
        blockIcon = iconRegister.registerIcon(FlightTableMod.ASSETS + ":flight_table_side");
        iconTop = iconRegister.registerIcon(FlightTableMod.ASSETS + ":flight_table_top");
        iconBottom = iconRegister.registerIcon(FlightTableMod.ASSETS + ":flight_table_bottom");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata)
    {
        return side == ForgeDirection.DOWN.ordinal() ? iconBottom : side != ForgeDirection.UP.ordinal() ? blockIcon : iconTop;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
    {
        TileEntityFlightTable entity = (TileEntityFlightTable) world.getTileEntity(x, y, z);
        entity.setPowered(world.isBlockIndirectlyGettingPowered(x, y, z));
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int metadata, float par1, float par2, float Par3)
    {
        TileEntity entity = world.getTileEntity(x, y, z);
        if (entity == null || player.isSneaking())
            return false;

        player.openGui(FlightTableMod.instance, 0, world, x, y, z);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random rand)
    {
        TileEntityFlightTable entity = (TileEntityFlightTable) world.getTileEntity(x, y, z);
        if (entity == null || !entity.isActive())
            return;

        double px = x + 0.5D + (rand.nextFloat() - 0.5D) * 0.2D;
        double py = y + 1F;
        double pz = z + 0.5D + (rand.nextFloat() - 0.5D) * 0.2D;
        world.spawnParticle("reddust", px, py, pz, 75f / 255, 237f / 255, 209f / 255);
    }

}
