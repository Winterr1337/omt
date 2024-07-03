package com.winter.omt.data;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;

public class LocaleManager {
	public static ResourceBundle bundle;
	public static String currentLocale;
	public LocaleManager(String locale) {
		Locale.setDefault(Locale.forLanguageTag(locale));
		File file = new File("resources/lang/");

		try {
			URL[] urls = { file.toURI().toURL() };

			ClassLoader loader = new URLClassLoader(urls);
			bundle = ResourceBundle.getBundle("omt", Locale.getDefault(), loader);

			currentLocale = locale;

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

	}

	public static String get(String key) {
		
		if (key.equals("omt_translationauthor") && currentLocale.toLowerCase().equals("en")) {
	
			return "";
			
		}
		
		return bundle.getString(key);
	}

}
