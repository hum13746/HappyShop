package ci553.happyshop;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class LoginDialog {

    public enum Role { CUSTOMER, MANAGER }

    public static Role show() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("HappyShop – Select Role");

        /* ---------- title ---------- */
        Label prompt = new Label("Log in as:");
        prompt.getStyleClass().add("login-title");

        /* ---------- bigger buttons ---------- */
        Button btnCustomer = new Button("Customer");
        Button btnManager  = new Button("Manager");
        btnCustomer.getStyleClass().add("login-btn");
        btnManager.getStyleClass().add("login-btn");

        final Role[] result = { null };
        btnCustomer.setOnAction(e -> { result[0] = Role.CUSTOMER; stage.close(); });
        btnManager.setOnAction(e  -> { result[0] = Role.MANAGER;  stage.close(); });

        /* ---------- white card ---------- */
        VBox card = new VBox(25, prompt, btnCustomer, btnManager);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("login-card");

        /* ---------- gradient background ---------- */
        VBox root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("login-root");

        Scene scene = new Scene(root, 420, 280);
        scene.getStylesheets().add(new File("style.css").toURI().toString());
        stage.setScene(scene);
        stage.showAndWait();
        return result[0]; // null if user closed window
    }
}
