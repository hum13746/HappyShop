package ci553.happyshop.client;
import ci553.happyshop.client.manager.ManagerView;
import ci553.happyshop.LoginDialog;
import ci553.happyshop.ManagerLoginDialog;
import ci553.happyshop.client.customer.*;
import ci553.happyshop.client.emergency.EmergencyExit;
import ci553.happyshop.client.manager.ManagerClient;
import ci553.happyshop.client.orderTracker.OrderTracker;
import ci553.happyshop.client.picker.PickerController;
import ci553.happyshop.client.picker.PickerModel;
import ci553.happyshop.client.picker.PickerView;
import ci553.happyshop.client.warehouse.*;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.storageAccess.DatabaseRWFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Executable Main class – now asks USER or MANAGER before launching clients.
 * @version 1.1
 * @author  Shine Shan University of Brighton
 */
public class Main extends Application {
    private static Stage stage;   // keep the same Stage alive
    private Stage primaryStage;          // keep one Stage alive
    private static DatabaseRW sharedDB;

    public static void main(String[] args) {
        launch(args);
    }

    /* ----------------------------------------------------------
     *  JavaFX entry – tiny login then launch chosen role
     * ---------------------------------------------------------- */
    @Override
    public void start(Stage primaryStage) throws IOException {
        stage = primaryStage;
        primaryStage.setTitle("HappyShop");
        sharedDB = DatabaseRWFactory.createDatabaseRW();
        OrderHub.getOrderHub().initializeOrderMap();
        showRolePicker();
    }

    /* ----------------------------------------------------------
     *  ROLE-SPECIFIC LAUNCHERS  (restore these)
     * ---------------------------------------------------------- */
    private void startCustomerClient() {
        CustomerView cusView = new CustomerView();
        CustomerController cusController = new CustomerController();
        CustomerModel cusModel = new CustomerModel();
        DatabaseRW databaseRW = DatabaseRWFactory.createDatabaseRW();

        cusView.cusController = cusController;
        cusController.cusModel = cusModel;
        cusModel.cusView = cusView;
        cusModel.databaseRW = sharedDB;
        cusView.start(new Stage());
    }

    private void startManagerClient() {
        new ManagerClient().start(new Stage());
    }
    /* inject bottom-right switch button */

    /* --------------  ORIGINAL HELPER METHODS  ---------------- */
    private void startPickerClient() {
        PickerModel pickerModel = new PickerModel();
        PickerView pickerView = new PickerView();
        PickerController pickerController = new PickerController();
        pickerView.pickerController = pickerController;
        pickerController.pickerModel = pickerModel;
        pickerModel.pickerView = pickerView;
        pickerModel.registerWithOrderHub();
        pickerView.start(new Stage());
    }

    private void startOrderTracker() {
        OrderTracker orderTracker = new OrderTracker();
        orderTracker.registerWithOrderHub();
    }

    private void initializeOrderMap() {
        OrderHub.getOrderHub().initializeOrderMap();
    }

    private static void openCustomer() {
        CustomerView cusView = new CustomerView();
        CustomerController cusController = new CustomerController();
        CustomerModel cusModel = new CustomerModel();

        cusView.cusController = cusController;
        cusController.cusModel = cusModel;
        cusModel.cusView = cusView;
        cusModel.databaseRW = sharedDB;   // shared connection

        cusView.start(stage);
        addBackButton("Switch to Manager", () -> showRolePicker());
    }

    /* ----------------------------------------------------------
     *  MANAGER  (keeps original wiring, uses shared DB)
     * ---------------------------------------------------------- */
    private static void openManager() {
        ManagerView mv = new ManagerView();
        mv.setSharedDatabase(sharedDB);
        mv.start(stage);
        addBackButton("Switch to Customer", () -> showRolePicker());
    }

    /* ----------------------------------------------------------
     *  BACK-TO-LOGIN BUTTON  (adds to current scene)
     * ---------------------------------------------------------- */
    private static void addBackButton(String text, Runnable action) {
        var scene = stage.getScene();
        if (scene == null) return;

        var btn = new javafx.scene.control.Button(text);
        btn.getStyleClass().add("login-btn");
        btn.setOnAction(e -> action.run());

        var bar = new javafx.scene.layout.HBox(btn);
        bar.setAlignment(javafx.geometry.Pos.BOTTOM_RIGHT);
        bar.setPadding(new javafx.geometry.Insets(10));

        var root = scene.getRoot();

        // If root is already a VBox, just add the button bar
        if (root instanceof javafx.scene.layout.VBox vb) {
            vb.getChildren().add(bar);
            return;
        }

        // Otherwise wrap the existing root inside a VBox
        var wrapper = new javafx.scene.layout.VBox();
        wrapper.getChildren().addAll(root, bar);
        scene.setRoot(wrapper);
    }




    private void startWarehouseClient() {
        WarehouseView view = new WarehouseView();
        WarehouseController controller = new WarehouseController();
        WarehouseModel model = new WarehouseModel();
        DatabaseRW databaseRW = DatabaseRWFactory.createDatabaseRW();

        view.controller = controller;
        controller.model = model;
        model.view = view;
        model.databaseRW = databaseRW;
        view.start(new Stage());

        HistoryWindow historyWindow = new HistoryWindow();
        AlertSimulator alertSimulator = new AlertSimulator();
        model.historyWindow = historyWindow;
        model.alertSimulator = alertSimulator;
        historyWindow.warehouseView = view;
        alertSimulator.warehouseView = view;
    }
    public static void showRolePicker() {
        stage.hide();
        LoginDialog.Role role = LoginDialog.show();
        if (role == null) Platform.exit();
        switch (role) {
            case CUSTOMER -> openCustomer();
            case MANAGER  -> openManager();
        }
    }

    private void startEmergencyExit() {
        EmergencyExit.getEmergencyExit();
    }
}
