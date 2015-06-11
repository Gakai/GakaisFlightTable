package de.gakai.levitator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import cofh.api.energy.IEnergyReceiver;

// import cpw.mods.fml.common.Optional;

// @Optional.Interface(iface = "cofh.api.energy.IEnergyReceiver", modid = "ThermalExpansion", striprefs = true)
public class TileEntityLevitator extends TileEntity implements ISidedInventory, IEnergyReceiver
{

    public static final int MAX_POWER = 50000;

    public static final int TICK_POWER_DRAIN = 10;

    private static final int TICK_POWER = TICK_POWER_DRAIN * 20;

    public static boolean verticalLimit = false;

    private static Set<EntityPlayer> affectedPlayers = Collections.newSetFromMap(new WeakHashMap<EntityPlayer, Boolean>());

    private static Map<EntityPlayer, Set<TileEntityLevitator>> playerAffectedBlocks = new WeakHashMap<EntityPlayer, Set<TileEntityLevitator>>();

    /********************************************************************************/

    public ItemStack[] inventory = new ItemStack[2];

    private int power = 0;

    private int powerPerTick = 0;

    protected boolean isPowered = false;

    /********************************************************************************/

    @Override
    public void updateEntity()
    {
        powerPerTick = TICK_POWER;
        if (!worldObj.isRemote)
        {
            Vec3 blockPos = Vec3.createVectorHelper(xCoord + 0.5, verticalLimit ? yCoord + 0.5 : 0, zCoord + 0.5);
            for (Object o : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
            {
                EntityPlayer player = (EntityPlayer) o;
                Vec3 playerPos = Vec3.createVectorHelper(player.posX, verticalLimit ? player.posY : 0, player.posZ);
                double dist = playerPos.distanceTo(blockPos);

                if (isActive() && dist < getRadius())
                {
                    if (player.capabilities.isFlying)
                        power -= getPowerConsumption(dist);
                    addPlayer(player);
                }
                else
                {
                    removePlayer(player, true);
                }
            }
            if (power < 0)
                power = 0;

            // Process refill
            boolean doUpdate = false;
            Integer fuelValue = LevitatorMod.getFuelValue(inventory[0]);
            if (fuelValue != null && power + fuelValue <= MAX_POWER)
            {
                power += fuelValue;
                if (inventory[0].getItem() != LevitatorMod.creativeFeather)
                    --inventory[0].stackSize;
                if (inventory[0].stackSize == 0)
                    inventory[0] = inventory[0].getItem().getContainerItem(inventory[0]);
                doUpdate = true;
            }
            if (doUpdate || worldObj.getWorldInfo().getWorldTotalTime() % 40 == 0)
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
        else
        {
            int playerCount = 0;
            Vec3 blockPos = Vec3.createVectorHelper(xCoord + 0.5, verticalLimit ? yCoord + 0.5 : 0, zCoord + 0.5);
            for (Object o : Minecraft.getMinecraft().theWorld.playerEntities)
            {
                EntityPlayer player = (EntityPlayer) o;
                Vec3 playerPos = Vec3.createVectorHelper(player.posX, verticalLimit ? player.posY : 0, player.posZ);
                double dist = playerPos.distanceTo(blockPos);
                if (isActive() && dist < getRadius() && player.capabilities.isFlying)
                    playerCount++;
            }
            power = Math.max(0, power - playerCount * TICK_POWER_DRAIN);
        }
    }

    public void onBreak()
    {
        for (Object o : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
            removePlayer((EntityPlayer) o, false);
    }

    private void addPlayer(EntityPlayer player)
    {
        Set<TileEntityLevitator> affectedBlocks = playerAffectedBlocks.get(player);
        if (affectedBlocks == null)
        {
            affectedBlocks = new HashSet<TileEntityLevitator>();
            playerAffectedBlocks.put(player, affectedBlocks);
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
        Set<TileEntityLevitator> affectedBlocks = playerAffectedBlocks.get(player);
        // Check if the player is or was affected by a levitator
        if (affectedBlocks != null)
        {
            // Remove this levitator from the set
            affectedBlocks.remove(this);

            // If no more levitators affect the player, start disabling flying
            if (affectedBlocks.isEmpty())
            {
                if (affectedPlayers.contains(player))
                {
                    // Player was set into fly mode by levitators
                    player.capabilities.isFlying = false;
                    if (player.onGround || !safe)
                    {
                        player.capabilities.allowFlying = false;
                        affectedPlayers.remove(player);
                        playerAffectedBlocks.remove(player);
                    }
                    player.sendPlayerAbilities();
                }
                else
                {
                    // Player was already in fly mode before so just clear old data
                    playerAffectedBlocks.remove(player);
                }
            }
        }
    }

    /********************************************************************************/

    public boolean isActive()
    {
        return power > 0 && !isPowered;
    }

    public double getRadius()
    {
        return 8 + 0.5 * (inventory[1] == null ? 0 : inventory[1].stackSize);
    }

    public int getPower()
    {
        return power;
    }

    public int getPowerConsumption(double distance)
    {
        // TODO: Make power consumption relative to the range upgrade
        // --> Greater range = more power consumption
        // This will make it easy to use the block for building projects / bases, but make
        // it harder to exploit for moving through the world
        // TODO: It might also be interesting to use more power the farther away the player is
        return TICK_POWER_DRAIN;
    }

    /********************************************************************************/

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
        data.setBoolean("powered", isPowered);
    }

    @Override
    public Packet getDescriptionPacket()
    {
        NBTTagCompound tagCompound = new NBTTagCompound();
        writeToNBT(tagCompound);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager networkManager, S35PacketUpdateTileEntity packet)
    {
        readFromNBT(packet.func_148857_g());
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
        isPowered = data.getBoolean("powered");
    }

    /*********************************************************************************************/

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
    public boolean canInsertItem(int slot, ItemStack item, int side)
    {
        return isItemValidForSlot(slot, item);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item)
    {
        return slot == 0 ? LevitatorMod.isItemFuel(item) : LevitatorMod.isItemUpgrade(item);
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
    public String getInventoryName()
    {
        return null;
    }

    @Override
    public boolean hasCustomInventoryName()
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
    public void openInventory()
    {
    }

    @Override
    public void closeInventory()
    {
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side)
    {
        return new int[] { side == 1 ? 1 : 0 };
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack item, int site)
    {
        return true;
    }

    /*********************************************************************************************/

    @Override
    public boolean canConnectEnergy(ForgeDirection from)
    {
        return true;
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
    {
        int received = Math.min(powerPerTick, Math.min(MAX_POWER - power, maxReceive));
        if (!simulate && received > 0)
        {
            power += received;
            powerPerTick -= received;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
        return received;
    }

    @Override
    public int getEnergyStored(ForgeDirection from)
    {
        return power;
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from)
    {
        return MAX_POWER;
    }

}
