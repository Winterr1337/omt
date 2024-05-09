package com.winter.omt;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

public class DataTab {

	Tab variableTab;
	boolean transferVariables;
	ComboBox<String> choiceBox;

	public DataTab() {

		variableTab = new Tab();
		variableTab.setText("Data");
		variableTab.setClosable(false);
		VBox variablesVbox = new VBox();

		Label labelHeader = new Label("Data");
		labelHeader.setStyle("-fx-font-weight: bold;");
		variablesVbox.getChildren().add(labelHeader);

		CheckBox transferVariablesCheckBox = new CheckBox("Transfer Variables");
		transferVariablesCheckBox.setSelected(true);
		this.transferVariables = true;
		transferVariablesCheckBox.setOnAction(event -> {
			if (transferVariablesCheckBox.isSelected()) {

				transferVariables = true;
			} else {
				transferVariables = false;
			}
		});

		Tooltip tooltip = new Tooltip("Check this box to transfer variables.");
		transferVariablesCheckBox.setTooltip(tooltip);

		variablesVbox.getChildren().add(transferVariablesCheckBox);

		choiceBox = new ComboBox<>();
		Label choiceBoxLabel = new Label("Action on existing account:");
		choiceBoxLabel.setStyle("-fx-padding: 10 0 10 0;");
		// Add items to the ComboBox
		choiceBox.getItems().addAll("Do nothing", // 0
				"Overwrite Octet data", // 1
				"Ask user on each case" // 2
		);

		// Set a prompt text (optional)
		choiceBox.setPromptText("Select action");

		choiceBox.getSelectionModel().select(0);

		variablesVbox.getChildren().addAll(choiceBoxLabel, choiceBox);

		variableTab.setContent(variablesVbox);

	}

	public Tab get() {

		return variableTab;

	}

	public boolean transferingVariables() {
		return transferVariables;
	}

	public String getDefaultLanguage() {
		return "en";
	}

	public int getExistingAccountOption() {

		return choiceBox.getSelectionModel().getSelectedIndex();

	}

}
