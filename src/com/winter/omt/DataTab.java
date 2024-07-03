package com.winter.omt;

import com.winter.omt.data.LocaleManager;

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
		variableTab.setText(LocaleManager.get("omt_tab_data"));
		variableTab.setClosable(false);
		VBox variablesVbox = new VBox();

		Label labelHeader = new Label(LocaleManager.get("omt_tab_data"));
		labelHeader.setStyle("-fx-font-weight: bold;");
		variablesVbox.getChildren().add(labelHeader);

		CheckBox transferVariablesCheckBox = new CheckBox(LocaleManager.get("omt_tab_data_variables"));
		transferVariablesCheckBox.setSelected(true);
		this.transferVariables = true;
		transferVariablesCheckBox.setOnAction(event -> {
			if (transferVariablesCheckBox.isSelected()) {

				transferVariables = true;
			} else {
				transferVariables = false;
			}
		});

		Tooltip tooltip = new Tooltip(LocaleManager.get("omt_tab_data_variables_tooltip"));
		transferVariablesCheckBox.setTooltip(tooltip);

		variablesVbox.getChildren().add(transferVariablesCheckBox);

		choiceBox = new ComboBox<>();
		Label choiceBoxLabel = new Label(LocaleManager.get("omt_tab_data_option") + " " );
		choiceBoxLabel.setStyle("-fx-padding: 10 0 10 0;");
		Tooltip choiceTooltip = new Tooltip(LocaleManager.get("omt_tab_data_option_tooltip"));
		choiceBoxLabel.setTooltip(choiceTooltip);
		

		
		choiceBox.getItems().addAll(LocaleManager.get("omt_common_donothing"), // 0
				LocaleManager.get("omt_common_overwrite"), // 1
				LocaleManager.get("omt_tab_data_askuser") // 2
		);


		choiceBox.setPromptText(LocaleManager.get("omt_tab_data_selectaction"));

		choiceBox.getSelectionModel().select(0);

		choiceBox.setTooltip(choiceTooltip);
		
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
