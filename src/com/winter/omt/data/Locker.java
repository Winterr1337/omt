package com.winter.omt.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class Locker {
	public int lockerId;
	public Map<Integer, BeItem> beItems;

	public LinkedHashMap<Integer, CoItem> coitems;
	public LinkedHashMap<Integer, EnItem> enitems;
	public int beitemcount;

	public Locker(int lockerId) {
		this.lockerId = lockerId;
		beItems = new LinkedHashMap<Integer, BeItem>(25);
		coitems = new LinkedHashMap<Integer, CoItem>(25);
		enitems = new LinkedHashMap<Integer, EnItem>(25);
		beitemcount = 0;
	}

	public void addCoItem(CoItem item) {
		if (coitems.containsKey(item.id)) {
			coitems.get(item.id).count += item.count;
		} else {
			coitems.put(item.id, item);
		}

	}

	public void addEnItem(EnItem item) {
		if (enitems.containsKey(item.id)) {
			enitems.get(item.id).count += item.count;
		} else {
			enitems.put(item.id, item);
		}

	}

	public void addBeItem(BeItem item) {

		beItems.put(item.slot, item);
		beitemcount++;

	}

	public void putBeItem(BeItem item) {
		beItems.put(item.slot, item);
		beitemcount++;
	}

	public void sortbeItems(int latestSlot) {

		LinkedHashMap<Integer, BeItem> updatedItems = new LinkedHashMap<>();

		for (Map.Entry<Integer, BeItem> entry : beItems.entrySet()) {
			latestSlot++;
			BeItem item = entry.getValue();

			updatedItems.put(latestSlot, item);

		}
		this.beItems = updatedItems;

	}
}
