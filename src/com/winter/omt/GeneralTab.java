package com.winter.omt;

import java.io.File;

import com.winter.omt.data.LocaleManager;

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
	private Task<Void> task;
	int accountCount;

	public GeneralTab(Stage primaryStage) {

		Tooltip connectButtonTooltip = new Tooltip(LocaleManager.get("omt_tab_general_connect_tooltip"));
		Tooltip disconnectButtonTooltip = new Tooltip(LocaleManager.get("omt_tab_general_disconnect_tooltip"));

		generalTab = new Tab();
		generalTab.setText(LocaleManager.get("omt_tab_general"));
		generalTab.setClosable(false);

		GridPane gridPane = new GridPane();
		gridPane.setHgap(10);
		gridPane.setVgap(10);

		GridPane.setHalignment(gridPane, HPos.CENTER);

		Label labelHeader = new Label(LocaleManager.get("omt_tab_general"));
		labelHeader.setStyle("-fx-font-weight: bold;");
		gridPane.add(labelHeader, 0, 0);

		Label quartetDbPath = new Label(LocaleManager.get("omt_tab_general_accountdbpath") + " ");

		Tooltip quartetDbPathTooltip = new Tooltip(LocaleManager.get("omt_tab_general_accountdbpath_tooltip"));
		quartetDbPath.setTooltip(quartetDbPathTooltip);

		gridPane.add(quartetDbPath, 0, 1);

		quartetDbField = new TextField();
		quartetDbField.getStyleClass().add("db-path");
		quartetDbField.setTooltip(quartetDbPathTooltip);

		quartetDbField.textProperty().addListener((observable, oldValue, newValue) -> {

			boolean isTextFieldEmpty = newValue.trim().isEmpty();
			boolean hasDbExtension = newValue.endsWith(".db");

			isQuartetDbFieldSet = !isTextFieldEmpty && hasDbExtension;

			updateMigrateButtonState();

		});

		gridPane.add(quartetDbField, 0, 2);

		Button browseButton = new Button(LocaleManager.get("omt_common_browse"));

		gridPane.add(browseButton, 1, 2);

		ExtensionFilter dbFilter = new ExtensionFilter(LocaleManager.get("omt_filechooser_accountdb"), "*.db");

		browseButton.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(LocaleManager.get("omt_filechooser_title"));
			fileChooser.getExtensionFilters().add(dbFilter);
			File selectedFile = fileChooser.showOpenDialog(primaryStage);
			if (selectedFile != null) {
				quartetDbField.setText(selectedFile.getAbsolutePath());

				if (OMT.checkSqliteConn(selectedFile)) {

					accountCount = OMT.getAccountCount(selectedFile);

					OMT.accountDBLoad(true);

				} else {

					OMT.accountDBLoad(false);
				}

			}
		});

		browseButton.setTooltip(quartetDbPathTooltip);

		Tooltip mySqlHostLabelTooltip = new Tooltip(LocaleManager.get("omt_tab_general_mysqlhost_tooltip"));

		Label mySqlHostLabel = new Label(LocaleManager.get("omt_tab_general_mysqlhost") + " ");
		mySqlHostLabel.setTooltip(mySqlHostLabelTooltip);
		gridPane.add(mySqlHostLabel, 0, 3);

		mySqlHostField = new TextField();
		mySqlHostField.setText("127.0.0.1:3306");

		mySqlHostField.setTooltip(mySqlHostLabelTooltip);

		gridPane.add(mySqlHostField, 0, 4);

		Label mySqlUserLabel = new Label(LocaleManager.get("omt_tab_general_mysqluser") + " ");
		gridPane.add(mySqlUserLabel, 0, 5);

		mySqlUserField = new TextField();

		gridPane.add(mySqlUserField, 0, 6);

		Label mySqlPass = new Label(LocaleManager.get("omt_tab_general_mysqlpass") + " ");
		gridPane.add(mySqlPass, 1, 5);

		mySqlPassField = new PasswordField();
		mySqlPassField.setPromptText("secret");

		gridPane.add(mySqlPassField, 1, 6);

		Label mySqlNameLabel = new Label(LocaleManager.get("omt_tab_general_mysqlname") + " ");
		gridPane.add(mySqlNameLabel, 2, 5);

		mySqlNameField = new TextField();
		mySqlNameField.setPromptText("octet_db");

		Tooltip tooltip = new Tooltip(LocaleManager.get("omt_tab_general_mysqlname_tooltip"));
		mySqlNameField.setTooltip(tooltip);
		gridPane.add(mySqlNameField, 2, 6);

		VBox buttonContainer = new VBox();
		buttonContainer.setAlignment(Pos.CENTER);
		buttonContainer.setSpacing(16);

		dbConnectButton = new Button(LocaleManager.get("omt_tab_general_connect"));

		dbConnectButton.setTooltip(connectButtonTooltip);

		dbConnectButton.setOnAction(e -> {


			if (!Database.isConnected()) {

				OMT.createDatabaseConnection(getMySQLHostname(), getMySQLUsername(), getMySQLPassword(),
						getMySQLDatabaseName());

				dbConnectButton.setTooltip(disconnectButtonTooltip);

			} else {
				startButton.setDisable(true);
				Database.close();

				dbConnectButton.setText(LocaleManager.get("omt_tab_general_connect"));

				dbConnectButton.setTooltip(connectButtonTooltip);

				statusLabel.setText(LocaleManager.get("omt_status_notconnected"));

			}
		});

		buttonContainer.getChildren().add(dbConnectButton);

		startButton = new Button(LocaleManager.get("omt_tab_general_migrate"));
		migrateButtonDisable(true);

		startButton.setOnAction(e -> {

			task = new Task<Void>() {

				@Override
				protected Void call() throws Exception {

					if (OMT.summary()) {

						OMT.startMigrateProcess();

					} else {

						task.cancel();

					}

					return null;
				}
			};

			progressBar.progressProperty().bind(task.progressProperty());

			task.setOnCancelled(event -> {
				progressBar.progressProperty().unbind();
				progressBar.setProgress(0);
			});

			task.setOnSucceeded(event -> {

				statusLabel.setText(LocaleManager.get("omt_status_complete"));
				progressBar.progressProperty().unbind();
				progressBar.progressProperty().set(10);
				migrateButtonDisable(false);
			});

			task.setOnFailed(event -> {
				statusLabel.setText(LocaleManager.get("omt_status_failed"));
				progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
				progressBar.progressProperty().unbind();
				progressBar.progressProperty().set(10);
				migrateButtonDisable(false);

			});

			new Thread(task).start();
		});

		startButton.setTooltip(new Tooltip(LocaleManager.get("omt_tab_general_migrate_tooltip")));

		buttonContainer.getChildren().add(startButton);

		gridPane.add(buttonContainer, 1, 8);

		VBox progressVBox = new VBox(10);
		progressVBox.setAlignment(Pos.CENTER);

		progressBar = new ProgressBar(0);
		statusLabel = new Label(LocaleManager.get("omt_status_notconnected"));

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
if (mySqlNameField.getText().equals("")) {
	
	return "octet_db";
	
}
		return mySqlNameField.getText();

	}

	public void migrateButtonDisable(boolean enable) {

		startButton.setDisable(enable);

	}

	public void updateMigrateButtonState() {
		if (Database.isConnected()) {
			dbConnectButton.setText(LocaleManager.get("omt_tab_general_disconnect"));
		}

		migrateButtonDisable(!Database.isConnected() || !isQuartetDbFieldSet);
	}

	public void setStatusText(String text) {

		this.statusLabel.setText(text);

	}

	public Object getAccountCount() {
		return accountCount;
	}

}
