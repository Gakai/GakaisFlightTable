package de.gakai.flighttable;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.Constants;

import org.apache.commons.lang3.StringUtils;

import de.gakai.flighttable.gui.ContainerFlightTable;

public class TileEntityFlightTable extends TileEntityLockable implements IUpdatePlayerListBox, ISidedInventory
{

    /** constants ********************************************************************************/

    public static final int MAX_POWER = 50000;
    public static final int RANGE_BASE;
    public static final int POWER_PER_PLAYER;
    public static final int POWER_PER_TICK;
    public static final double RANGE_PER_UPGRADE;
    public static final double POWER_PER_UPGRADE;
    public static final Shape shape;
    public static final int MAX_TICK_POWER_RECEIVE = 800;

    private static final String SHAPES_HELP = "Available shapes: " + StringUtils.join(Shape.values(), ", ");

    private static final Set<EntityPlayer> affectedPlayers = Collections.newSetFromMap(new WeakHashMap<EntityPlayer, Boolean>());
    private static final Map<EntityPlayer, Set<TileEntityFlightTable>> playerAffectingBlocks = new WeakHashMap<EntityPlayer, Set<TileEntityFlightTable>>();

    /** static initializer ***********************************************************************/

    static
    {
        Configuration config = new Configuration(new File("config/FlightTable.cfg"), true);
        POWER_PER_PLAYER = config.get(FlightTableMod.CONF_CAT, "PowerPerPlayer", 10).getInt();
        POWER_PER_TICK = config.get(FlightTableMod.CONF_CAT, "PowerPerTick", 1).getInt();
        POWER_PER_UPGRADE = config.get(FlightTableMod.CONF_CAT, "PowerPerUpgrade", 0.0625).getDouble();
        RANGE_BASE = config.get(FlightTableMod.CONF_CAT, "BaseRange", 8).getInt();
        RANGE_PER_UPGRADE = config.get(FlightTableMod.CONF_CAT, "RangePerUpgrade", 0.5).getDouble();
        shape = Shape.valueOf(config.get(FlightTableMod.CONF_CAT, "shape", Shape.SPHERE.toString(), SHAPES_HELP).getString().toUpperCase());
        config.save();
    }

    /** fields ***********************************************************************************/

    private ItemStack[] inventory = new ItemStack[2];

    private int power = 0;
    private int powerReceivablePerTick = 0;

    private boolean powered = false;

    /** getter / setter **************************************************************************/

    public int getPower()
    {
        return power;
    }

    /**
     * @param amount
     *            amount to be added
     * @param negate
     *            negates amount to subtract it
     * @return true iff amount could be completely added or subtractet without hitting the
     *         borders&nbsp;(0,&nbsp;MAX_POWER).
     */
    public boolean incPower(int amount, boolean negate)
    {
        if (!negate)
            power += amount;
        else
            power -= amount;

        if (power > MAX_POWER)
        {
            power = MAX_POWER;
            return false;
        }
        if (power < 0)
        {
            power = 0;
            return false;
        }
        return true;
    }

    public boolean isPowered()
    {
        return powered;
    }

    public void setPowered(boolean powered)
    {
        this.powered = powered;
    }

    /** TileEntity *******************************************************************************/

    @Override
    public void update()
    {
        powerReceivablePerTick = MAX_TICK_POWER_RECEIVE;
        if (!worldObj.isRemote)
        {
            Vec3 blockPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            for (Object o : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
            {
                EntityPlayer player = (EntityPlayer) o;
                Vec3 playerPos = new Vec3(player.posX, player.posY, player.posZ);

                if (isActive() && shape.contains(blockPos, getRadius(), playerPos))
                {
                    if (player.capabilities.isFlying)
                        incPower(getPowerConsumptionPerPlayer(), true);
                    addPlayer(player);
                }
                else
                    removePlayer(player, true);
            }
            if (isActive())
                incPower(getPowerConsumptionPerTick(), true);

            // Process refill
            boolean doUpdate = false;
            if (inventory[0] != null)
            {
                if (inventory[0].getItem() != FlightTableMod.creativeFeather)
                {
                    Integer fuelValue = FlightTableMod.getFuelValue(inventory[0]);
                    if (fuelValue != null && power + fuelValue <= MAX_POWER)
                    {
                        incPower(fuelValue, false);
                        --inventory[0].stackSize;
                        if (inventory[0].stackSize == 0)
                            inventory[0] = inventory[0].getItem().getContainerItem(inventory[0]);
                        doUpdate = true;
                    }
                }
                else
                    power = MAX_POWER;
            }
            if (doUpdate || worldObj.getWorldInfo().getWorldTotalTime() % 40 == 0)
                worldObj.markBlockForUpdate(pos);
        }
        else
        {
            Vec3 blockPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            for (Object o : Minecraft.getMinecraft().theWorld.playerEntities)
            {
                EntityPlayer player = (EntityPlayer) o;
                Vec3 playerPos = new Vec3(player.posX, player.posY, player.posZ);
                if (isActive() && shape.contains(blockPos, getRadius(), playerPos) && player.capabilities.isFlying)
                    incPower(getPowerConsumptionPerPlayer(), true);
            }
            if (isActive())
                incPower(getPowerConsumptionPerTick(), true);
            if (power < 0)
                power = 0;
        }
    }

    public void onBreak()
    {
        List<EntityPlayer> players;
        if (worldObj.isRemote)
            players = Minecraft.getMinecraft().theWorld.playerEntities;
        else
            players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;

        for (EntityPlayer player : players)
            removePlayer(player, false);
    }

    private void addPlayer(EntityPlayer player)
    {
        Set<TileEntityFlightTable> affectedBlocks = playerAffectingBlocks.get(player);
        if (affectedBlocks == null)
        {
            affectedBlocks = new HashSet<TileEntityFlightTable>();
            playerAffectingBlocks.put(player, affectedBlocks);
        }
        // Only affect players that were not in fly-mode to begin with
        if (!player.capabilities.allowFlying)
        {
            affectedPlayers.add(player);
            player.capabilities.allowFlying = true;
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
        }
        // Remember our block affects this player
        affectedBlocks.add(this);
    }

    private void removePlayer(EntityPlayer player, boolean safe)
    {
        Set<TileEntityFlightTable> affectingBlocks = playerAffectingBlocks.get(player);
        // Check if the player is or was affected by a flightTable
        if (affectingBlocks != null)
        {
            // Remove this flightTable from the set
            affectingBlocks.remove(this);

            // If no more flight tables affect the player, start disabling flying
            if (affectingBlocks.isEmpty())
                if (affectedPlayers.contains(player))
                {
                    // Player was set into fly mode by flight tables
                    if (!player.capabilities.isCreativeMode)
                        player.capabilities.isFlying = false;
                    if (player.onGround || !safe)
                    {
                        if (!player.capabilities.isCreativeMode)
                            player.capabilities.allowFlying = false;
                        affectedPlayers.remove(player);
                        playerAffectingBlocks.remove(player);
                    }
                    player.sendPlayerAbilities();
                }
                else
                    // Player was already in fly mode before so just clear old data
                    playerAffectingBlocks.remove(player);
        }
    }

    public boolean isActive()
    {
        return power > 0 && !powered;
    }

    public double getRadius()
    {
        return RANGE_BASE + RANGE_PER_UPGRADE * getUpgradeLevel();
    }

    public int getUpgradeLevel()
    {
        return inventory[1] == null ? 0 : inventory[1].stackSize;
    }

    public int getPowerConsumptionPerPlayer()
    {
        return POWER_PER_PLAYER;
    }

    public int getPowerConsumptionPerTick()
    {
        return (int) (POWER_PER_TICK + POWER_PER_UPGRADE * getUpgradeLevel());
    }

    /** TileEntity - persistence/network *********************************************************/

    @Override
    public void writeToNBT(NBTTagCompound data)
    {
        super.writeToNBT(data);

        NBTTagList invList = new NBTTagList();
        for (int i = 0; i < getSizeInventory(); i++)
        {
            ItemStack itemStack = inventory[i];
            if (itemStack != null)
            {
                NBTTagCompound slotTag = new NBTTagCompound();
                slotTag.setByte("slot", (byte) i);
                itemStack.writeToNBT(slotTag);
                invList.appendTag(slotTag);
            }
        }
        data.setTag("inv", invList);
        data.setInteger("fuel", power);
        data.setBoolean("powered", powered);
    }

    @Override
    public void readFromNBT(NBTTagCompound data)
    {
        super.readFromNBT(data);

        NBTTagList tagList = data.getTagList("inv", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.tagCount(); i++)
        {
            NBTTagCompound slotTag = tagList.getCompoundTagAt(i);
            byte slot = slotTag.getByte("slot");
            if (slot >= 0 && slot < getSizeInventory())
                inventory[slot] = ItemStack.loadItemStackFromNBT(slotTag);
        }

        power = data.getInteger("fuel");
        powered = data.getBoolean("powered");
    }

    @Override
    public Packet getDescriptionPacket()
    {
        NBTTagCompound tagCompound = new NBTTagCompound();
        writeToNBT(tagCompound);
        return new S35PacketUpdateTileEntity(pos, 1, tagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager networkManager, S35PacketUpdateTileEntity packet)
    {
        readFromNBT(packet.getNbtCompound());
    }

    /** IInventory *******************************************************************************/

    @Override
    public int getSizeInventory()
    {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int index)
    {
        return inventory[index];
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack item, EnumFacing side)
    {
        return isItemValidForSlot(slot, item);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item)
    {
        return slot == 0 ? FlightTableMod.isItemFuel(item) : FlightTableMod.isItemUpgrade(item);
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount)
    {
        if (inventory[slot] == null)
            return null;

        ItemStack stack;
        if (inventory[slot].stackSize <= amount)
        {
            stack = inventory[slot];
            inventory[slot] = null;
        }
        else
        {
            stack = inventory[slot].splitStack(amount);
            if (inventory[slot].stackSize == 0)
                inventory[slot] = null;
        }
        return stack;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot)
    {
        if (inventory[slot] == null)
            return null;

        ItemStack stack = inventory[slot];
        inventory[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack item)
    {
        inventory[slot] = item;
        if (item != null && item.stackSize > getInventoryStackLimit())
            item.stackSize = getInventoryStackLimit();
    }

    @Override
    public String getName()
    {
        return "tile.flighttable.name";
    }

    @Override
    public boolean hasCustomName()
    {
        return false;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player)
    {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player)
    {
    }

    @Override
    public void closeInventory(EntityPlayer player)
    {
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    {
        return new int[] { side == EnumFacing.UP ? 1 : 0 };
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack item, EnumFacing side)
    {
        return true;
    }

    @Override
    public int getField(int id)
    {
        return 0;
    }

    @Override
    public void setField(int id, int value)
    {
    }

    @Override
    public int getFieldCount()
    {
        return 0;
    }

    @Override
    public void clear()
    {
        inventory = new ItemStack[2];
        power = 0;
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer player)
    {
        return new ContainerFlightTable(playerInventory, this);
    }

    @Override
    public String getGuiID()
    {
        return "flighttable:flighttable";
    }

    /** IEnergyConnection (RF) *******************************************************************/
    //
    // @Override
    // public boolean canConnectEnergy(ForgeDirection from)
    // {
    // return from != ForgeDirection.UP;
    // }
    //
    // @Override
    // public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
    // {
    // int received = Math.min(powerReceivablePerTick, Math.min(MAX_POWER - power, maxReceive));
    // if (!simulate && received > 0)
    // {
    // power += received;
    // powerReceivablePerTick -= received;
    // worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    // }
    // return received;
    // }
    //
    // @Override
    // public int getEnergyStored(ForgeDirection from)
    // {
    // return power;
    // }
    //
    // @Override
    // public int getMaxEnergyStored(ForgeDirection from)
    // {
    // return MAX_POWER;
    // }

}
