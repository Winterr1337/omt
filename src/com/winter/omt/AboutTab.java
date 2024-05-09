package com.winter.omt;

import javafx.application.HostServices;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class AboutTab {

	Tab aboutTab;

	public AboutTab(HostServices hostServices) {

		aboutTab = new Tab();
		aboutTab.setText("About");

		aboutTab.setClosable(false);
		VBox vbox2 = new VBox(10);
		vbox2.setAlignment(Pos.CENTER);
		Label label = new Label("Quartet → Octet migration tool 0.6 by Winter.");
		label.setWrapText(true);
		aboutTab.setContent(vbox2);
		Image omtLogo = new Image(OMT.iconPath);
		ImageView imageView = new ImageView(omtLogo);
		Hyperlink octetWebsite = new Hyperlink("https://octet-yg.org");

		octetWebsite.setOnAction(e -> {

			hostServices.showDocument("https://octet-yg.org");
		});
		vbox2.getChildren().addAll(imageView, label, octetWebsite);

	}

	public Tab get() {

		return aboutTab;

	}

}
