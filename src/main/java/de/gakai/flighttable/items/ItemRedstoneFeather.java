package de.gakai.flighttable.items;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class ItemRedstoneFeather extends Item {
	public ItemRedstoneFeather() {
		setFull3D();
        setUnlocalizedName("redstone_feather");
        setCreativeTab(CreativeTabs.tabTransport);
	}
}
