package com.winter.omt;

import java.io.File;

import com.winter.omt.data.LocaleManager;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PermissionsTab {

	Tab permissionsTab;
	TextField defaultPlayerPermissionGroup;
	TextField defaultCMPermissionGroup;
	TextField defaultGMPermissionGroup;
	boolean isPermissionFileSet;
	 Button playerPermButton;
	 Button cmPermButton;
	 Button gmPermButton;
	
	
	public PermissionsTab(Stage primaryStage) {
		
		
	      permissionsTab = new Tab();
	        permissionsTab.setText(LocaleManager.get("omt_tab_permissions"));
	        permissionsTab.setClosable(false);
	
			GridPane gridPane = new GridPane();
			gridPane.setHgap(10);
			gridPane.setVgap(10);

	        Tooltip selectDBPermGroupTooltip = new Tooltip(LocaleManager.get("omt_tab_permissions_button_tooltip"));
			Label labelHeader = new Label(LocaleManager.get("omt_tab_permissions"));
			labelHeader.setStyle("-fx-font-weight: bold;");
			gridPane.add(labelHeader, 0, 0);

			
			
			Label permissionYmlPath = new Label(LocaleManager.get("omt_tab_permissions_ymlpath") + " ");
			gridPane.add(permissionYmlPath, 0, 1);

			TextField permissionYmlField = new TextField();
	
			
			
			gridPane.add(permissionYmlField, 0, 2);
			

			Button browseButton = new Button(LocaleManager.get("omt_common_browse"));
			ExtensionFilter dbFilter = new ExtensionFilter(LocaleManager.get("omt_filechooser_permissiongroupsyml"), "permission-groups.yml");

			browseButton.setOnAction(e -> {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle(LocaleManager.get("omt_filechooser_permissiongroups_title"));
				fileChooser.getExtensionFilters().add(dbFilter);
				File selectedFile = fileChooser.showOpenDialog(primaryStage);
				if (selectedFile != null) {
					permissionYmlField.setText(selectedFile.getAbsolutePath());
					
					OMT.parsePermissionGroupsYml(selectedFile);
					notifyPermissionFileSet();
					
				}
			});
			gridPane.add(browseButton, 1, 2);
	        
		      
			
			Label defaultPlayerPermissionGroupText = new Label(LocaleManager.get("omt_tab_permissions_default_player") + " ");
			gridPane.add(defaultPlayerPermissionGroupText, 0, 4);

			defaultPlayerPermissionGroup = new TextField();
			
			defaultPlayerPermissionGroup.setText("player");
	        playerPermButton = new Button("📁");
	        
	        
	        playerPermButton.setOnAction(e -> openListWindow(defaultPlayerPermissionGroup));
	        
	        playerPermButton.setTooltip(selectDBPermGroupTooltip);
			

			
			
			gridPane.add(defaultPlayerPermissionGroup, 0, 5);
	        gridPane.add(playerPermButton, 1, 5);
	        
			Label defaultCMPermissionGroupText = new Label(LocaleManager.get("omt_tab_permissions_default_cm") + " ");
			gridPane.add(defaultCMPermissionGroupText, 0, 7);

			defaultCMPermissionGroup = new TextField();
			
			defaultCMPermissionGroup.setText("CM");

	        cmPermButton = new Button("📁");

	        cmPermButton.setOnAction(e -> openListWindow(defaultCMPermissionGroup));
	        
	        cmPermButton.setTooltip(selectDBPermGroupTooltip);
			

			
			
			gridPane.add(defaultCMPermissionGroup, 0, 8);
	        gridPane.add(cmPermButton, 1, 8);
	        
	        
	      
	        
			Label defaultGMPermissionGroupText = new Label(LocaleManager.get("omt_tab_permissions_default_gm") + " ");
			gridPane.add(defaultGMPermissionGroupText, 0, 10);

			defaultGMPermissionGroup = new TextField();
			
			defaultGMPermissionGroup.setText("GM");

			
	        gmPermButton = new Button("📁");
	        gmPermButton.setOnAction(e -> openListWindow(defaultCMPermissionGroup));
	        
	        gmPermButton.setTooltip(selectDBPermGroupTooltip);
			

			
			
			gridPane.add(defaultGMPermissionGroup, 0, 11);
	        gridPane.add(gmPermButton, 1, 11);
	        
	        
	        
	        
	        
			if (!isPermissionFileSet) {
				playerPermButton.setDisable(true);
				cmPermButton.setDisable(true);
				gmPermButton.setDisable(true);
			}
	        
	        
	        
			permissionsTab.setContent(gridPane);

	}
	
	public Tab get() {
		
	return permissionsTab;
		
	}
	
	
	public void notifyPermissionFileSet() {
		this.isPermissionFileSet = true;
		
		
		
		playerPermButton.setDisable(false);
		cmPermButton.setDisable(false);
		gmPermButton.setDisable(false);
		
		
		
	}
	
    @SuppressWarnings("deprecation")
	private void openListWindow(TextField textField) {
        Stage listStage = new Stage();
        listStage.initModality(Modality.APPLICATION_MODAL);
        listStage.setTitle(LocaleManager.get("omt_tab_permissions_select"));



        ListView<String> listView = new ListView<>(OMT.permGroups);
        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

            textField.setText(newValue);
            listStage.close();
        });


        VBox listLayout = new VBox(10);
        listLayout.setPadding(new Insets(10));

        
        listLayout.getChildren().add(listView);

        Scene listScene = new Scene(listLayout, 200, 200);
        File css = new File("style.css");
        try {
        if (css.exists()) {
         listScene.getStylesheets().add(css.toURL().toExternalForm());
        }
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        listStage.setScene(listScene);
        listStage.showAndWait();
    }

	public String getPlayerPermissionGroup() {
		return defaultPlayerPermissionGroup.getText();
	}
    
	public String getCMPermissionGroup() {
		return defaultCMPermissionGroup.getText();
	}
    
	public String getGMPermissionGroup() {
		return defaultGMPermissionGroup.getText();
	}
	
}
