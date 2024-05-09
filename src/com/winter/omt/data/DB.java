package com.winter.omt.data;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


public class DB {
	static Map<Integer, List<String>> BeItemType = new HashMap<>();
	
	
	public static void initBeItems() {
		try {
			Reader in = new FileReader("resources/db/BeItemType.txt", Charset.forName("Shift-JIS"));

			Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);

			for (CSVRecord record : records) {

				String type = record.get("type");
				String code = record.get("code");
				String name = record.get("name");
				String type2 = record.get("type2");
				String flag = record.get("flag");
				String pos = record.get("pos");
				String weapontype = record.get("weapontype");
				String sex = record.get("sex");
				String school = record.get("school");
				String attack = record.get("attack");
				String skill = record.get("skill");
				String beitemslot = record.get("beitemslot");
				List<String> elo = new LinkedList<String>();
				elo.add(code);
				elo.add(name);
				elo.add(type2);
				elo.add(flag);
				elo.add(pos);
				elo.add(weapontype);
				elo.add(sex);
				elo.add(school);
				elo.add(attack);
				elo.add(skill);
				elo.add(beitemslot);
				BeItemType.put(Integer.parseInt(type), elo);
			}



		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	public static int getBeItemPos(int beItemID) {
		return Integer.parseInt(BeItemType.get(beItemID).get(4));
	}
	
}
