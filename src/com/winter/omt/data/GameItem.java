package com.winter.omt.data;


public class GameItem{

	public int id;
	public long price;
	public int slot;
	public int type;
	public int count;
public GameItem(int id) {
		this.id = id;
	}


public GameItem(int id, int count) {
	this.id = id;
	if (count == 0) {
		count = 1;
	}
	this.count = count;
}

public GameItem(int id, long price) {
	this.id = id;
	this.price = price;
}

public void setType(int type) {
	this.type = type;
}



}
