package com.winter.omt.data;

public class EnItem extends GameItem {

	
	
	public EnItem(int id) {
		super(id);
		count = 1;
	}
	
	public EnItem(int id, long price, int count) {
		super(id, price);
		this.count = count;
		
	}
	
	public EnItem(int id, int count) {
		super(id, count);
	}
	
	
	public EnItem retrieve(int count) {
	    if (count > this.count) {
	        throw new IllegalArgumentException("Requested quantity is not available.");
	    }
	    this.count -= count;
	    return new EnItem(this.id, count);
	}
}