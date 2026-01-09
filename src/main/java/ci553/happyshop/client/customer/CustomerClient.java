package ci553.happyshop.client.customer;

import ci553.happyshop.LoginDialog;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.storageAccess.DatabaseRWFactory;
import javafx.application.Application;
import javafx.stage.Stage;

public class CustomerClient extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage ignoredStage) {
        do {
            CustomerView cusView = new CustomerView();
            CustomerController cusController = new CustomerController();
            CustomerModel cusModel = new CustomerModel();
            DatabaseRW databaseRW = DatabaseRWFactory.createDatabaseRW();

            cusView.cusController = cusController;
            cusController.cusModel = cusModel;
            cusModel.cusView = cusView;
            cusModel.databaseRW = databaseRW;

            Stage customerStage = new Stage();
            cusView.start(customerStage); // blocks until all customer windows close
        } while (LoginDialog.show() == LoginDialog.Role.CUSTOMER);
    }
}
