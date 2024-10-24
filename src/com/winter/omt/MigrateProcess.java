package com.winter.omt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import at.favre.lib.crypto.bcrypt.BCrypt;

import com.winter.omt.data.BeItem;
import com.winter.omt.data.CoItem;
import com.winter.omt.data.DB;
import com.winter.omt.data.EnItem;
import com.winter.omt.data.FieldMemoEntry;
import com.winter.omt.data.ItemHotkey;
import com.winter.omt.data.LocaleManager;
import com.winter.omt.data.Locker;
import com.winter.omt.data.LockerManager;
import com.winter.omt.data.SkillHotkey;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;

public class MigrateProcess {
	private static LockerManager lockerMgr;

	public static void start(GeneralTab general, PermissionsTab permissions, DataTab data) {
		System.out.println("Quartet DB Path: " + general.getQuartetDatabasePath());
		System.out.println("Transfering variables? " + data.transferingVariables());

		String sqliteurl = "jdbc:sqlite:" + general.getQuartetDatabasePath();
		boolean validId = false;
		boolean validXml = false;
		DB.initBeItems();

		try (Connection sqLiteConn = DriverManager.getConnection(sqliteurl)) {
			try (PreparedStatement selectStmt = sqLiteConn
					.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name='tbl_account'")) {
				ResultSet rs = selectStmt.executeQuery();

				while (rs.next()) {
					if (rs.getString(1).equals("tbl_account")) {
						String columnCheck = "PRAGMA table_info(" + rs.getString(1) + ")";
						try (Statement columnCheckStatement = sqLiteConn.createStatement();
								ResultSet resultSet = columnCheckStatement.executeQuery(columnCheck)) {
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

			if (validId && validXml) {
				try (Connection conn = Database.getConnection()) {
					createTables(conn);

					conn.setAutoCommit(false);

					try (Statement selectXmlStmt = sqLiteConn.createStatement();
							ResultSet rs = selectXmlStmt
									.executeQuery("SELECT xml FROM tbl_account where id NOT NULL AND xml NOT NULL")) {
						while (rs.next()) {
							String xml = rs.getString(1).replace("encoding=\"UTF-16\"", "encoding=\"UTF-8\"");

							SAXReader reader = new SAXReader();
							try {
								Document doc = reader.read(new ByteArrayInputStream(xml.getBytes()));

								if (!doc.getRootElement().getName().equals("Account")) {
									System.out.println("Invalid Account XML Schema. Skipping...");
									continue;
								}

								Node mainNode = doc.selectSingleNode("/Account");
								String accountName = mainNode.selectSingleNode("AccountID").getText();
								int accountCount = 0;

								try (PreparedStatement accCheckPs = conn
										.prepareStatement("SELECT COUNT(*) FROM accounts WHERE username = ?")) {
									accCheckPs.setString(1, accountName);
									try (ResultSet accCount = accCheckPs.executeQuery()) {
										if (accCount.next()) {
											accountCount = accCount.getInt(1);
										}
									}
								} catch (SQLException e) {
									e.printStackTrace();
								}

								if (accountCount == 0) {
									continueMigration(accountName, mainNode, conn, data, permissions);
								} else {
									switch (data.getExistingAccountOption()) {
									case 0: // Ignore
										continue;
									case 1: // Overwrite
										deleteAccount(accountName, conn);
										continueMigration(accountName, mainNode, conn, data, permissions);
										break;
									case 2: // Ask user
										if (askOnExistingAccount(accountName)) {
											deleteAccount(accountName, conn);
											continueMigration(accountName, mainNode, conn, data, permissions);
										} else {
											continue;
										}
										break;
									}
								}

							} catch (DocumentException e) {
								e.printStackTrace();
								System.out.println("Invalid Account XML Schema. Skipping...");
								continue;
							}
						}
						conn.commit();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	private static boolean askOnExistingAccount(String accountName) {
		CompletableFuture<Boolean> userChoiceFuture = new CompletableFuture<>();

		Platform.runLater(() -> {
			Stage dialogStage = new Stage();
			dialogStage.initModality(Modality.WINDOW_MODAL);

			Label existingAccountText = new Label(
					String.format(LocaleManager.get("omt_migrate_process_existingaccountask"), accountName));
			existingAccountText.setAlignment(Pos.CENTER);
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

			VBox dialogVBox = new VBox(20, existingAccountText, buttonBox);
			dialogVBox.setAlignment(Pos.CENTER);

			Scene dialogScene = new Scene(dialogVBox, 450, 200);

			File css = new File("resources/style.css");
			try {
				if (css.exists()) {
					dialogScene.getStylesheets().add(css.toURL().toExternalForm());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			dialogStage.setTitle(LocaleManager.get("omt_migrate_process_existingaccountask_title"));
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

	private static void deleteAccount(String accountName, Connection conn) {

		try {

			try (PreparedStatement deleteAccountPs = conn.prepareStatement("DELETE FROM accounts WHERE username = ?")) {

				deleteAccountPs.setString(1, accountName);

				deleteAccountPs.executeUpdate();

			}

		} catch (SQLException e) {

			e.printStackTrace();

		}

	}

	private static void continueMigration(String accountName, Node mainNode, Connection conn, DataTab data,
			PermissionsTab permissions) {

		String charName = null;
		int phone = 0;
		int gender = 0;
		int school = 0;
		int face = 0;
		int hair = 0;
		int skin = 0;
		int birthMonth = 0;
		int birthDay = 0;
		int blood = 0;

		int fieldId = 0;
		int x = 0;
		int y = 0;

		int saveFieldId = 0;
		int saveX = 0;
		int saveY = 0;

		int level = 0;
		int exp = 0;
		int dexLevel1 = 0;
		int dexExp1 = 0;
		int dexLevel2 = 0;
		int dexExp2 = 0;
		int dexLevel3 = 0;
		int dexExp3 = 0;
		int dexLevel4 = 0;
		int dexExp4 = 0;

		int grade = 0;
		int title = 0;
		int skillPoint = 0;
		int taff = 0;
		int shopPoint = 0;

		int tag = 0;
		boolean picket = false;
		boolean banned = false;
		String picketContent = null;
		String authType = permissions.getPlayerPermissionGroup();

		int headwear = 0, upperwear = 0, backwear = 0, handwear = 0, lowerwear = 0, footwear = 0;

		List<FieldMemoEntry> fieldMemos = new ArrayList<FieldMemoEntry>();
		Map<Integer, BeItem> beItems = new HashMap<Integer, BeItem>();
		Map<Integer, CoItem> coItems = new HashMap<Integer, CoItem>();
		Map<Integer, EnItem> enItems = new LinkedHashMap<Integer, EnItem>();
		ItemHotkey[] itemHotkeys = new ItemHotkey[10];

		lockerMgr = new LockerManager();

		List<Integer> skills = new ArrayList<Integer>();

		Map<Integer, SkillHotkey[]> skillHotkeys = new HashMap<Integer, SkillHotkey[]>(4);
		for (int i = 1; i <= 4; i++) {

			skillHotkeys.put(i, new SkillHotkey[9]);

		}
		Map<String, String> variables = null;
		if (data.transferVariables) {
			variables = new HashMap<String, String>();
		}

		String password = BCrypt.withDefaults().hashToString(12,
				mainNode.selectSingleNode("Password").getText().toCharArray());

		boolean hasChar = mainNode.selectSingleNode("HasChara").getText().equals("true");
		if (hasChar) {

			if (data.transferVariables) {

				Node variablesNode = mainNode.selectSingleNode("Variables");

				List<Node> variableNodes = variablesNode.selectNodes("Item");

				for (Node variableNode : variableNodes) {

					String varname = variableNode.valueOf("@Key").stripTrailing();
					variables.put(varname, variableNode.valueOf("@Value"));

				}

			}

			charName = mainNode.selectSingleNode("Name").getText();
			
			
			phone = Integer.parseInt(mainNode.selectSingleNode("TelNumber").getText());
			gender = Integer.parseInt(mainNode.selectSingleNode("Sex").getText());
			school = Integer.parseInt(mainNode.selectSingleNode("School").getText());
			face = Integer.parseInt(mainNode.selectSingleNode("Face").getText());
			hair = Integer.parseInt(mainNode.selectSingleNode("Hair").getText());
			skin = Integer.parseInt(mainNode.selectSingleNode("Skin").getText());
			birthMonth = Integer.parseInt(mainNode.selectSingleNode("BirthMonth").getText());
			birthDay = Integer.parseInt(mainNode.selectSingleNode("BirthDay").getText());
			blood = Integer.parseInt(mainNode.selectSingleNode("BloodType").getText());

			authType = mainNode.selectSingleNode("AuthType").getText();

			if (authType.equals("gm")) {

				authType = permissions.getGMPermissionGroup();

			} else if (authType.equals("cm")) {

				authType = permissions.getCMPermissionGroup();

			} else {

				authType = permissions.getPlayerPermissionGroup();

			}

			fieldId = Integer.parseInt(mainNode.selectSingleNode("FieldID").getText());
			x = Integer.parseInt(mainNode.selectSingleNode("X").getText());
			y = Integer.parseInt(mainNode.selectSingleNode("Y").getText());

			saveFieldId = Integer.parseInt(mainNode.selectSingleNode("SaveFieldID").getText());
			saveX = Integer.parseInt(mainNode.selectSingleNode("SaveX").getText());
			saveY = Integer.parseInt(mainNode.selectSingleNode("SaveY").getText());

			Node memoNode = mainNode.selectSingleNode("Memo");

			List<Node> memoNodes = memoNode.selectNodes("Item");

			for (Node memo : memoNodes) {

				int index = memo.numberValueOf("@Index").intValue();
				int memoFieldId = memo.numberValueOf("@Field").intValue();
				int memoX = memo.numberValueOf("@X").intValue();
				int memoY = memo.numberValueOf("@Y").intValue();

				fieldMemos.add(new FieldMemoEntry(index, memoFieldId, memoX, memoY));

			}

			Node beItemsNode = mainNode.selectSingleNode("BeItems");

			List<Node> beItemNodes = beItemsNode.selectNodes("Item");
			int i = 1;
			for (Node beItem : beItemNodes) {

				int id = beItem.numberValueOf("@Type").intValue();
				int slot = beItem.numberValueOf("@Slot0").intValue();
				int slot2 = beItem.numberValueOf("@Slot1").intValue();
				int slot3 = beItem.numberValueOf("@Slot2").intValue();
				int slot4 = beItem.numberValueOf("@Slot3").intValue();
				int slot5 = beItem.numberValueOf("@Slot4").intValue();

				BeItem item = new BeItem(id);

				item.setSlot1(new CoItem(slot));
				item.setSlot2(new CoItem(slot2));
				item.setSlot3(new CoItem(slot3));
				item.setSlot4(new CoItem(slot4));
				item.setSlot5(new CoItem(slot5));
				item.slot = i;
				beItems.put(i, item);
				i++;

			}

			Node epuipNode = mainNode.selectSingleNode("Equips");

			List<Node> equipNodes = epuipNode.selectNodes("Item");

			for (Node item : equipNodes) {

				boolean star = item.valueOf("@Star").equals("true");

				if (!star) {

					int slot = item.numberValueOf("@Index").intValue() + 1;

					BeItem equippedItem = beItems.get(slot);

					if (equippedItem != null) {

						int itemPos = DB.getBeItemPos(equippedItem.id);
						switch (itemPos) {
						case 1:

							headwear = slot;

							break;
						case 4:

							backwear = slot;

							break;
						case 8:

							handwear = slot;

							break;

						case 16:
							handwear = slot;

							break;

						case 64:
							upperwear = slot;

							break;

						case 128:
							lowerwear = slot;

							break;

						case 192:

							upperwear = slot;
							lowerwear = slot;

							break;

						case 256:
							footwear = slot;

							break;

						case 448:

							upperwear = slot;
							lowerwear = slot;
							footwear = slot;

							break;

						}

					}

				}

			}

			Node coItemsNode = mainNode.selectSingleNode("CoItems");

			List<Node> coItemNodes = coItemsNode.selectNodes("Item");
			for (Node coItem : coItemNodes) {

				int id = coItem.numberValueOf("@Type").intValue();
				int count = coItem.numberValueOf("@Count").intValue();

				coItems.put(id, new CoItem(id, count));

			}

			Node enItemsNode = mainNode.selectSingleNode("EnItems");

			List<Node> enItemNodes = enItemsNode.selectNodes("Item");
			for (Node enItem : enItemNodes) {

				int id = enItem.numberValueOf("@Type").intValue();
				int count = enItem.numberValueOf("@Count").intValue();

				enItems.put(id, new EnItem(id, count));

			}

			Node locker0Node = mainNode.selectSingleNode("Locker0");

			processLocker(locker0Node, 0);

			Node locker1Node = mainNode.selectSingleNode("Locker1");

			processLocker(locker1Node, 1);

			Node locker2Node = mainNode.selectSingleNode("Locker2");

			processLocker(locker2Node, 2);

			Node locker3Node = mainNode.selectSingleNode("Locker3");

			processLocker(locker3Node, 3);

			Node locker4Node = mainNode.selectSingleNode("Locker4");

			processLocker(locker4Node, 4);

			Node locker5Node = mainNode.selectSingleNode("Locker5");

			processLocker(locker5Node, 5);

			Node itemHotkeysNode = mainNode.selectSingleNode("QuickSlots");

			List<Node> itemHotkeyNodes = itemHotkeysNode.selectNodes("Item");

			i = 0;
			for (Node itemHotkey : itemHotkeyNodes) {

				int itemType = itemHotkey.numberValueOf("@Kind").intValue();
				int itemId;

				if (itemType == 2) {

					itemId = itemHotkey.numberValueOf("@Index").intValue() + 1;

				} else {

					itemId = itemHotkey.numberValueOf("@Type").intValue();

				}

				itemHotkeys[i] = new ItemHotkey(itemId, itemType);

				i++;
			}

			level = Integer.parseInt(mainNode.selectSingleNode("Level").getText());
			exp = Integer.parseInt(mainNode.selectSingleNode("Exp").getText());
			dexLevel1 = Integer.parseInt(mainNode.selectSingleNode("DexLevel1").getText());
			dexExp1 = Integer.parseInt(mainNode.selectSingleNode("DexExp1").getText());
			dexLevel2 = Integer.parseInt(mainNode.selectSingleNode("DexLevel2").getText());
			dexExp2 = Integer.parseInt(mainNode.selectSingleNode("DexExp2").getText());
			dexLevel3 = Integer.parseInt(mainNode.selectSingleNode("DexLevel3").getText());
			dexExp3 = Integer.parseInt(mainNode.selectSingleNode("DexExp3").getText());
			dexLevel4 = Integer.parseInt(mainNode.selectSingleNode("DexLevel4").getText());
			dexExp4 = Integer.parseInt(mainNode.selectSingleNode("DexExp4").getText());

			grade = Integer.parseInt(mainNode.selectSingleNode("Grade").getText());
			title = Integer.parseInt(mainNode.selectSingleNode("TitleID").getText());
			skillPoint = Integer.parseInt(mainNode.selectSingleNode("SkillPoint").getText());
			taff = Integer.parseInt(mainNode.selectSingleNode("Taff").getText());
			shopPoint = Integer.parseInt(mainNode.selectSingleNode("ShopPoint").getText());

			Node skillsNode = mainNode.selectSingleNode("Skills");

			List<Node> skillNodes = skillsNode.selectNodes("Item");

			for (Node skill : skillNodes) {

				int skillId = skill.numberValueOf("@ID").intValue();

				skills.add(skillId);

			}

			Node skillHotkeysNode = mainNode.selectSingleNode("SkillHotKey");

			List<Node> skillHotkeyNodes = skillHotkeysNode.selectNodes("Item");

			for (Node skillHotkey : skillHotkeyNodes) {

				int weapon = skillHotkey.numberValueOf("@Weapon").intValue();
				int index = skillHotkey.numberValueOf("@Index").intValue();
				int skillId = skillHotkey.numberValueOf("@ID").intValue();

				SkillHotkey hotkey = new SkillHotkey(skillId);

				SkillHotkey[] weaponSkillHK = skillHotkeys.get(weapon);

				weaponSkillHK[index] = hotkey;

			}

			tag = Integer.parseInt(mainNode.selectSingleNode("TagType").getText());
			picket = mainNode.selectSingleNode("TagType").getText().equals("true");

			picketContent = mainNode.selectSingleNode("Contents").getText();
			banned = mainNode.selectSingleNode("Denied").getText().equals("true");
		}

		int playerId = 0;

		try (Connection tempConn = Database.getConnection()) {

			try (PreparedStatement insertAccPs = tempConn.prepareStatement(
					"INSERT INTO accounts (username, password, HasChar, lang, permissiongroup, banned) VALUES (?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS)) {

				insertAccPs.setString(1, accountName);
				insertAccPs.setString(2, password);
				insertAccPs.setBoolean(3, hasChar);
				insertAccPs.setString(4, data.getDefaultLanguage());
				insertAccPs.setString(5, authType);
				insertAccPs.setBoolean(6, banned);

				insertAccPs.executeUpdate();

				ResultSet generatedKeys = insertAccPs.getGeneratedKeys();
				if (generatedKeys.next()) {
					playerId = generatedKeys.getInt(1);

				}

				generatedKeys.close();

			}

			if (hasChar) {

				try (PreparedStatement insertPlayerPs = tempConn.prepareStatement(
						"INSERT INTO players (id, charname, phone, gender, school, blood, grade, level, hp, dexlevel1, dexExp1, dexlevel2, dexExp2, dexlevel3, dexExp3, dexlevel4, dexExp4, face, hair, skin, month, day, field, x, y, respawnfield, respawnx, respawny, xp, title, taff, shoppoint, skillpoints, colortag, picket, picketcontent, headwear, upperwear, backwear, handwear, lowerwear, footwear) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ?)")) {

					insertPlayerPs.setInt(1, playerId);
					insertPlayerPs.setString(2, charName);
					insertPlayerPs.setInt(3, phone);
					insertPlayerPs.setInt(4, gender);
					insertPlayerPs.setInt(5, school);
					insertPlayerPs.setInt(6, blood);
					insertPlayerPs.setInt(7, grade);
					insertPlayerPs.setInt(8, level);
					insertPlayerPs.setInt(9, 28);
					insertPlayerPs.setInt(10, dexLevel1);
					insertPlayerPs.setInt(11, dexExp1);
					insertPlayerPs.setInt(12, dexLevel2);
					insertPlayerPs.setInt(13, dexExp2);
					insertPlayerPs.setInt(14, dexLevel3);
					insertPlayerPs.setInt(15, dexExp3);
					insertPlayerPs.setInt(16, dexLevel4);
					insertPlayerPs.setInt(17, dexExp4);
					insertPlayerPs.setInt(18, face);
					insertPlayerPs.setInt(19, hair);
					insertPlayerPs.setInt(20, skin);
					insertPlayerPs.setInt(21, birthMonth);
					insertPlayerPs.setInt(22, birthDay);
					insertPlayerPs.setInt(23, fieldId);
					insertPlayerPs.setInt(24, x);
					insertPlayerPs.setInt(25, y);
					insertPlayerPs.setInt(26, saveFieldId);
					insertPlayerPs.setInt(27, saveX);
					insertPlayerPs.setInt(28, saveY);
					insertPlayerPs.setInt(29, exp);
					insertPlayerPs.setInt(30, title);
					insertPlayerPs.setInt(31, taff);
					insertPlayerPs.setInt(32, shopPoint);
					insertPlayerPs.setInt(33, skillPoint);
					insertPlayerPs.setInt(34, tag);
					insertPlayerPs.setBoolean(35, picket);
					insertPlayerPs.setString(36, picketContent);
					insertPlayerPs.setInt(37, headwear);
					insertPlayerPs.setInt(38, upperwear);
					insertPlayerPs.setInt(39, backwear);
					insertPlayerPs.setInt(40, handwear);
					insertPlayerPs.setInt(41, lowerwear);
					insertPlayerPs.setInt(42, footwear);
					insertPlayerPs.executeUpdate();

				}

				try (PreparedStatement ps = conn.prepareStatement(
						"INSERT INTO  player_variables (id, varname, value) VALUES (?,?,?) ON DUPLICATE KEY UPDATE value = VALUES(value)")) {

					for (String varname : variables.keySet()) {

						ps.setInt(1, playerId);
						ps.setString(2, varname);
						ps.setString(3, variables.get(varname));

						ps.addBatch();

					}

					ps.executeBatch();

				} catch (SQLException e) {
					e.printStackTrace();
				}

				int newestPieceId = 0;

				try (PreparedStatement upsertPlayerBeItemPs = conn.prepareStatement(
						"INSERT INTO player_beitems (slot1, slot2, slot3, slot4, slot5, ownerid, itemid, pieceid) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE slot1 = VALUES(slot1), slot2 = VALUES(slot2), slot3 = VALUES(slot3), slot4 = VALUES(slot4), slot5 = VALUES(slot5)",
						Statement.RETURN_GENERATED_KEYS);) {

					for (BeItem beitem : beItems.values()) {
						upsertPlayerBeItemPs.setInt(1, beitem.getReinforceSlotID(1));
						upsertPlayerBeItemPs.setInt(2, beitem.getReinforceSlotID(2));
						upsertPlayerBeItemPs.setInt(3, beitem.getReinforceSlotID(3));
						upsertPlayerBeItemPs.setInt(4, beitem.getReinforceSlotID(4));
						upsertPlayerBeItemPs.setInt(5, beitem.getReinforceSlotID(5));

						upsertPlayerBeItemPs.setInt(6, playerId);
						upsertPlayerBeItemPs.setInt(7, beitem.id);
						upsertPlayerBeItemPs.setInt(8, beitem.pieceId);

						upsertPlayerBeItemPs.executeUpdate();

						if (beitem.pieceId == 0) {

							ResultSet generatedKeys = upsertPlayerBeItemPs.getGeneratedKeys();
							if (generatedKeys.next()) {
								newestPieceId = generatedKeys.getInt(1);

							}

							generatedKeys.close();

						}

					}

				} catch (SQLException e) {
					e.printStackTrace();
				}

				try (PreparedStatement insertLockerBeItemPs = conn.prepareStatement(
						"INSERT INTO locker_beitems (itemid, slot1, slot2, slot3, slot4, slot5, pieceid, lockerid, ownerid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE lockerid = VALUES(lockerid)");
						PreparedStatement coips = conn.prepareStatement(
								"INSERT INTO locker_coitems (itemid, count, ownerid, lockerid) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE lockerid = VALUES(lockerid), count = IF(count <> VALUES(count), VALUES(count), count)");
						PreparedStatement enips = conn.prepareStatement(
								"INSERT INTO locker_enitems (itemid, count, ownerid, lockerid) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE lockerid = VALUES(lockerid), count = IF(count <> VALUES(count), VALUES(count), count)");) {

					for (Locker locker : lockerMgr.lockerSlots) {

						for (Iterator<BeItem> iterator = locker.beItems.values().iterator(); iterator.hasNext();) {
							BeItem beitem = iterator.next();

							if (beitem.isInLocker) {
								newestPieceId++;
								insertLockerBeItemPs.setInt(1, beitem.id);

								insertLockerBeItemPs.setInt(2, beitem.getReinforceSlotID(1));
								insertLockerBeItemPs.setInt(3, beitem.getReinforceSlotID(2));
								insertLockerBeItemPs.setInt(4, beitem.getReinforceSlotID(3));
								insertLockerBeItemPs.setInt(5, beitem.getReinforceSlotID(4));
								insertLockerBeItemPs.setInt(6, beitem.getReinforceSlotID(5));

								insertLockerBeItemPs.setInt(7, newestPieceId);
								insertLockerBeItemPs.setInt(8, locker.lockerId);
								insertLockerBeItemPs.setInt(9, playerId);

								insertLockerBeItemPs.addBatch();

							}
						}

						for (Iterator<CoItem> iterator = locker.coitems.values().iterator(); iterator.hasNext();) {
							CoItem coitem = iterator.next();

							coips.setInt(1, coitem.id);
							coips.setInt(2, coitem.count);
							coips.setInt(3, playerId);
							coips.setInt(4, locker.lockerId);
							coips.addBatch();

						}

						for (Iterator<EnItem> iterator = locker.enitems.values().iterator(); iterator.hasNext();) {
							EnItem enitem = iterator.next();

							enips.setInt(1, enitem.id);
							enips.setInt(2, enitem.count);
							enips.setInt(3, playerId);
							enips.setInt(4, locker.lockerId);
							enips.addBatch();

						}

						insertLockerBeItemPs.executeBatch();
						coips.executeBatch();
						enips.executeBatch();

					}

				} catch (SQLException e) {
					e.printStackTrace();
				}

				try (PreparedStatement insertCoPs = conn.prepareStatement(
						"INSERT INTO player_coitems (itemid, count, ownerid) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE count = IF(count <> VALUES(count), VALUES(count), count)");
						PreparedStatement insertEnPs = conn.prepareStatement(
								"INSERT INTO player_enitems (itemid, count, ownerid) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE count = IF(count <> VALUES(count), VALUES(count), count)");

				) {

					for (Iterator<CoItem> iterator = coItems.values().iterator(); iterator.hasNext();) {
						CoItem coitem = iterator.next();
						insertCoPs.setInt(1, coitem.id);
						insertCoPs.setInt(2, coitem.count);
						insertCoPs.setInt(3, playerId);
						insertCoPs.addBatch();

					}

					insertCoPs.executeBatch();

					for (Iterator<EnItem> iterator = enItems.values().iterator(); iterator.hasNext();) {
						EnItem enitem = iterator.next();

						insertEnPs.setInt(1, enitem.id);
						insertEnPs.setInt(2, enitem.count);
						insertEnPs.setInt(3, playerId);
						insertEnPs.addBatch();

					}
					insertEnPs.executeBatch();

				} catch (SQLException e) {
					e.printStackTrace();
				}

				try (PreparedStatement insertHotkeysPs = conn.prepareStatement(
						"INSERT INTO player_itemhotkeys (playerid, hotkey_index, itemtype, itemid) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE itemtype = VALUES(itemtype), itemid = VALUES(itemid)");) {

					for (int i = 0; i < 10; i++) {
						int itemid = 0;

						ItemHotkey hotkey = itemHotkeys[i];

						insertHotkeysPs.setInt(1, playerId);
						insertHotkeysPs.setInt(2, i);

						if (hotkey != null) {

							if (hotkey.itemId != 0) {

								switch (hotkey.itemType) {
								case 2:
									BeItem item = beItems.get(hotkey.itemId);
									if (item != null) {
										itemid = item.slot;
									}
									break;
								case 3:
									CoItem coitem = coItems.get(hotkey.itemId);
									if (coitem != null) {
										itemid = coitem.id;
									}
									break;
								}

								insertHotkeysPs.setInt(3, hotkey.itemType);
								insertHotkeysPs.setInt(4, itemid);
								insertHotkeysPs.addBatch();

							}

						}
					}

					insertHotkeysPs.executeBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}

				String insertSkills = "INSERT INTO player_skills (playerid, skillid) VALUES (?, ?) ON DUPLICATE KEY UPDATE skillid = skillid";

				try (PreparedStatement insertSkillsPs = conn.prepareStatement(insertSkills)) {

					for (int skill : skills) {
						insertSkillsPs.setInt(1, playerId);
						insertSkillsPs.setInt(2, skill);
						insertSkillsPs.addBatch();

					}

					insertSkillsPs.executeBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}

				try (PreparedStatement skillHotkeysInsert = conn.prepareStatement(
						"INSERT INTO player_skillhotkeys (playerid, weapontype, hotkey_index, skillid) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE skillid= VALUES(skillid)")) {

					for (int i = 1; i <= 4; i++) {
						SkillHotkey[] weaponSkillHotkeys = skillHotkeys.get(i);

						for (int j = 0; j < 9; j++) {

							SkillHotkey hotkey = weaponSkillHotkeys[j];
							if (hotkey != null) {

								if (hotkey.skillId != 0) {

									skillHotkeysInsert.setInt(1, playerId);
									skillHotkeysInsert.setInt(2, i);

									skillHotkeysInsert.setInt(3, j);
									skillHotkeysInsert.setInt(4, hotkey.getSkillId());
									skillHotkeysInsert.addBatch();

								}

							}

						}

					}

					skillHotkeysInsert.executeBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}

				String insertFieldMemoSql = "INSERT INTO player_fieldmemos(playerid, idx, fieldid, x, y) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE idx = idx";

				try (PreparedStatement insertFieldMemoPs = conn.prepareStatement(insertFieldMemoSql)) {

					for (FieldMemoEntry entry : fieldMemos) {

						insertFieldMemoPs.setInt(1, playerId);
						insertFieldMemoPs.setInt(2, entry.index);
						insertFieldMemoPs.setInt(3, entry.getFieldId());
						insertFieldMemoPs.setInt(4, entry.getX());
						insertFieldMemoPs.setInt(5, entry.getY());
						insertFieldMemoPs.addBatch();

					}

					insertFieldMemoPs.executeBatch();

				} catch (SQLException e) {
					e.printStackTrace();
				}

			}

		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	private static void processLocker(Node lockerNode, int lockerId) {

		Node lockerBeItemsNode = lockerNode.selectSingleNode("BeItems");

		List<Node> lockerBeItemNodes = lockerBeItemsNode.selectNodes("Item");
		int i = 1;
		for (Node beItem : lockerBeItemNodes) {

			int id = beItem.numberValueOf("@Type").intValue();
			int slot = beItem.numberValueOf("@Slot0").intValue();
			int slot2 = beItem.numberValueOf("@Slot1").intValue();
			int slot3 = beItem.numberValueOf("@Slot2").intValue();
			int slot4 = beItem.numberValueOf("@Slot3").intValue();
			int slot5 = beItem.numberValueOf("@Slot4").intValue();

			BeItem item = new BeItem(id);
			item.setSlot(i);
			item.setSlot1(new CoItem(slot));
			item.setSlot2(new CoItem(slot2));
			item.setSlot3(new CoItem(slot3));
			item.setSlot4(new CoItem(slot4));
			item.setSlot5(new CoItem(slot5));

			lockerMgr.addBeItem(lockerId, item);
			i++;
		}

		Node lockerCoItemsNode = lockerNode.selectSingleNode("CoItems");

		List<Node> lockerCoItemNodes = lockerCoItemsNode.selectNodes("Item");
		for (Node coItem : lockerCoItemNodes) {

			int id = coItem.numberValueOf("@Type").intValue();
			int count = coItem.numberValueOf("@Count").intValue();

			lockerMgr.addCoItem(lockerId, new CoItem(id, count));

		}

		Node lockerEnItemsNode = lockerNode.selectSingleNode("EnItems");

		List<Node> lockerEnItemNodes = lockerEnItemsNode.selectNodes("Item");
		for (Node enItem : lockerEnItemNodes) {

			int id = enItem.numberValueOf("@Type").intValue();
			int count = enItem.numberValueOf("@Count").intValue();

			lockerMgr.addEnItem(lockerId, new EnItem(id, count));

		}

	}

	private static void createTables(Connection conn) throws SQLException {

		try (Statement stmt = conn.createStatement()) {

			String createAccounts = "CREATE TABLE IF NOT EXISTS accounts (id INT UNIQUE AUTO_INCREMENT NOT NULL, username VARCHAR(64) UNIQUE NOT NULL, password VARCHAR(255) NOT NULL, email VARCHAR(255), haschar BOOLEAN NOT NULL default false, lang VARCHAR(5), created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, permissiongroup VARCHAR(32), muted BOOLEAN NOT NULL DEFAULT FALSE, banned BOOLEAN NOT NULL DEFAULT FALSE, mutereason TEXT DEFAULT NULL, banreason TEXT DEFAULT NULL, muteduntil TIMESTAMP, banneduntil TIMESTAMP, verified BOOLEAN, PRIMARY KEY (`id`), vercode VARCHAR(32), INDEX idx_id (id), INDEX idx_username (username) ) ENGINE=InnoDB";
			stmt.executeUpdate(createAccounts);
			String createPlayers = "CREATE TABLE IF NOT EXISTS players (id INT NOT NULL AUTO_INCREMENT, charname TEXT CHARACTER SET utf16, phone INT, gender INT, school INT, blood INT, face INT, hair INT, skin INT, month INT, day INT, level INT, grade INT, xp INT, dexLevel1 INT, dexExp1 INT, dexLevel2 INT, dexExp2 INT, dexLevel3 INT, dexExp3 INT, dexLevel4 INT, dexExp4 INT, TAFF INT, shoppoint INT, HP INT, field INT, x INT, y INT, headwear INT NOT NULL DEFAULT '0', upperwear INT NOT NULL DEFAULT '0', handwear INT NOT NULL DEFAULT '0', backwear INT NOT NULL DEFAULT '0', lowerwear INT NOT NULL DEFAULT '0', footwear INT NOT NULL DEFAULT '0', title INT NOT NULL DEFAULT 0, skillpoints INT NOT NULL DEFAULT 0, respawnfield INT NOT NULL DEFAULT 1, respawnx INT NOT NULL DEFAULT 80, respawny INT NOT NULL DEFAULT 70, colortag INT NOT NULL DEFAULT 1, picket BOOLEAN NOT NULL DEFAULT FALSE, picketcontent TEXT CHARACTER SET utf16, status INT NOT NULL DEFAULT 0, PRIMARY KEY (`id`), FOREIGN KEY (`id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE,INDEX idx_id (id)) ENGINE=InnoDB";
			stmt.executeUpdate(createPlayers);
			String createVars = "CREATE TABLE IF NOT EXISTS `player_variables` (id INT NOT NULL, varname VARCHAR(64), value TEXT,"
					+ " FOREIGN KEY (`id`) REFERENCES `players` (`id`) ON DELETE CASCADE, UNIQUE KEY(id, varname), INDEX idx_id (id)) ENGINE=InnoDB";
			stmt.executeUpdate(createVars);
			String createBeItems = "CREATE TABLE IF NOT EXISTS player_beitems (itemid INT NOT NULL DEFAULT '0', slot1 INT NOT NULL DEFAULT '0', slot2 INT NOT NULL DEFAULT '0', slot3 INT NOT NULL DEFAULT '0', slot4 INT NOT NULL DEFAULT '0', slot5 INT NOT NULL DEFAULT '0',  pieceid INT AUTO_INCREMENT, ownerid INT NOT NULL DEFAULT '0', date_updated TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY (pieceid), FOREIGN KEY (ownerid) REFERENCES players (id) ON DELETE CASCADE, INDEX idx_id (ownerid)) ENGINE=InnoDB;";
			stmt.executeUpdate(createBeItems);
			String createCoItems = "CREATE TABLE IF NOT EXISTS `player_coitems` (`itemid` INT NOT NULL DEFAULT '0', `count` INT NOT NULL, `ownerid` int NOT NULL DEFAULT '0', date_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (itemid, ownerid), FOREIGN KEY (`ownerid`) REFERENCES `players` (`id`) ON DELETE CASCADE, INDEX idx_id (ownerid)) ENGINE=InnoDB";
			stmt.executeUpdate(
					"CREATE TABLE IF NOT EXISTS `player_enitems` (`itemid` INT NOT NULL DEFAULT '0', `count` INT NOT NULL, `ownerid` int NOT NULL DEFAULT '0', date_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (itemid, ownerid), FOREIGN KEY (`ownerid`) REFERENCES `players` (`id`) ON DELETE CASCADE, INDEX idx_id (ownerid)) ENGINE=InnoDB");

			stmt.executeUpdate(createCoItems);

			String createByulBeItems = "CREATE TABLE IF NOT EXISTS player_byulbeitems (itemid INT NOT NULL DEFAULT '0', slot1 INT NOT NULL DEFAULT '0', slot2 INT NOT NULL DEFAULT '0', slot3 INT NOT NULL DEFAULT '0', slot4 INT NOT NULL DEFAULT '0', slot5 INT NOT NULL DEFAULT '0',  pieceid INT AUTO_INCREMENT, date_expire TIMESTAMP(6), ownerid INT NOT NULL DEFAULT '0', date_updated TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY (pieceid), FOREIGN KEY (ownerid) REFERENCES players (id) ON DELETE CASCADE, INDEX idx_id (ownerid)) ENGINE=InnoDB;";
			stmt.executeUpdate(createByulBeItems);

			String createItemHotkeys = "CREATE TABLE IF NOT EXISTS `player_itemhotkeys` (playerid INT, hotkey_index INT, itemtype INT, itemid INT, PRIMARY KEY(playerid, hotkey_index), FOREIGN KEY (playerid) REFERENCES players (id) ON DELETE CASCADE, INDEX idx_id (playerid)) ENGINE=InnoDB;";
			stmt.executeUpdate(createItemHotkeys);

			String createSkillHotkeys = "CREATE TABLE IF NOT EXISTS `player_skillhotkeys` (playerid INT,  hotkey_index INT, skillid INT, weapontype INT, PRIMARY KEY (playerid, hotkey_index, weapontype), FOREIGN KEY (playerid) REFERENCES players (id) ON DELETE CASCADE, INDEX idx_id (playerid)) ENGINE=InnoDB;";
			stmt.executeUpdate(createSkillHotkeys);

			String createPlayerSkills = "CREATE TABLE IF NOT EXISTS `player_skills` (playerid INT, skillid INT UNIQUE, skillevel INT, FOREIGN KEY (playerid) REFERENCES players (id) ON DELETE CASCADE, INDEX idx_id (playerid)) ENGINE=InnoDB";

			stmt.executeUpdate(createPlayerSkills);

			String createLockerBeItems = "CREATE TABLE IF NOT EXISTS locker_beitems (itemid INT NOT NULL DEFAULT '0', slot1 INT NOT NULL DEFAULT '0', slot2 INT NOT NULL DEFAULT '0', slot3 INT NOT NULL DEFAULT '0', slot4 INT NOT NULL DEFAULT '0', slot5 INT NOT NULL DEFAULT '0',  pieceid INT AUTO_INCREMENT, lockerid INT NOT NULL, ownerid INT NOT NULL DEFAULT '0', date_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (pieceid), FOREIGN KEY (ownerid) REFERENCES players (id) ON DELETE CASCADE, INDEX idx_id (ownerid)) ENGINE=InnoDB;";

			stmt.executeUpdate(createLockerBeItems);

			String createLockerCoItems = "CREATE TABLE IF NOT EXISTS `locker_coitems` (`itemid` INT NOT NULL DEFAULT '0', `count` INT NOT NULL, `ownerid` int NOT NULL DEFAULT '0', lockerid INT NOT NULL, date_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (itemid, ownerid), FOREIGN KEY (`ownerid`) REFERENCES `players` (`id`) ON DELETE CASCADE, INDEX idx_id (ownerid)) ENGINE=InnoDB";
			stmt.executeUpdate(createLockerCoItems);

			String createLockerEnItems = "CREATE TABLE IF NOT EXISTS `locker_enitems` (`itemid` INT NOT NULL DEFAULT '0', `count` INT NOT NULL, `ownerid` int NOT NULL DEFAULT '0', lockerid INT NOT NULL, date_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (itemid, ownerid), FOREIGN KEY (`ownerid`) REFERENCES `players` (`id`) ON DELETE CASCADE, INDEX idx_id (ownerid)) ENGINE=InnoDB";
			stmt.executeUpdate(createLockerEnItems);

			String createFieldMemos = "CREATE TABLE IF NOT EXISTS player_fieldmemos (playerid INT NOT NULL, idx INT NOT NULL PRIMARY KEY, fieldid INT NOT NULL, x INT NOT NULL, y INT NOT NULL, FOREIGN KEY (playerid) REFERENCES players(id) ON DELETE CASCADE, INDEX idx_id (playerid)) ENGINE=InnoDB;";

			stmt.executeUpdate(createFieldMemos);

		}

	}

}
