package com.winter.omt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class OMT extends Application {

	public static final String iconPath = "file:resources/OctetTransitionTool64.png";
	public static GeneralTab generalTab;
	public static PermissionsTab permissionsTab;
	public static DataTab dataTab;
    public static ObservableList<String> permGroups;
	
	public static void main(String[] args) {
		launch(args);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void start(Stage primaryStage) throws MalformedURLException {
		primaryStage.setTitle("Quartet → Octet database migration tool");
        Image icon = new Image(iconPath);
        primaryStage.getIcons().add(icon);



        
		
        TabPane tabPane = new TabPane();
 
       generalTab = new GeneralTab(primaryStage);
        

        permissionsTab = new PermissionsTab(primaryStage);

        
      dataTab = new DataTab();
      

      AboutTab aboutTab = new AboutTab(getHostServices());
      
        tabPane.getTabs().addAll(generalTab.get(), permissionsTab.get(), dataTab.get(), aboutTab.get());

        StackPane root = new StackPane();
        root.getChildren().add(tabPane);

        Scene scene = new Scene(root, 500, 500);
        File css = new File("resources/style.css");
        
        if (css.exists()) {
          scene.getStylesheets().add(css.toURL().toExternalForm());
        }

        primaryStage.setMaxWidth(500);
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(500);
        primaryStage.setMaxHeight(500);
        primaryStage.setScene(scene);
        primaryStage.show();
		

	}
	
	
	public static void startMigrateProcess() {
		
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
			
		generalTab.setStatusText("Connected to an existing database " + dbName);
			
		} else {
			
			generalTab.setStatusText("Connected and created " + dbName + " database");
			
			
		}
		
		
		generalTab.updateMigrateButtonState();
		
		
		} catch (SQLException e) {
			e.printStackTrace();
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
	
	
	
	
	

}

