package de.gakai.flighttable;

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
import net.minecraftforge.common.util.Constants;
import de.gakai.flighttable.gui.ContainerFlightTable;

public class TileEntityFlightTable extends TileEntityLockable implements IUpdatePlayerListBox, ISidedInventory
{

    /** constants ********************************************************************************/

    public static int RANGE_BASE;
    public static int POWER_PER_PLAYER;
    public static int POWER_PER_TICK;
    public static double RANGE_PER_UPGRADE;
    public static double POWER_PER_UPGRADE;
    public static final int MAX_POWER = 50000;
    public static final int MAX_TICK_POWER_RECEIVE = 800;
    public static Shape SHAPE;

    private static final Set<EntityPlayer> affectedPlayers = Collections.newSetFromMap(new WeakHashMap<EntityPlayer, Boolean>());
    private static final Map<EntityPlayer, Set<TileEntityFlightTable>> playerAffectingBlocks = new WeakHashMap<EntityPlayer, Set<TileEntityFlightTable>>();

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

                if (isActive() && SHAPE.contains(blockPos, getRadius(), playerPos))
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
                if (isActive() && SHAPE.contains(blockPos, getRadius(), playerPos) && player.capabilities.isFlying)
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
            NBTTagCompound slotTag = new NBTTagCompound();
            slotTag.setByte("slot", (byte) i);
            if (itemStack != null)
                itemStack.writeToNBT(slotTag);
            invList.appendTag(slotTag);
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
            inventory[slot] = !slotTag.hasKey("id") ? null : ItemStack.loadItemStackFromNBT(slotTag);
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
}
