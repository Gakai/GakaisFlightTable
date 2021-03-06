package de.gakai.flighttable.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import de.gakai.flighttable.FlightTableMod;
import de.gakai.flighttable.TileEntityFlightTable;

public class ContainerFlightTable extends Container
{
    private TileEntityFlightTable flightTableEntity;

    /** constructor ******************************************************************************/

    public ContainerFlightTable(InventoryPlayer playerInventory, TileEntityFlightTable entity)
    {
        flightTableEntity = entity;
        // fuel
        addSlotToContainer(new Slot(entity, 0, 89, 38));
        // upgrades
        addSlotToContainer(new Slot(entity, 1, 12, 60));

        addInventoryToContainer(playerInventory);
    }

    private void addInventoryToContainer(InventoryPlayer playerInventory)
    {
        for (int i = 0; i < 3; ++i)
            for (int j = 0; j < 9; ++j)
                addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));

        for (int i = 0; i < 9; ++i)
            addSlotToContainer(new Slot(playerInventory, i, 8 + i * 18, 142));
    }

    /** Container ********************************************************************************/

    @Override
    public boolean canInteractWith(EntityPlayer player)
    {
        return flightTableEntity.isUseableByPlayer(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slot)
    {
        Slot slotObject = (Slot) inventorySlots.get(slot);

        // null checks and checks if the item can be stacked (maxStackSize > 1)
        if (slotObject == null || !slotObject.getHasStack())
            return null;

        // merges the item into player inventory since its in the tileEntity
        ItemStack stackInSlot = slotObject.getStack();
        if (slot < flightTableEntity.getSizeInventory())
        {
            if (!this.mergeItemStack(stackInSlot, flightTableEntity.getSizeInventory(),
                    player.inventory.mainInventory.length + flightTableEntity.getSizeInventory(), true))
                return null;
        }
        // places it into the tileEntity is possible since its in the player inventory
        else
        {
            if (!this.mergeItemStack(stackInSlot, 0, flightTableEntity.getSizeInventory(), false))
                return null;
        }

        if (stackInSlot.stackSize == 0)
            slotObject.putStack(null);
        else
            slotObject.onSlotChanged();

        ItemStack stack = stackInSlot.copy();
        if (stackInSlot.stackSize == stack.stackSize)
            return null;
        slotObject.onPickupFromSlot(player, stackInSlot);

        return stack;
    }

    @Override
    protected boolean mergeItemStack(ItemStack stack, int fromSlot, int toSlot, boolean toPlayer)
    {
        if (!toPlayer)
        {
            if (FlightTableMod.isItemFuel(stack))
                return mergeItemStack(stack, 0);
            else if (FlightTableMod.isItemUpgrade(stack))
                return mergeItemStack(stack, 1);
            else
                return false;
        }
        else
            return super.mergeItemStack(stack, fromSlot, toSlot, toPlayer);
    }

    private boolean mergeItemStack(ItemStack stack, int slotIndex)
    {
        Slot slot = (Slot) inventorySlots.get(slotIndex);
        ItemStack stackInSlot = slot.getStack();
        if (stackInSlot == null)
        {
            slot.putStack(stack.copy());
            stack.stackSize = 0;
            return true;
        }
        else if (stackInSlot.getItem() != stack.getItem())
            return false;
        else if (stackInSlot.stackSize + stack.stackSize <= stackInSlot.getMaxStackSize())
        {
            stackInSlot.stackSize += stack.stackSize;
            stack.stackSize = 0;
            return true;
        }
        else
        {
            stack.stackSize -= stackInSlot.getMaxStackSize() - stackInSlot.stackSize;
            stackInSlot.stackSize = stackInSlot.getMaxStackSize();
            return false;
        }
    }
}
