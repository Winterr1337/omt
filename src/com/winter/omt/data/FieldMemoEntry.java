package com.winter.omt.data;

public class FieldMemoEntry {
public int index;
int fieldId;
int x;
int y;

public FieldMemoEntry(int index, int fieldId, int x, int y) {
	this.index = index;
	this.fieldId = fieldId;
	this.x = x;
	this.y = y;
}

public int getFieldId() {
	return fieldId;
}

public int getX() {
	return x;
}

public int getY() {
	return y;
}





}
