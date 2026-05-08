package com.winter.omt;

import java.io.File;

import com.winter.omt.data.LocaleManager;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class GeneralTab {

	Tab generalTab;
	GridPane gridPane;
	TextField quartetDbField;
	TextField mySqlHostField;
	TextField mySqlUserField;
	PasswordField mySqlPassField;
	TextField mySqlNameField;
	TextField sqlitePathField;
	ComboBox<String> migrationTypeComboBox;	Label mySqlHostLabel;
	Label mySqlUserLabel;
	Label mySqlPass;
	Label mySqlNameLabel;
	Label sqlitePathLabel;
	Button sqliteBrowseButton;	Button startButton;
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

		gridPane = new GridPane();
		gridPane.setHgap(10);
		gridPane.setVgap(12);

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

		Label migrationTypeLabel = new Label("Migration Type ");
		gridPane.add(migrationTypeLabel, 2, 1);

		migrationTypeComboBox = new ComboBox<>();
		migrationTypeComboBox.getItems().addAll("SQLite -> SQLite", "SQLite -> MySQL");
		migrationTypeComboBox.setValue("SQLite -> SQLite");
		migrationTypeComboBox.setOnAction(e -> updateUIForMigrationType());
		gridPane.add(migrationTypeComboBox, 2, 2);

		Tooltip mySqlHostLabelTooltip = new Tooltip(LocaleManager.get("omt_tab_general_mysqlhost_tooltip"));

		
		sqlitePathLabel = new Label("Octet SQLite Path ");
		sqlitePathLabel.setDisable(true);
		gridPane.add(sqlitePathLabel, 0, 4);

		sqlitePathField = new TextField();
		sqlitePathField.setDisable(true);
		gridPane.add(sqlitePathField, 0, 5);

		sqliteBrowseButton = new Button(LocaleManager.get("omt_common_browse"));
		sqliteBrowseButton.setDisable(true);
		gridPane.add(sqliteBrowseButton, 1, 5);
		
		mySqlHostLabel = new Label(LocaleManager.get("omt_tab_general_mysqlhost") + " ");
		mySqlHostLabel.setTooltip(mySqlHostLabelTooltip);
		gridPane.add(mySqlHostLabel, 0, 6);

		mySqlHostField = new TextField();
		mySqlHostField.setText("127.0.0.1:3306");

		mySqlHostField.setTooltip(mySqlHostLabelTooltip);

		gridPane.add(mySqlHostField, 0, 7);

		mySqlUserLabel = new Label(LocaleManager.get("omt_tab_general_mysqluser") + " ");
		gridPane.add(mySqlUserLabel, 0, 8);

		mySqlUserField = new TextField();

		gridPane.add(mySqlUserField, 0, 9);

		mySqlPass = new Label(LocaleManager.get("omt_tab_general_mysqlpass") + " ");
		gridPane.add(mySqlPass, 1, 8);

		mySqlPassField = new PasswordField();
		mySqlPassField.setPromptText("secret");

		gridPane.add(mySqlPassField, 1, 9);

		mySqlNameLabel = new Label(LocaleManager.get("omt_tab_general_mysqlname") + " ");
		gridPane.add(mySqlNameLabel, 2, 8);

		mySqlNameField = new TextField();
		mySqlNameField.setPromptText("octet_db");

		Tooltip tooltip = new Tooltip(LocaleManager.get("omt_tab_general_mysqlname_tooltip"));
		mySqlNameField.setTooltip(tooltip);
		gridPane.add(mySqlNameField, 2, 9);

		sqliteBrowseButton.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Select Octet SQLite Database");
			ExtensionFilter sqliteFilter = new ExtensionFilter("SQLite Database", "*.sqlite");
			fileChooser.getExtensionFilters().add(sqliteFilter);
			fileChooser.setInitialFileName("octet.sqlite");
			File selectedFile = fileChooser.showSaveDialog(primaryStage);
			if (selectedFile != null) {
				sqlitePathField.setText(selectedFile.getAbsolutePath());
				updateMigrateButtonState();
			}
		});

		VBox buttonContainer = new VBox();
		buttonContainer.setAlignment(Pos.CENTER);
		buttonContainer.setSpacing(12);
		dbConnectButton = new Button(LocaleManager.get("omt_tab_general_connect"));

		dbConnectButton.setTooltip(connectButtonTooltip);

		dbConnectButton.setOnAction(e -> {


			if (!Database.isConnected() && sqlitePathField.getText() != null) {

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

					String migrationType = migrationTypeComboBox.getValue();
if ("SQLite -> SQLite".equals(migrationType)) {
						OMT.createSQLiteConnection(getSQLitePath());
					}

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

		gridPane.add(buttonContainer, 0, 11);
		GridPane.setColumnSpan(buttonContainer, 3);
		GridPane.setHalignment(buttonContainer, HPos.CENTER);

		VBox progressVBox = new VBox(10);
		progressVBox.setAlignment(Pos.TOP_CENTER);

		progressBar = new ProgressBar(0);
		statusLabel = new Label(LocaleManager.get("omt_status_notconnected"));

		progressVBox.getChildren().addAll(statusLabel, progressBar);

		progressBar.setMaxWidth(Double.MAX_VALUE);
		progressVBox.setFillWidth(true);

		gridPane.add(progressVBox, 0, 12);
		GridPane.setColumnSpan(progressVBox, GridPane.REMAINING);

		generalTab.setContent(gridPane);

		updateUIForMigrationType(); // Initial setup

	}

	private void updateUIForMigrationType() {
		String selected = migrationTypeComboBox.getValue();
		if ("SQLite -> SQLite".equals(selected)) {
			// Disable MySQL fields, enable SQLite fields
			mySqlHostLabel.setDisable(true);
			mySqlHostField.setDisable(true);
			mySqlUserLabel.setDisable(true);
			mySqlUserField.setDisable(true);
			mySqlPass.setDisable(true);
			mySqlPassField.setDisable(true);
			mySqlNameLabel.setDisable(true);
			mySqlNameField.setDisable(true);
			
			sqlitePathLabel.setDisable(false);
			sqlitePathField.setDisable(false);
			sqliteBrowseButton.setDisable(false);
			dbConnectButton.setDisable(true);
			
			statusLabel.setText("Ready for SQLite migration");
		} else {
			mySqlHostLabel.setDisable(false);
			mySqlHostField.setDisable(false);
			mySqlUserLabel.setDisable(false);
			mySqlUserField.setDisable(false);
			mySqlPass.setDisable(false);
			mySqlPassField.setDisable(false);
			mySqlNameLabel.setDisable(false);
			mySqlNameField.setDisable(false);
			
			sqlitePathLabel.setDisable(true);
			sqlitePathField.setDisable(true);
			sqliteBrowseButton.setDisable(true);
			dbConnectButton.setDisable(false);
			
			if (Database.isConnected()) {
				statusLabel.setText("Connected to MySQL");
			} else {
				statusLabel.setText(LocaleManager.get("omt_status_notconnected"));
			}
		}
		updateMigrateButtonState();
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

	public int getAccountCount() {
		return accountCount;
	}

	public void migrateButtonDisable(boolean enable) {

		startButton.setDisable(enable);

	}

	public void updateMigrateButtonState() {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(this::updateMigrateButtonState);
			return;
		}

		String selected = migrationTypeComboBox.getValue();
		boolean isConnectedOrReady = false;
		if ("SQLite -> SQLite".equals(selected)) {
			isConnectedOrReady = !sqlitePathField.getText().trim().isEmpty();
		} else {
			if (Database.isConnected()) {
				dbConnectButton.setText(LocaleManager.get("omt_tab_general_disconnect"));
			}
			isConnectedOrReady = Database.isConnected();
		}

		migrateButtonDisable(!isConnectedOrReady || !isQuartetDbFieldSet);
	}

	public void setStatusText(String text) {
		if (Platform.isFxApplicationThread()) {
			this.statusLabel.setText(text);
		} else {
			Platform.runLater(() -> this.statusLabel.setText(text));
		}

	}

	public String getSQLitePath() {
		return sqlitePathField.getText();
	}

	public String getMigrationType() {
		return migrationTypeComboBox.getValue();
	}

	public void setQuartetDatabasePath(String path) {
		quartetDbField.setText(path == null ? "" : path);
	}

	public void setAccountCount(int accountCount) {
		this.accountCount = accountCount;
	}

	public void setMigrationType(String migrationType) {
		if (migrationType != null && migrationTypeComboBox.getItems().contains(migrationType)) {
			migrationTypeComboBox.setValue(migrationType);
			updateUIForMigrationType();
		}
	}

	public void setMySQLHostname(String hostname) {
		mySqlHostField.setText(hostname == null ? "" : hostname);
	}

	public void setMySQLUsername(String username) {
		mySqlUserField.setText(username == null ? "" : username);
	}

	public void setMySQLPassword(String password) {
		mySqlPassField.setText(password == null ? "" : password);
	}

	public void setMySQLDatabaseName(String databaseName) {
		mySqlNameField.setText(databaseName == null ? "" : databaseName);
	}

	public void setSQLitePath(String sqlitePath) {
		sqlitePathField.setText(sqlitePath == null ? "" : sqlitePath);
	}

}
