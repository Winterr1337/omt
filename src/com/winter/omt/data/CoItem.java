package com.winter.omt.data;

public class CoItem extends GameItem {

	
	
	public CoItem(int id) {
		super(id);
		count = 1;
	}
	
	public CoItem(int id, long price, int count) {
		super(id, price);
		this.count = count;
		
	}
	
	public CoItem(int id, int count) {
		super(id, count);

	}
	
	
	public CoItem retrieve(int count) {
	    if (count > this.count) {
	        throw new IllegalArgumentException("Requested quantity is not available.");
	    }
	    this.count -= count;
	    return new CoItem(this.id, count);
	}
}