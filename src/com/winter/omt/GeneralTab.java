package com.winter.omt;

import java.io.File;

import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class GeneralTab {

	Tab generalTab;
	TextField quartetDbField;
	TextField mySqlHostField;
	TextField mySqlUserField;
	PasswordField mySqlPassField;
	TextField mySqlNameField;
	Button startButton;
	Button dbConnectButton;
	boolean isQuartetDbFieldSet;
	Label statusLabel;
	ProgressBar progressBar;
	public GeneralTab(Stage primaryStage) {

		generalTab = new Tab();
		generalTab.setText("General");
		generalTab.setClosable(false);

		GridPane gridPane = new GridPane();
		gridPane.setHgap(10);
		gridPane.setVgap(10);
		
		GridPane.setHalignment(gridPane, HPos.CENTER);
		

		Label labelHeader = new Label("General");
		labelHeader.setStyle("-fx-font-weight: bold;");
		gridPane.add(labelHeader, 0, 0);

		Label quartetDbPath = new Label("Account.db path: ");
		gridPane.add(quartetDbPath, 0, 1);

		quartetDbField = new TextField();
		quartetDbField.getStyleClass().add("db-path");
		

		
		
	    quartetDbField.textProperty().addListener((observable, oldValue, newValue) -> {

	        boolean isTextFieldEmpty = newValue.trim().isEmpty();
	        boolean hasDbExtension = newValue.endsWith(".db");

	       isQuartetDbFieldSet = !isTextFieldEmpty && hasDbExtension;

	       updateMigrateButtonState();
	       
	    });
		
		
		gridPane.add(quartetDbField, 0, 2);
		

		Button browseButton = new Button("Browse...");
		ExtensionFilter dbFilter = new ExtensionFilter("Quartet SQLite3 Database File (*.db)", "*.db");

		browseButton.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Select Database File");
			fileChooser.getExtensionFilters().add(dbFilter);
			File selectedFile = fileChooser.showOpenDialog(primaryStage);
			if (selectedFile != null) {
				quartetDbField.setText(selectedFile.getAbsolutePath());
			}
		});
		gridPane.add(browseButton, 1, 2);
		
		
		Label mySqlHostLabel = new Label("MySQL hostname: ");
		gridPane.add(mySqlHostLabel, 0, 3);

		mySqlHostField = new TextField();
		mySqlHostField.setText("127.0.0.1:3306");
		gridPane.add(mySqlHostField, 0, 4);

		Label mySqlUserLabel = new Label("MySQL username: ");
		gridPane.add(mySqlUserLabel, 0, 5);

		mySqlUserField = new TextField();
		

		
		gridPane.add(mySqlUserField, 0, 6);

		Label mySqlPass = new Label("MySQL password: ");
		gridPane.add(mySqlPass, 1, 5);

		mySqlPassField = new PasswordField();
		mySqlPassField.setPromptText("secret");
		

		
		gridPane.add(mySqlPassField, 1, 6);

		Label mySqlNameLabel = new Label("MySQL database: ");
		gridPane.add(mySqlNameLabel, 2, 5);

		mySqlNameField = new TextField();
		mySqlNameField.setPromptText("octet_db");
		

		
		Tooltip tooltip = new Tooltip(
				"Enter the name of the MySQL database to which the data will be moved.\nIf you specify the name of an existing database,\nthe tool will insert player accounts from Quartet's database without affecting any existing data.");
		mySqlNameField.setTooltip(tooltip);
		gridPane.add(mySqlNameField, 2, 6);

		VBox buttonContainer = new VBox();
		buttonContainer.setAlignment(Pos.CENTER);
		buttonContainer.setSpacing(16);
		
		dbConnectButton = new Button("Connect");



		dbConnectButton.setOnAction(e -> {
			
			if (!Database.isConnected()) {
			
			OMT.createDatabaseConnection(getMySQLHostname(), getMySQLUsername(), getMySQLPassword(), getMySQLDatabaseName());

			} else {
				startButton.setDisable(true);
				Database.close();

				dbConnectButton.setText("Connect");
				
				
			}
		});
		
		buttonContainer.getChildren().add(dbConnectButton);
		
		startButton = new Button("Migrate");
		migrateButtonDisable(true);

		startButton.setOnAction(e -> {
	    	statusLabel.setText("Working...");
		Task<Void> task = new Task<Void>() {

			
            @Override
            protected Void call() throws Exception {
            	

            	
    			OMT.startMigrateProcess();
                
    			
                return null;
            }
        };


        progressBar.progressProperty().bind(task.progressProperty());


        

        task.setOnSucceeded(event -> {
        	statusLabel.setText("Migration completed.");
            progressBar.progressProperty().unbind();
            progressBar.progressProperty().set(10);
            migrateButtonDisable(false);
        });

        task.setOnFailed(event -> {
        	statusLabel.setText("Migration failed.");
        	progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            progressBar.progressProperty().unbind();
            progressBar.progressProperty().set(10);
            migrateButtonDisable(false);

            
        });


        new Thread(task).start();
    });
		


		buttonContainer.getChildren().add(startButton);
		
		gridPane.add(buttonContainer, 1, 8);

		VBox progressVBox = new VBox(10);
		progressVBox.setAlignment(Pos.CENTER);

		progressBar = new ProgressBar(0);
		statusLabel = new Label("Not connected");

		progressVBox.getChildren().addAll(statusLabel, progressBar);

		progressBar.setMaxWidth(Double.MAX_VALUE);
		progressVBox.setFillWidth(true);

		gridPane.add(progressVBox, 0, 10);
		GridPane.setColumnSpan(progressVBox, GridPane.REMAINING); 

		generalTab.setContent(gridPane);

	}

	public Tab get() {

		return generalTab;

	}
	
	public String getQuartetDatabasePath() {
		
		return quartetDbField.getText();
		
	}
	
	public String getMySQLHostname() {
		
		String dbName = mySqlHostField.getText();
		
		if (dbName.endsWith("/")) {

			dbName = dbName.substring(0, dbName.length() - 1);

			mySqlHostField.setText(dbName);
			
		}
		
		return dbName;
		
	}
	
	public String getMySQLUsername() {
		
		return mySqlUserField.getText();
		
	}
	
	public String getMySQLPassword() {
		
		return mySqlPassField.getText();
		
	}
	
	public String getMySQLDatabaseName() {
		
		return mySqlNameField.getText();
		
	}
	
	public void migrateButtonDisable(boolean enable) {
		
		startButton.setDisable(enable);
		
	}
	
	
	public void updateMigrateButtonState() {
	    if (Database.isConnected()) {
	        dbConnectButton.setText("Disconnect");
	    }


	    migrateButtonDisable(!Database.isConnected() || !isQuartetDbFieldSet);
	}
	
	public void setStatusText(String text) {
		
		this.statusLabel.setText(text);
		
		
	}
	
	
	
	

}
