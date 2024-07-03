package com.winter.omt;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class Config {

	private String lang;
	File configFile = new File("omt.cfg");

	public Config() {

		if (!configFile.exists()) {
			lang = "en";
			try {
				configFile.createNewFile();
			} catch (IOException e) {

				e.printStackTrace();
				System.exit(1);
			}
			flushConfig();

		}

		Properties props = new Properties();

		try (FileReader reader = new FileReader(configFile)) {
			props.load(reader);

			this.lang = props.getProperty("lang");

		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	public String getLang() {

		return lang;

	}

	public void setLang(String lang) {

		this.lang = lang;
		flushConfig();
	}

	public void flushConfig() {

		try (FileWriter writer = new FileWriter(configFile)) {

			writer.write("lang=" + lang);

		} catch (IOException e) {

			e.printStackTrace();

		}

	}

}
