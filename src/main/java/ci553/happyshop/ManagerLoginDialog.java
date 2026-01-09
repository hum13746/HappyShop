package ci553.happyshop;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class ManagerLoginDialog {

    public static boolean show() {          // true = authenticated, false = cancelled
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Manager Portal – HappyShop");

        Label title = new Label("Manager Log In");
        title.getStyleClass().add("login-title");

        Button btnEnter = new Button("Enter Portal");
        btnEnter.getStyleClass().add("login-btn");

        Button btnBack  = new Button("Back");
        btnBack.getStyleClass().add("login-btn");

        final boolean[] ok = {false};

        btnEnter.setOnAction(e -> { ok[0] = true;  stage.close(); });
        btnBack.setOnAction(e  -> { ok[0] = false; stage.close(); });

        VBox card = new VBox(25, title, btnEnter, btnBack);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("login-card");

        VBox root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("login-root");

        Scene scene = new Scene(root, 420, 280);
        scene.getStylesheets().add(new File("style.css").toURI().toString());
        stage.setScene(scene);
        stage.showAndWait();
        return ok[0];
    }
}
