package com.winter.omt.data;


public class BeItem extends GameItem {
public CoItem[] reinforceslot;
public int skillFrame;
public int pieceId;
public boolean removed = false;
public boolean isInLocker = false;

	public BeItem(int id) {
		super(id);
		reinforceslot = new CoItem[5];
	}
	public BeItem(int id, int pieceId) {
		super(id);
		this.pieceId = pieceId;
		reinforceslot = new CoItem[5];

	}
	
	public BeItem(int id, long price) {
		super(id, price);
		reinforceslot = new CoItem[5];

	}
	
	public void setSlot1(CoItem slot) {
		this.reinforceslot[0] = slot;
	}
	public void setSlot2(CoItem slot) {
		this.reinforceslot[1] = slot;
	}
	public void setSlot3(CoItem slot) {
		this.reinforceslot[2] = slot;
	}
	public void setSlot4(CoItem slot) {
		this.reinforceslot[3] = slot;
	}
	public void setSlot5(CoItem slot) {
		this.reinforceslot[4] = slot;
	}
	
	
	public void setSlot(int slot, int coItemId) {
		
		if (slot < 5) {
			this.reinforceslot[slot - 1] = new CoItem(coItemId);
		}
	}
	
	
	
	
	public int getReinforceSlotID(int slot) {
		slot--;
		CoItem reinforcestone = reinforceslot[slot];
		if (reinforcestone != null) {
			return reinforcestone.id;
		} return 0;
	}
	
	public BeItem clone() {
		BeItem cloned = new BeItem(this.id);
		for(int i = 1; i < 6; i++) {
			
			cloned.setSlot(i, this.getReinforceSlotID(i));

		}
		return cloned;
		
		
	}
	
	public void setSlot(int slot) {
		this.slot = slot;
	}

	
	
	public String toString() {
		return "BeItem ID: " + this.id + " PieceID: " + this.pieceId + " Slot: " + this.slot + " RSSlot1: " + this.getReinforceSlotID(1) + " RSSlot2: " + this.getReinforceSlotID(2) + " RSSlot3: " + this.getReinforceSlotID(3) + " RSSlot4: " + this.getReinforceSlotID(4) + " RSSlot5: " + this.getReinforceSlotID(5);
	}

}
