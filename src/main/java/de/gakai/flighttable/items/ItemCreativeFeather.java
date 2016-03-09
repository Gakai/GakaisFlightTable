package de.gakai.flighttable.items;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemCreativeFeather extends Item {
		
		public ItemCreativeFeather() {
			setFull3D();
            setUnlocalizedName("creative_feather");
            setCreativeTab(CreativeTabs.tabTransport);
            setMaxStackSize(1);
		}
		
		@Override
        public boolean hasEffect(ItemStack par1ItemStack)
        {
        	return true;
        }
}
