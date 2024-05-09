package com.winter.omt.data;

public class LockerManager {

	public Locker[] lockerSlots;

	public LockerManager() {

		lockerSlots = new Locker[6];
		for (int i = 0; i < lockerSlots.length; i++) {
			lockerSlots[i] = new Locker(i);
		}
	}

	public void addBeItem(int lockerId, BeItem outboundItem) {

		outboundItem.isInLocker = true;
		lockerSlots[lockerId].addBeItem(outboundItem);

	}

	public void addCoItem(int lockerId, CoItem outboundItem) {
		lockerSlots[lockerId].addCoItem(outboundItem);

	}

	public void addEnItem(int lockerId, EnItem outboundItem) {
		lockerSlots[lockerId].addEnItem(outboundItem);

	}

	public void clear() {
		for (Locker locker : lockerSlots) {
			locker.coitems.clear();
			locker.enitems.clear();
			locker.beItems.clear();
		}

	}

	public BeItem takeBeItem(int lockerId, int slot) {
		Locker locker = lockerSlots[lockerId];
		BeItem result = locker.beItems.get(slot);
		if (result != null) {
			result.isInLocker = false;
			locker.beitemcount--;
			return result;
		}
		return null;

	}

	public CoItem takeCoItem(int lockerId, int itemtype, int count) {

		CoItem target = lockerSlots[lockerId].coitems.get(itemtype);

		CoItem result = target.retrieve(count);

		return result;

	}

	public EnItem takeEnItem(int lockerId, int itemtype, int count) {

		EnItem target = lockerSlots[lockerId].enitems.get(itemtype);

		EnItem result = target.retrieve(count);

		return result;

	}

	public void sortAll(int lastBeItemSlot) {
		for (Locker locker : lockerSlots) {
			locker.sortbeItems(lastBeItemSlot);
		}
	}

}
