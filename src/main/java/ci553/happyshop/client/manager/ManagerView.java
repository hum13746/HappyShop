package ci553.happyshop.client.manager;
import ci553.happyshop.orderManagement.OrderState;
import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.client.manager.ManagerModel;
import ci553.happyshop.client.manager.ManagerController;
import ci553.happyshop.storageAccess.DatabaseRW;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.Comparator;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import javafx.geometry.Insets;
import java.time.LocalDate;
import java.util.stream.Stream;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import java.io.File;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;

public class ManagerView {
    public ManagerModel getModel(){ return model; }
    private ManagerController controller = new ManagerController();
    private ManagerModel model = new ManagerModel();

    /*  tables  */
    private TableView<OrderRow> orderTable = new TableView<>();
    private TableView<ProductRow> lowStockTable = new TableView<>();

    public void start(Stage window) {
        System.out.println("DEBUG – ManagerView.start entered");
        model.setView(this);
        controller.setModel(model);

        System.out.println("DEBUG – about to call refreshOrders()");
        controller.refreshOrders();
        System.out.println("DEBUG – refreshOrders() finished");

        TabPane tabPane = new TabPane();

        Tab ordersTab = new Tab("Orders");
        ordersTab.setContent(createOrdersTab());
        ordersTab.setClosable(false);

        Tab stockTab = new Tab("Low Stock (≤ 5)");
        stockTab.setContent(createLowStockTab());
        stockTab.setClosable(false);

        Tab allOrdersTab = new Tab("All Orders");
        allOrdersTab.setContent(createAllOrdersTab());
        allOrdersTab.setClosable(false);
        tabPane.getTabs().add(allOrdersTab);

        tabPane.getTabs().addAll(ordersTab, stockTab);
        tabPane.getStyleClass().add("manager-root");
        Scene scene = new Scene(tabPane, 700, 500);
        tabPane.getStyleClass().add("manager-root");
        scene.getStylesheets().add(new File("style.css").toURI().toString());
        window.setScene(scene);
        window.setTitle("HappyShop Manager");
        window.show();

        Button backBtn = new Button("← Back to Login");
        backBtn.getStyleClass().add("manager-btn");
        backBtn.setOnAction(e -> ci553.happyshop.client.Main.showRolePicker());

        HBox bottom = new HBox(backBtn);
        bottom.setAlignment(Pos.BOTTOM_RIGHT);
        bottom.setPadding(new Insets(10));


        model.refreshOrderList();   // load orders
        model.refreshLowStockTable(); // load low-stock
        controller.refreshOrders(); // forces real load
        Collection<Order> recent = model.getRecentOrders();
        System.out.println("DEBUG – loading " + recent.size() + " orders");
        displayOrders(recent);
    }

    private Node createOrdersTab() {
        /* ---------- title row ---------- */
        Label title = new Label("All Orders");
        title.getStyleClass().add("manager-title");

        Button refreshBtn = new Button("Refresh Orders");
        refreshBtn.getStyleClass().add("manager-btn");
        refreshBtn.setOnAction(e -> controller.refreshOrders());

        HBox topBar = new HBox(15, title, refreshBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);

        /* ---------- filter bar ---------- */
        TextField searchField = new TextField();
        searchField.setPromptText("Order ID, customer, item...");
        searchField.getStyleClass().add("text-field");

        DatePicker fromPicker = new DatePicker();
        fromPicker.setPromptText("From");
        DatePicker toPicker   = new DatePicker();
        toPicker.setPromptText("To");

        Button filterBtn = new Button("Filter");
        filterBtn.getStyleClass().add("manager-btn");

        HBox filterBar = new HBox(10, searchField, fromPicker, toPicker, filterBtn);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10, 0, 10, 0));

        /* ---------- table ---------- */
        buildOrderTable();

        /* ---------- root ---------- */
        VBox root = new VBox(10, topBar, filterBar, orderTable);
        root.getStyleClass().add("manager-card");
        root.setPadding(new Insets(15));

        /* ---------- auto-filter while typing ---------- */
        searchField.textProperty().addListener((obs, oldV, newV) ->
                applyFilter(newV, fromPicker.getValue(), toPicker.getValue()));
        fromPicker.valueProperty().addListener((obs, oldV, newV) ->
                applyFilter(searchField.getText(), newV, toPicker.getValue()));
        toPicker.valueProperty().addListener((obs, oldV, newV) ->
                applyFilter(searchField.getText(), fromPicker.getValue(), newV));
        filterBtn.setOnAction(e ->
                applyFilter(searchField.getText(), fromPicker.getValue(), toPicker.getValue()));

        return root;
    }

    /* ----------------------------------------------------------
     *  ALL ORDERS (FULL HISTORY) – read-only, unlimited, searchable
     * ---------------------------------------------------------- */
    private Node createAllOrdersTab() {
        /* ---------- title ---------- */
        Label title = new Label("All Orders (Full History)");
        title.getStyleClass().add("manager-title");

        /* ---------- search & date bar ---------- */
        TextField searchField = new TextField();
        searchField.setPromptText("Order ID, customer, item...");
        searchField.getStyleClass().add("text-field");

        DatePicker fromPicker = new DatePicker();
        fromPicker.setPromptText("From");
        DatePicker toPicker   = new DatePicker();
        toPicker.setPromptText("To");

        HBox filterBar = new HBox(10, searchField, fromPicker, toPicker);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10, 0, 10, 0));

        /* ---------- unlimited table ---------- */
        TableView<OrderRow> allTable = new TableView<>();
        buildAllOrdersTable(allTable);

        /* ---------- initial load ---------- */
        loadAllOrders(allTable, searchField.getText(), fromPicker.getValue(), toPicker.getValue());

        /* ---------- live filtering ---------- */
        searchField.textProperty().addListener((obs, oldV, newV) ->
                loadAllOrders(allTable, newV, fromPicker.getValue(), toPicker.getValue()));
        fromPicker.valueProperty().addListener((obs, oldV, newV) ->
                loadAllOrders(allTable, searchField.getText(), newV, toPicker.getValue()));
        toPicker.valueProperty().addListener((obs, oldV, newV) ->
                loadAllOrders(allTable, searchField.getText(), fromPicker.getValue(), newV));

        /* ---------- root ---------- */
        VBox root = new VBox(10, title, filterBar, allTable);
        root.getStyleClass().add("manager-card");
        root.setPadding(new Insets(15));
        return root;
    }

    /* Builds the SAME columns as the normal orders table but NO action column */
    private void buildAllOrdersTable(TableView<OrderRow> table) {
        TableColumn<OrderRow, String> colId    = new TableColumn<>("Order ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("orderId"));

        TableColumn<OrderRow, String> colDate  = new TableColumn<>("Date & Time");
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateTime"));

        TableColumn<OrderRow, String> colState = new TableColumn<>("State");
        colState.setCellValueFactory(new PropertyValueFactory<>("state"));

        TableColumn<OrderRow, String> colItems = new TableColumn<>("Items");
        colItems.setCellValueFactory(new PropertyValueFactory<>("items"));

        TableColumn<OrderRow, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        table.getColumns().setAll(colId, colDate, colState, colItems, colTotal);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /* Loads EVERY order, applies search + date filters, NO 4-row limit */
    private void loadAllOrders(TableView<OrderRow> table,
                               String text,
                               LocalDate from,
                               LocalDate to) {

        Stream<Order> stream = OrderHub.getOrderHub().getAllOrders().values().stream();

        /* text filter */
        if (text != null && !text.isBlank()) {
            String needle = text.toLowerCase();
            stream = stream.filter(o ->
                    String.valueOf(o.getOrderId()).contains(needle) ||
                            o.getProductList().stream()
                                    .anyMatch(p -> p.getProductDescription().toLowerCase().contains(needle)));
        }

        /* date filters */
        if (from != null)
            stream = stream.filter(o -> !LocalDate.parse(o.getOrderedDateTime().substring(0,10)).isBefore(from));
        if (to != null)
            stream = stream.filter(o -> !LocalDate.parse(o.getOrderedDateTime().substring(0,10)).isAfter(to));

        /* newest first */
        stream = stream.sorted(Comparator.comparing(Order::getOrderedDateTime).reversed());

        /* map to rows */
        ObservableList<OrderRow> rows = FXCollections.observableArrayList();
        stream.forEach(o -> rows.add(new OrderRow(
                String.valueOf(o.getOrderId()),
                o.getOrderedDateTime(),
                o.getState().toString(),
                o.getProductList().size() + " items",
                String.format("£%.2f", o.getOrderTotal())
        )));

        table.setItems(rows);
    }


    private void buildOrderTable() {
        TableColumn<OrderRow, String> colId = new TableColumn<>("Order ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("orderId"));

        TableColumn<OrderRow, String> colDate = new TableColumn<>("Date & Time");
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateTime"));

        TableColumn<OrderRow, String> colState = new TableColumn<>("State");
        colState.setCellValueFactory(new PropertyValueFactory<>("state"));

        TableColumn<OrderRow, String> colItems = new TableColumn<>("Items");
        colItems.setCellValueFactory(new PropertyValueFactory<>("items"));

        TableColumn<OrderRow, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        /* ---------- live action buttons ---------- */
        TableColumn<OrderRow, Void> colAction = new TableColumn<>("Action");
        colAction.setCellFactory(col -> new TableCell<OrderRow, Void>() {
            private final Button btn = new Button();
            {
                btn.getStyleClass().add("login-btn");
                btn.setOnAction(e -> {
                    OrderRow row = getTableView().getItems().get(getIndex());
                    int orderId = Integer.parseInt(row.getOrderId());
                    OrderState current = OrderState.valueOf(row.getState());

                    try {
                        if (current == OrderState.Ordered) {
                            OrderHub.getOrderHub().changeOrderStateMoveFile(orderId, OrderState.Progressing);
                        } else if (current == OrderState.Progressing) {
                            OrderHub.getOrderHub().changeOrderStateMoveFile(orderId, OrderState.Collected);
                        }
                        controller.refreshOrders(); // refresh table
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }@Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    OrderRow row = getTableView().getItems().get(getIndex());
                    OrderState st = OrderState.valueOf(row.getState());
                    if (st == OrderState.Ordered) {
                        btn.setText("Start Progress");
                        setGraphic(btn);
                    } else if (st == OrderState.Progressing) {
                        btn.setText("Mark Collected");
                        setGraphic(btn);
                    } else {
                        setGraphic(null); // Collected -> no button
                    }
                }
            }
        });

        orderTable.getColumns().setAll(colId, colDate, colState, colItems, colTotal, colAction);
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }





    /* helper – filters the *full* list and re-displays */
    private void applyFilter(String text, LocalDate from, LocalDate to) {
        Collection<Order> all = OrderHub.getOrderHub().getAllOrders().values(); // raw map values
        Stream<Order> stream = all.stream()
                .filter(o -> text == null || text.isBlank() ||
                        String.valueOf(o.getOrderId()).contains(text) ||
                        o.getProductList().stream().anyMatch(p -> p.getProductDescription().toLowerCase().contains(text.toLowerCase())))
                .filter(o -> from == null || !LocalDate.parse(o.getOrderedDateTime().substring(0, 10)).isBefore(from))
                .filter(o -> to   == null || !LocalDate.parse(o.getOrderedDateTime().substring(0, 10)).isAfter(to))
                .sorted(Comparator.comparing(Order::getOrderedDateTime).reversed())
                .limit(4); // keep last-4 rule

        displayOrders(stream.toList());
    }
    public void displayOrders(Collection<Order> orders) {
        List<Order> sorted = orders.stream()
                .sorted(Comparator.comparing(Order::getOrderedDateTime).reversed())
                .limit(4)
                .toList();

        ObservableList<OrderRow> rows = FXCollections.observableArrayList();
        for (Order o : sorted) {   // ✅ use sorted
            rows.add(new OrderRow(
                    String.valueOf(o.getOrderId()),
                    o.getOrderedDateTime(),
                    o.getState().toString(),
                    o.getProductList().size() + " items",
                    String.format("£%.2f", o.getOrderTotal())
            ));
        }
        orderTable.setItems(rows);
    }


    /* ----------------------------------------------------------
     *  LOW-STOCK TAB
     * ---------------------------------------------------------- */
    private Node createLowStockTab() {
        Label title = new Label("Items with stock ≤ 5");
        title.getStyleClass().add("manager-title");

        buildLowStockTable();

        VBox root = new VBox(10, title, lowStockTable);
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("manager-card");
        root.setPadding(new Insets(15));
        return root;
    }

    public void setSharedDatabase(DatabaseRW db){
        model.databaseRW = db;
    }

    private void buildLowStockTable() {
        TableColumn<ProductRow, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("productId"));

        TableColumn<ProductRow, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<ProductRow, String> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        TableColumn<ProductRow, String> colAction = new TableColumn<>("Add Stock");
        colAction.setCellFactory(col -> new TableCell<ProductRow, String>() {
            final HBox box = new HBox(5);
            final Spinner<Integer> spinner = new Spinner<>(0, 999, 1, 1);
            final Button updateBtn = new Button("Update");
            {
                box.getChildren().addAll(spinner, updateBtn);
                updateBtn.setOnAction(e -> {
                    ProductRow row = getTableView().getItems().get(getIndex());
                    int newQty = row.p.getStockQuantity() + spinner.getValue();
                    try {
                        controller.updateStock(row.p.getProductId(), newQty);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        lowStockTable.getColumns().setAll(colId, colName, colStock, colAction);
        lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void displayLowStock(List<Product> products) {
        ObservableList<ProductRow> rows = FXCollections.observableArrayList();
        for (Product p : products) rows.add(new ProductRow(p));
        lowStockTable.setItems(rows);
    }

    public void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static class OrderRow {
        private final String orderId, dateTime, state, items, total;
        public OrderRow(String orderId, String dateTime, String state, String items, String total) {
            this.orderId = orderId; this.dateTime = dateTime; this.state = state;
            this.items = items; this.total = total;
        }
        public String getOrderId() { return orderId; }
        public String getDateTime() { return dateTime; }
        public String getState() { return state; }
        public String getItems() { return items; }
        public String getTotal() { return total; }
    }

    public static class ProductRow {
        public final Product p;
        public ProductRow(Product p) { this.p = p; }
        public String getProductId()   { return p.getProductId(); }
        public String getDescription() { return p.getProductDescription(); }
        public String getStock() {
            int q = p.getStockQuantity();
            return q == 0 ? "OUT OF STOCK" : String.valueOf(q);
        }
        public String getDummy() { return ""; }  // button column
    }
}