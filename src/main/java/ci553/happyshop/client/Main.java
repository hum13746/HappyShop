package ci553.happyshop.client;
import ci553.happyshop.LoginDialog;
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
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Executable Main class – now asks USER or MANAGER before launching clients.
 * @version 1.1
 * @author  Shine Shan University of Brighton
 */
public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    /* ----------------------------------------------------------
     *  JavaFX entry – tiny login then launch chosen role
     * ---------------------------------------------------------- */
    @Override
    public void start(Stage primaryStage) throws IOException {
        LoginDialog.Role role = LoginDialog.show();
        if (role == null) {           // user closed the box
            Platform.exit();
            return;
        }

        switch (role) {
            case CUSTOMER -> startCustomerClient();
            case MANAGER  -> startManagerClient();   // FIXED capital C
        }
    }

    /* ----------------------------------------------------------
     *  ROLE-SPECIFIC LAUNCHERS
     * ---------------------------------------------------------- */
    private void startCustomerClient() {
        CustomerView cusView = new CustomerView();
        CustomerController cusController = new CustomerController();
        CustomerModel cusModel = new CustomerModel();
        DatabaseRW databaseRW = DatabaseRWFactory.createDatabaseRW();

        cusView.cusController = cusController;
        cusController.cusModel = cusModel;
        cusModel.cusView = cusView;
        cusModel.databaseRW = databaseRW;
        cusView.start(new Stage());
    }

    private void startManagerClient() {              // FIXED capital C
        new ManagerClient().start(new Stage());
    }

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

    private void startEmergencyExit() {
        EmergencyExit.getEmergencyExit();
    }
}




