package com.winter.omt.data;

public class ItemHotkey {
public int itemId;
public int itemType;



public ItemHotkey(int itemId, int itemType) {
	this.itemId = itemId;
	this.itemType = itemType;
}


public String toString() {
	
	return "itemId: " + itemId + " itemType: " + itemType;
	
}

public boolean equals(ItemHotkey hotkey) {
	
	return this.itemId == hotkey.itemId && this.itemType == hotkey.itemType;
	
	
}


}
