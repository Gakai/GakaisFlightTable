package de.gakai.levitator;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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

public class TileEntityLevitator extends TileEntity implements ISidedInventory
{

    public static int POWER_PER_PLAYER_TICK = 10;

    public static boolean verticalLimit = false;

    private static Set<EntityPlayer> previousFlyState = Collections.newSetFromMap(new WeakHashMap<EntityPlayer, Boolean>());

    /********************************************************************************/

    public ItemStack[] inventory = new ItemStack[2];

    private int fuel = 0;

    protected boolean isPowered = false;

    /********************************************************************************/

    @Override
    public void updateEntity()
    {
        if (!worldObj.isRemote)
        {
            boolean doUpdate = false;
            int playerCount = 0;
            Vec3 blockPos = Vec3.createVectorHelper(xCoord + 0.5, verticalLimit ? yCoord + 0.5 : 0, zCoord + 0.5);
            for (Object o : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
            {
                EntityPlayerMP player = (EntityPlayerMP) o;
                Vec3 playerPos = Vec3.createVectorHelper(player.posX, verticalLimit ? player.posY : 0, player.posZ);
                double dist = playerPos.distanceTo(blockPos);
                if (isActive() && dist < getRadius())
                {
                    setFlying(player, true);
                    if (player.capabilities.isFlying)
                        playerCount++;
                }
                else
                    setFlying(player, false);
            }
            fuel = Math.max(0, fuel - playerCount * POWER_PER_PLAYER_TICK);
            if (inventory[0] != null && LevitatorMod.isItemFuel(inventory[0]))
            {
                Integer fuelValue = LevitatorMod.fuels.get(inventory[0].getItem());
                if (fuelValue != null && fuel + fuelValue <= BlockLevitator.MAX_POWER)
                {
                    fuel += fuelValue;
                    --inventory[0].stackSize;
                    doUpdate = true;
                    if (inventory[0].stackSize == 0)
                        inventory[0] = inventory[0].getItem().getContainerItem(inventory[0]);
                }
            }
            if (doUpdate || worldObj.getWorldInfo().getWorldTotalTime() % 40 == 0)
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
        else
        {
            int playerCount = 0;
            Vec3 blockPos = Vec3.createVectorHelper(xCoord + 0.5, verticalLimit ? yCoord + 0.5 : 0, zCoord + 0.5);
            for (Object o : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
            {
                EntityPlayerMP player = (EntityPlayerMP) o;
                Vec3 playerPos = Vec3.createVectorHelper(player.posX, verticalLimit ? player.posY : 0, player.posZ);
                double dist = playerPos.distanceTo(blockPos);
                if (isActive() && dist < getRadius() && player.capabilities.isFlying)
                    playerCount++;
            }
            fuel = Math.max(0, fuel - playerCount * POWER_PER_PLAYER_TICK);
        }
    }

    public double getRadius()
    {
        return 8 + 0.5 * (inventory[1] == null ? 0 : inventory[1].stackSize);
    }

    private void setFlying(EntityPlayerMP player, boolean value)
    {
        if (value)
        {
            if (!player.capabilities.allowFlying)
            {
                previousFlyState.add(player);
                player.capabilities.allowFlying = true;
                player.sendPlayerAbilities();
            }

        }
        else if (previousFlyState.contains(player))
        {
            player.capabilities.isFlying = false;
            if (player.onGround)
            {
                player.capabilities.allowFlying = false;
                previousFlyState.remove(player);
            }
            player.sendPlayerAbilities();
        }
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
        data.setInteger("fuel", fuel);
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

        fuel = data.getInteger("fuel");
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

    public int getFuel()
    {
        return fuel;
    }

    public boolean isActive()
    {
        return fuel > 0 && !isPowered;
    }

    public void onBreak()
    {
        Vec3 blockPos = Vec3.createVectorHelper(xCoord + 0.5, verticalLimit ? yCoord + 0.5 : 0, zCoord + 0.5);
        for (Object o : MinecraftServer.getServer().getConfigurationManager().playerEntityList)
        {
            EntityPlayerMP player = (EntityPlayerMP) o;
            if (!previousFlyState.contains(player))
                continue;

            Vec3 playerPos = Vec3.createVectorHelper(player.posX, verticalLimit ? player.posY : 0, player.posZ);
            double dist = playerPos.distanceTo(blockPos);
            if (isActive() && dist < getRadius())
            {
                player.capabilities.isFlying = false;
                player.capabilities.allowFlying = false;
                player.sendPlayerAbilities();
            }
        }
    }

}
