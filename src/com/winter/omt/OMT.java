package com.winter.omt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.yaml.snakeyaml.Yaml;

import com.winter.omt.data.LocaleManager;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class OMT extends Application {

	public static final String iconPath = "file:resources/img/OctetTransitionTool64.png";
	public static GeneralTab generalTab;
	public static PermissionsTab permissionsTab;
	public static DataTab dataTab;
	public static ObservableList<String> permGroups;
	public static Config config;
	public static LanguageTab langTab;
	public static Stage primaryStage;
	public static HostServices hostServices;
	private static TabPane tabPane;

	public static void start(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws MalformedURLException {
		OMT.primaryStage = primaryStage;
		hostServices = getHostServices();
		initialize(primaryStage);

	}

	static void clearPrimaryStage() {
		primaryStage.getScene().setRoot(new StackPane());
	}

	@SuppressWarnings("deprecation")
	static void initialize(Stage primaryStage) throws MalformedURLException {
		config = new Config();
		new LocaleManager(config.getLang());

		primaryStage.setTitle(LocaleManager.get("omt_app_title"));
		Image icon = new Image(iconPath);
		primaryStage.getIcons().add(icon);

		tabPane = new TabPane();

		generalTab = new GeneralTab(primaryStage);
		permissionsTab = new PermissionsTab(primaryStage);
		dataTab = new DataTab();
		AboutTab aboutTab = new AboutTab(hostServices);
		langTab = new LanguageTab(LocaleManager.currentLocale);

		tabPane.getTabs().addAll(generalTab.get(), permissionsTab.get(), dataTab.get(), langTab.get(), aboutTab.get());

		StackPane root = new StackPane();
		root.getChildren().add(tabPane);

		Scene scene = new Scene(root, 500, 500);
		File css = new File("resources/style.css");

		if (css.exists()) {
			scene.getStylesheets().add(css.toURL().toExternalForm());
		}
		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), OMT::refreshUI);
		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN), OMT::refreshUI);

		primaryStage.setWidth(525);
		primaryStage.setHeight(600);
		primaryStage.setResizable(false);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static void refreshUI() {
		Platform.runLater(() -> {
			UIState uiState = captureUIState();

			clearPrimaryStage();
			try {
				initialize(primaryStage);
				restoreUIState(uiState);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		});
	}

	public static void startMigrateProcess() {
		Platform.runLater(() -> generalTab.statusLabel.setText(LocaleManager.get("omt_status_working")));
		MigrateProcess.start(generalTab, permissionsTab, dataTab);

	}

	public static void createDatabaseConnection(String hostname, String dbUser, String dbPassword, String dbName) {

		try {

			System.out.println("MySQL Hostname: " + hostname);
			System.out.println("MySQL Username: " + dbUser);
			System.out.println("MySQL Password: *********");
			System.out.println("MySQL Database Name: " + dbName);

			new Database(hostname, dbUser, dbPassword, dbName);

			if (Database.octetDatabaseExists()) {

				generalTab.setStatusText(String.format(LocaleManager.get("omt_status_connected_existing"), dbName));

			} else {

				generalTab.setStatusText(String.format(LocaleManager.get("omt_status_connected_new"), dbName));

			}

			generalTab.updateMigrateButtonState();

		} catch (SQLException | PoolInitializationException e) {
			e.printStackTrace();
			showMysqlFailDialog(e);
		}
	}

	public static void createSQLiteConnection(String sqlitePath) {

		try {

			System.out.println("SQLite Path: " + sqlitePath);

			new Database(sqlitePath);

			generalTab.setStatusText("Connected to SQLite database");

			generalTab.updateMigrateButtonState();

		} catch (SQLException e) {
			e.printStackTrace();
			showSqliteFailDialog(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static void parsePermissionGroupsYml(File permFile) {
		permGroups = FXCollections.observableArrayList();
		Yaml yaml = new Yaml();
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(permFile);

			Map<String, Object> data = yaml.load(inputStream);

			Map<String, Object> pGroups = (Map<String, Object>) data.get("permission-groups");
			for (String groupName : pGroups.keySet()) {

				Map<String, Object> permissionGroup = (Map<String, Object>) pGroups.get(groupName);
				if (permissionGroup != null) {

					permGroups.add(groupName);

				}

			}

			FXCollections.reverse(permGroups);

		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}

	}

	@SuppressWarnings("deprecation")
	static boolean summary() {
		CompletableFuture<Boolean> userChoiceFuture = new CompletableFuture<>();

		Platform.runLater(() -> {
			Stage dialogStage = new Stage();
			dialogStage.initModality(Modality.WINDOW_MODAL);

			Label summaryTitle = new Label(LocaleManager.get("omt_migrate_summary"));

			summaryTitle.setStyle("-fx-font-weight: bold;");

			Label summary = new Label(String.format(LocaleManager.get("omt_migrate_summary_countname"),
					generalTab.getAccountCount(), generalTab.getMySQLDatabaseName()));
			summary.setAlignment(Pos.CENTER);

			Label variablesSummary = new Label();

			if (dataTab.transferVariables) {

				variablesSummary.setText(LocaleManager.get("omt_migrate_summary_data_variables_transferred"));

			} else {

				variablesSummary.setText(LocaleManager.get("omt_migrate_summary_data_variables_nottransferred"));

			}

			Label existingSummary = new Label();

			switch (dataTab.getExistingAccountOption()) {

			case 0:

				existingSummary.setText(LocaleManager.get("omt_migrate_summary_data_duplicate1"));

				break;

			case 1:

				existingSummary.setText(LocaleManager.get("omt_migrate_summary_data_duplicate2"));

				break;

			case 2:

				existingSummary.setText(LocaleManager.get("omt_migrate_summary_data_duplicate3"));

				break;

			}

			existingSummary.setTextAlignment(TextAlignment.CENTER);

			Label proceedAsk = new Label(LocaleManager.get("omt_migrate_summary_ask"));

			proceedAsk.setStyle("-fx-font-weight: bold;");

			Button yesButton = new Button(LocaleManager.get("omt_common_yes"));
			yesButton.setOnAction(e -> {
				userChoiceFuture.complete(true);
				dialogStage.close();
			});

			Button noButton = new Button(LocaleManager.get("omt_common_no"));
			noButton.setOnAction(e -> {
				userChoiceFuture.complete(false);
				dialogStage.close();
			});

			HBox buttonBox = new HBox(10, yesButton, noButton);
			buttonBox.setAlignment(Pos.CENTER);

			VBox dialogVBox = new VBox(10, summaryTitle, summary, variablesSummary, existingSummary, proceedAsk,
					buttonBox);

			dialogVBox.setAlignment(Pos.CENTER);

			Scene dialogScene = new Scene(dialogVBox, 800, 250);

			File css = new File("resources/style.css");
			try {
				if (css.exists()) {
					dialogScene.getStylesheets().add(css.toURL().toExternalForm());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			dialogStage.setMinWidth(600);
			dialogStage.setMinHeight(300);
			dialogStage.setScene(dialogScene);
			dialogStage.showAndWait();
		});

		try {
			return userChoiceFuture.get();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	@SuppressWarnings("deprecation")
	static void accountDBLoad(boolean success) {

		Platform.runLater(() -> {
			Stage dialogStage = new Stage();
			dialogStage.initModality(Modality.WINDOW_MODAL);

			Label loadResult = new Label();

			if (success) {

				loadResult.setText(String.format(LocaleManager.get("omt_tab_general_accountdbload"),
						generalTab.getAccountCount()));

			} else {

				loadResult.setTextFill(Color.web("#eb4034"));

				loadResult.setText(LocaleManager.get("omt_tab_general_accountdbload_fail"));

			}

			loadResult.setStyle("-fx-font-weight: bold;");

			Button okButton = new Button("OK");
			okButton.setOnAction(e -> {
				dialogStage.close();
			});

			HBox buttonBox = new HBox(10, okButton);
			buttonBox.setAlignment(Pos.CENTER);

			VBox dialogVBox = new VBox(10, loadResult, buttonBox);

			dialogVBox.setAlignment(Pos.CENTER);

			Scene dialogScene = new Scene(dialogVBox, 350, 300);

			
			
			File css = new File("resources/style.css");
			try {
				if (css.exists()) {
					dialogScene.getStylesheets().add(css.toURL().toExternalForm());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			dialogStage.setMinWidth(400);
			dialogStage.setScene(dialogScene);
			dialogStage.showAndWait();
		});

	}

	public static boolean checkSqliteConn(File selectedFile) {

		String sqliteurl = "jdbc:sqlite:" + selectedFile.getAbsolutePath();

		boolean validId = false;
		boolean validXml = false;
		try {
			Connection sqLiteConn = DriverManager.getConnection(sqliteurl);

			try (PreparedStatement selectStmt = sqLiteConn
					.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name='tbl_account'")) {
				ResultSet rs = selectStmt.executeQuery();

				while (rs.next()) {

					if (rs.getString(1).equals("tbl_account")) {

						String columnCheck = "PRAGMA table_info(" + rs.getString(1) + ")";

						Statement columnCheckStatement = sqLiteConn.createStatement();
						try (ResultSet resultSet = columnCheckStatement.executeQuery(columnCheck)) {

							while (resultSet.next()) {
								String columnName = resultSet.getString("name");
								String dataType = resultSet.getString("type");
								if (columnName.equals("id") && dataType.equals("TEXT")) {
									validId = true;

								}
								if (columnName.equals("xml") && dataType.equals("TEXT")) {

									validXml = true;

								}

							}

						}

					}

				}

			} catch (SQLException e) {

				e.printStackTrace();

			}

		} catch (SQLException e) {

			e.printStackTrace();

		}
		return validXml && validId;

	}

	@SuppressWarnings("deprecation")
	static void showMysqlFailDialog(Exception sql) {

		Platform.runLater(() -> {
			Stage dialogStage = new Stage();
			dialogStage.initModality(Modality.WINDOW_MODAL);

			Label failText = new Label();

			failText.setTextFill(Color.web("#eb4034"));
			failText.setStyle("-fx-font-weight: bold;");

			failText.setText(LocaleManager.get("omt_mysql_connection_failed") + "! ");

			Label eMessage = new Label(sql.getLocalizedMessage());

			Button okButton = new Button("OK");
			okButton.setOnAction(e -> {
				dialogStage.close();
			});

			HBox buttonBox = new HBox(10, okButton);
			buttonBox.setAlignment(Pos.CENTER);

			VBox dialogVBox = new VBox(10, failText, eMessage, buttonBox);

			dialogVBox.setAlignment(Pos.CENTER);

			Scene dialogScene = new Scene(dialogVBox, 700, 130);

			File css = new File("resources/style.css");
			try {
				if (css.exists()) {
					dialogScene.getStylesheets().add(css.toURL().toExternalForm());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			dialogStage.setMinWidth(700);
			dialogStage.setMinHeight(150);
			dialogStage.setScene(dialogScene);
			dialogStage.showAndWait();
		});

	}

	public static int getAccountCount(File selectedFile) {

		String sqliteurl = "jdbc:sqlite:" + selectedFile.getAbsolutePath();
		try {
			Connection sqLiteConn = DriverManager.getConnection(sqliteurl);

			try (PreparedStatement accCount = sqLiteConn.prepareStatement("SELECT COUNT(*) from tbl_account")) {

				ResultSet rs = accCount.executeQuery();

				int accountsCount = rs.getInt(1);

				return accountsCount;

			} catch (SQLException e) {

				e.printStackTrace();

			}

		} catch (SQLException e) {

			e.printStackTrace();

		}
		return 0;

	}

	private static UIState captureUIState() {
		UIState uiState = new UIState();

		if (tabPane != null) {
			uiState.selectedTabIndex = tabPane.getSelectionModel().getSelectedIndex();
		}

		if (generalTab != null) {
			uiState.quartetDatabasePath = generalTab.getQuartetDatabasePath();
			uiState.accountCount = generalTab.getAccountCount();
			uiState.migrationType = generalTab.getMigrationType();
			uiState.mySqlHostname = generalTab.getMySQLHostname();
			uiState.mySqlUsername = generalTab.getMySQLUsername();
			uiState.mySqlPassword = generalTab.getMySQLPassword();
			uiState.mySqlDatabaseName = generalTab.getMySQLDatabaseName();
			uiState.sqlitePath = generalTab.getSQLitePath();
			uiState.statusText = generalTab.statusLabel.getText();
		}

		if (permissionsTab != null) {
			uiState.permissionYmlPath = permissionsTab.getPermissionYmlPath();
			uiState.permissionFileConfigured = permissionsTab.isPermissionFileConfigured();
			uiState.playerPermissionGroup = permissionsTab.getPlayerPermissionGroup();
			uiState.cmPermissionGroup = permissionsTab.getCMPermissionGroup();
			uiState.gmPermissionGroup = permissionsTab.getGMPermissionGroup();
		}

		if (dataTab != null) {
			uiState.transferVariables = dataTab.transferingVariables();
			uiState.existingAccountOption = dataTab.getExistingAccountOption();
		}

		return uiState;
	}

	private static void restoreUIState(UIState uiState) {
		if (uiState == null) {
			return;
		}

		generalTab.setQuartetDatabasePath(uiState.quartetDatabasePath);
		generalTab.setAccountCount(uiState.accountCount);
		generalTab.setMigrationType(uiState.migrationType);
		generalTab.setMySQLHostname(uiState.mySqlHostname);
		generalTab.setMySQLUsername(uiState.mySqlUsername);
		generalTab.setMySQLPassword(uiState.mySqlPassword);
		generalTab.setMySQLDatabaseName(uiState.mySqlDatabaseName);
		generalTab.setSQLitePath(uiState.sqlitePath);

		permissionsTab.setPermissionYmlPath(uiState.permissionYmlPath);
		permissionsTab.setPlayerPermissionGroup(uiState.playerPermissionGroup);
		permissionsTab.setCMPermissionGroup(uiState.cmPermissionGroup);
		permissionsTab.setGMPermissionGroup(uiState.gmPermissionGroup);
		if (uiState.permissionFileConfigured) {
			permissionsTab.notifyPermissionFileSet();
		}

		dataTab.setTransferVariables(uiState.transferVariables);
		dataTab.setExistingAccountOption(uiState.existingAccountOption);

		if (uiState.statusText != null && !uiState.statusText.isBlank()) {
			generalTab.setStatusText(uiState.statusText);
		}

		generalTab.updateMigrateButtonState();

		if (tabPane != null && uiState.selectedTabIndex >= 0
				&& uiState.selectedTabIndex < tabPane.getTabs().size()) {
			tabPane.getSelectionModel().select(uiState.selectedTabIndex);
		}
	}

	private static class UIState {
		int selectedTabIndex;
		String quartetDatabasePath;
		int accountCount;
		String migrationType;
		String mySqlHostname;
		String mySqlUsername;
		String mySqlPassword;
		String mySqlDatabaseName;
		String sqlitePath;
		String statusText;
		String permissionYmlPath;
		boolean permissionFileConfigured;
		String playerPermissionGroup;
		String cmPermissionGroup;
		String gmPermissionGroup;
		boolean transferVariables;
		int existingAccountOption;
	}

	@SuppressWarnings("deprecation")
	static void showSqliteFailDialog(Exception e) {
		Platform.runLater(() -> {
			Stage dialogStage = new Stage();
			dialogStage.initModality(Modality.WINDOW_MODAL);

			Label errorLabel = new Label("Failed to connect to SQLite database: " + e.getMessage());
			Button okButton = new Button("OK");
			okButton.setOnAction(event -> dialogStage.close());

			VBox dialogVBox = new VBox(10, errorLabel, okButton);
			dialogVBox.setAlignment(Pos.CENTER);

			Scene dialogScene = new Scene(dialogVBox, 600, 300);

			File css = new File("resources/style.css");
			try {
				if (css.exists()) {
					dialogScene.getStylesheets().add(css.toURL().toExternalForm());
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			dialogStage.setMinWidth(500);
			dialogStage.setMinHeight(300);
		
			dialogStage.setScene(dialogScene);
			dialogStage.showAndWait();
		});
	}

}
