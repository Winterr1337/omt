package com.winter.omt;

import java.net.MalformedURLException;
import java.util.HashSet;

import com.winter.omt.data.LocaleManager;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;

public class LanguageTab {

	Tab langTab;
	HashSet<Button> buttons;
	String lang;
	Button enButton;
	Button krButton;
	Button thButton;

	public LanguageTab(String lang) {
		this.lang = lang;
		langTab = new Tab();
		langTab.setText(LocaleManager.get("omt_tab_lang"));
		langTab.setClosable(false);
		buttons = new HashSet<Button>();
		HBox hbox = new HBox(10);

		enButton = createLanguageButton("file:resources/img/en.png", "English");
		buttons.add(enButton);
		krButton = createLanguageButton("file:resources/img/kr.png", "한국어 (Korean)");
		buttons.add(krButton);
		thButton = createLanguageButton("file:resources/img/th.png", "ภาษาไทย (Thai)");
		buttons.add(thButton);

		enButton.setOnAction(e -> selectLanguage("en"));
		krButton.setOnAction(e -> selectLanguage("ko"));
		thButton.setOnAction(e -> selectLanguage("th"));

		selectLanguage(lang);

		hbox.getChildren().addAll(enButton, krButton, thButton);

		hbox.setAlignment(Pos.CENTER);

		langTab.setContent(hbox);
	}

	public Tab get() {
		return langTab;
	}

	private Button createLanguageButton(String imagePath, String languageName) {

		ImageView imageView = new ImageView(new Image(imagePath));
		imageView.setFitWidth(48);
		imageView.setFitHeight(37);

		Label text = new Label(languageName);

		VBox vbox = new VBox(imageView, text);
		vbox.setSpacing(5);
		vbox.setStyle("-fx-alignment: center;");

		Button button = new Button();
		button.setGraphic(vbox);

		return button;
	}

	private void selectLanguage(String selectedLang) {

		Button selectedButton = null;

		switch (selectedLang) {

		case "en":

			selectedButton = enButton;

			break;

		case "ko":

			selectedButton = krButton;

			break;

		case "th":

			selectedButton = thButton;

			break;

		}

		selectedButton.setEffect(new DropShadow(20, Color.SNOW));

		for (Button button : buttons) {

			if (!button.equals(selectedButton)) {

				button.setEffect(null);

			}
		}

		if (selectedLang != this.lang) {

			OMT.config.setLang(selectedLang);

			OMT.clearPrimaryStage();
			if (Database.isConnected()) {
				
				Database.close();
				
			}
			try {
				OMT.initialize(OMT.primaryStage);

			} catch (MalformedURLException e) {

				e.printStackTrace();
			}

		}

	}

}