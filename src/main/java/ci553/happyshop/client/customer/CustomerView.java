package ci553.happyshop.client.customer;
import java.io.File;
import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WinPosManager;
import ci553.happyshop.utility.WindowBounds;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Duration;
import ci553.happyshop.storageAccess.DerbyRW;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import javafx.scene.layout.HBox;
import javafx.application.Platform;
import ci553.happyshop.LoginDialog;
/**
 * Four separate pop-up bricks: Search, Trolley, Orders, Receipt
 */
public class CustomerView {

    public CustomerController cusController;

    private final int WIDTH  = UIStyle.customerWinWidth;
    private final int HEIGHT = UIStyle.customerWinHeight;
    private final int COLUMN_WIDTH = WIDTH / 2 - 10;

    /*  four pop-up windows */
    private Stage searchStage, trolleyStage, ordersStage, receiptStage;

    /*  left-search controls */
    TextField tfId;
    TextField tfName;
    private ImageView ivProduct;
    private Label lbProductInfo;

    /*  trolley controls */
    private VBox vbTrolleyRows;
    private Label lbTrolleyTotal;

    /*  orders table */
    private TableView<OrderRow> orderTable = new TableView<>();

    /*  receipt control */
    private TextArea taReceipt;

    private Stage viewWindow;   // owner

    /* =================================================================== */
    public void start(Stage ownerWindow) {
        viewWindow = ownerWindow;

        /*  1. create controller  */
        cusController = new CustomerController();
        cusController.cusModel = new CustomerModel();
        cusController.cusModel.databaseRW = new DerbyRW();
        cusController.setView(this);                    // inject view
        cusController.cusModel.cusView = this;          // <<<  MISSING LINE

        /*  3. now build bricks  */
        Node search   = createSearchPage();
        Node trolley  = createTrolleyTab();
        Node orders   = createOrdersTab();
        Node receipt  = createReceiptTab();

        double w = COLUMN_WIDTH * 1.5;
        double h = HEIGHT   * 0.80;

        searchStage   = popUpBrick(ownerWindow, "Search",  search,  0,   0,   w, h);
        trolleyStage  = popUpBrick(ownerWindow, "Trolley", trolley, w+20,0,   w, h);
        ordersStage   = popUpBrick(ownerWindow, "Orders",  orders,  0,   h+40,w, h);
        receiptStage  = popUpBrick(ownerWindow, "Receipt", receipt, w+20,h+40,w, h);


    }

    /* ------------------------------------------------------------------- */
    private Stage popUpBrick(Stage owner, String title, Node content, double x, double y, double w, double h) {
        Stage stage = new Stage();
        stage.setTitle(title);
        Scene scene = new Scene((Parent) content, w, h);

        /*  load CSS as plain file  */
        File cssFile = new File("style.css");   // sits beside pom.xml
        if (!cssFile.exists()) {
            throw new RuntimeException("CSS file not found at " + cssFile.getAbsolutePath());
        }
        scene.getStylesheets().add(cssFile.toURI().toString());

        stage.setScene(scene);
        stage.setX(x);
        stage.setY(y);
        stage.initOwner(owner);
        stage.show();
        return stage;
    }

    /* ------------------------------------------------------------------- */
    private VBox createSearchPage() {
        Label laPageTitle = new Label("Search by Product ID/Name");
        laPageTitle.getStyleClass().add("title");

        Label laId = new Label("ID:      ");
        laId.setStyle(UIStyle.labelStyle);
        tfId = new TextField(); tfId.setPromptText("eg. 0001");
        tfId.getStyleClass().add("text-field");
        HBox hbId = new HBox(10, laId, tfId);

        Label laName = new Label("Name:");
        laName.setStyle(UIStyle.labelStyle);
        tfName = new TextField(); tfName.setPromptText("implement it if you want");
        tfName.setStyle(UIStyle.textFiledStyle);
        HBox hbName = new HBox(10, laName, tfName);

        Label laPlaceHolder = new Label(" ".repeat(15));
        Button btnSearch = new Button("Search");
        btnSearch.getStyleClass().add("btn");
        btnSearch.setOnAction(this::buttonClicked);
        Button btnAddToTrolley = new Button("Add to Trolley");
        btnAddToTrolley.getStyleClass().add("btn");
        btnAddToTrolley.setOnAction(this::buttonClicked);
        HBox hbBtns = new HBox(10, laPlaceHolder, btnSearch, btnAddToTrolley);

        ivProduct = new ImageView("imageHolder.jpg");
        ivProduct.setFitHeight(60); ivProduct.setFitWidth(60);
        ivProduct.setPreserveRatio(true); ivProduct.setSmooth(true);

        lbProductInfo = new Label("Thank you for shopping with us.");
        lbProductInfo.setWrapText(true);
        lbProductInfo.setStyle(UIStyle.labelMulLineStyle);
        HBox hbSearchResult = new HBox(5, ivProduct, lbProductInfo);
        hbSearchResult.setAlignment(Pos.CENTER_LEFT);

        VBox vbSearchPage = new VBox(15, laPageTitle, hbId, hbName, hbBtns, hbSearchResult);
        vbSearchPage.getStyleClass().add("card");
        vbSearchPage.setPrefWidth(COLUMN_WIDTH);
        vbSearchPage.setAlignment(Pos.TOP_CENTER);
        vbSearchPage.setStyle("-fx-padding: 15px;");
        return vbSearchPage;
    }

    /* ------------------------------------------------------------------- */
    private Node createTrolleyTab() {
        Label laTitle = new Label("🛒 Trolley");
        laTitle.setStyle(UIStyle.labelTitleStyle);

        vbTrolleyRows = new VBox(5);
        vbTrolleyRows.setStyle("-fx-padding: 5;");

        ScrollPane sp = new ScrollPane(vbTrolleyRows);
        sp.setFitToWidth(true);
        sp.setPrefSize(COLUMN_WIDTH, HEIGHT - 150);

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(this::buttonClicked);
        btnCancel.setStyle(UIStyle.buttonStyle);

        Button btnCheckout = new Button("Check Out");
        btnCheckout.setOnAction(this::buttonClicked);
        btnCheckout.setStyle(UIStyle.buttonStyle);

        HBox hbBtns = new HBox(10, btnCancel, btnCheckout);
        hbBtns.setStyle("-fx-padding: 15px;");
        hbBtns.setAlignment(Pos.CENTER);

        lbTrolleyTotal = new Label("Trolley Total: £0.00");
        lbTrolleyTotal.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 5 0 0 0;");

        VBox vb = new VBox(15, laTitle, sp, lbTrolleyTotal, hbBtns);
        vb.setAlignment(Pos.TOP_CENTER);
        vb.setStyle("-fx-padding: 15px;");

        return vb;
    }

    /* ------------------------------------------------------------------- */
    private Node createOrdersTab() {
        Label laTitle = new Label("Your Orders – Live Status");
        laTitle.setStyle("-fx-font-size: 15; -fx-padding: 0 0 10 0;");

        TableColumn<OrderRow, String> colId = new TableColumn<>("Order ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("orderId"));

        TableColumn<OrderRow, String> colState = new TableColumn<>("State");
        colState.setCellValueFactory(new PropertyValueFactory<>("state"));

        TableColumn<OrderRow, String> colDate = new TableColumn<>("Ordered");
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateTime"));

        TableColumn<OrderRow, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        orderTable.getColumns().setAll(colId, colState, colDate, colTotal);
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refreshOrderTable());

        VBox vb = new VBox(10, laTitle, orderTable, refreshBtn);
        vb.setAlignment(Pos.TOP_CENTER);
        vb.setStyle("-fx-padding: 15px;");
        return vb;
    }


    public void refreshOrderTable() {
        List<Order> all = new ArrayList<>(OrderHub.getOrderHub().getAllOrders().values());
        all.sort(Comparator.comparing(Order::getOrderedDateTime).reversed());
        List<Order> recent = all.stream().limit(4).toList();

        ObservableList<OrderRow> rows = FXCollections.observableArrayList();
        for (Order o : recent) {
            rows.add(new OrderRow(
                    String.valueOf(o.getOrderId()),
                    o.getState().toString(),
                    o.getOrderedDateTime(),
                    String.format("£%.2f", o.getOrderTotal())
            ));
        }
        orderTable.setItems(rows);
    }

    /* ------------------------------------------------------------------- */
    private Node createReceiptTab() {
        Label laTitle = new Label("Receipt");
        laTitle.setStyle(UIStyle.labelTitleStyle);

        taReceipt = new TextArea();
        taReceipt.setEditable(false);
        taReceipt.setPrefSize(COLUMN_WIDTH, HEIGHT - 150);

        /*  Pay button  */
        Button btnPay = new Button("Pay");
        btnPay.setStyle(UIStyle.buttonStyle);
        btnPay.setOnAction(e -> {
            try { cusController.doAction("Pay"); }
            catch (SQLException | IOException ex) { ex.printStackTrace(); }
        });

        Button btnClose = new Button("OK & Close");
        btnClose.setStyle(UIStyle.buttonStyle);
        btnClose.setOnAction(this::buttonClicked);

        Button btnBack = new Button("← Back to Login");
        btnBack.getStyleClass().add("login-btn");
        btnBack.setOnAction(e -> ci553.happyshop.client.Main.showRolePicker());

        /*  button bar  */
        HBox btnBar = new HBox(10, btnPay, btnClose, btnBack);
        btnBar.setAlignment(Pos.CENTER);

        VBox vb = new VBox(15, laTitle, taReceipt, btnBar);
        vb.setAlignment(Pos.TOP_CENTER);
        vb.setStyle("-fx-padding: 15px;");
        return vb;
    }

    /* ------------------------------------------------------------------- */
    private void buttonClicked(ActionEvent event) {
        try {
            Button btn = (Button) event.getSource();
            String action = btn.getText();
            cusController.doAction(action);

            switch (action) {
                case "Add to Trolley" -> refreshTrolleyRows();                     // refresh pop-up

                case "Check Out"      -> refreshOrderTable();
                case "OK & Close"     -> {
                    /*  close all bricks  */
                    searchStage.close();
                    trolleyStage.close();
                    ordersStage.close();
                    receiptStage.close();
                    viewWindow.close();
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    /* ------------------------------------------------------------------- */
    public void refreshTrolleyRows() {
        vbTrolleyRows.getChildren().clear();
        if (cusController == null || cusController.getModel().getTrolley().isEmpty()) {
            vbTrolleyRows.getChildren().add(new Label("Your trolley is empty"));
            lbTrolleyTotal.setText("Trolley Total: £0.00");
            return;
        }
        for (Product p : cusController.getModel().getTrolley()) {
            String line = String.format("%s  %s  £%.2f  (qty: %d)",
                    p.getProductId(), p.getProductDescription(),
                    p.getUnitPrice(), p.getOrderedQuantity());
            Label lb = new Label(line);
            lb.setStyle("-fx-font-size: 13;");

            Button removeBtn = new Button("Remove one");
            removeBtn.setStyle(UIStyle.buttonStyle);
            removeBtn.setOnAction(e -> {
                try {
                    cusController.doAction("Remove one:" + p.getProductId());
                } catch (SQLException | IOException ex) {
                    ex.printStackTrace();
                }
            });

            HBox row = new HBox(10, lb, removeBtn);
            row.setAlignment(Pos.CENTER_LEFT);
            vbTrolleyRows.getChildren().add(row);
        }
        lbTrolleyTotal.setText(String.format("Trolley Total: £%.2f",
                cusController.getModel().getTrolleyTotal()));
    }
    /* ------------------------------------------------------------------- */
    public void update(String imageName, String searchResult, String trolley, String receipt) {
        ivProduct.setImage(new Image(imageName));
        lbProductInfo.setText(searchResult);
        refreshTrolleyRows();          // <--  NEW – fill trolley window
        if (!receipt.isEmpty()) taReceipt.setText(receipt);
    }

    /* ------------------------------------------------------------------- */
    WindowBounds getWindowBounds() {
        return new WindowBounds(viewWindow.getX(), viewWindow.getY(),
                viewWindow.getWidth(), viewWindow.getHeight());
    }
    public void bringReceiptToFront() {
        receiptStage.toFront();
    }
    /* =================================================================== */
    public static class OrderRow {
        private final String orderId, state, dateTime, total;
        public OrderRow(String orderId, String state, String dateTime, String total) {
            this.orderId = orderId; this.state = state; this.dateTime = dateTime; this.total = total;
        }
        public String getOrderId() { return orderId; }
        public String getState() { return state; }
        public String getDateTime() { return dateTime; }
        public String getTotal() { return total; }
    }
    public void closeAllBricks() {
        searchStage.close();
        trolleyStage.close();
        ordersStage.close();
        receiptStage.close();
        viewWindow.close();
    }
    // wire view to controller
    public void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.initOwner(receiptStage);
        alert.showAndWait();
    }
}