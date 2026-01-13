package ci553.happyshop.client.manager;
import java.util.List;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.storageAccess.DatabaseRWFactory;
import ci553.happyshop.orderManagement.OrderHub;
import java.util.Collection;
import java.util.Comparator;
import ci553.happyshop.catalogue.Order;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ManagerModel {

    private ManagerView view;
    private final DatabaseRW databaseRW = DatabaseRWFactory.createDatabaseRW();

    public void setView(ManagerView view) {
        this.view = view;
        refreshLowStockTable();   // load on start
    }

    /* return last 4 orders (any state) newest first */
    public Collection<Order> getRecentOrders() {
        return OrderHub.getOrderHub().getAllOrders().values()
                .stream()
                .sorted(Comparator.comparing(Order::getOrderedDateTime).reversed())
                .limit(4)
                .toList();
    }

    public void refreshOrderList() {
        // Real orders from hub
        Collection<Order> real = OrderHub.getOrderHub().getAllOrders().values();
        if (real.isEmpty()) {
            // Seed one dummy order for demo
            try {
                Product p = databaseRW.searchByProductId("0001");
                if (p != null) {
                    p.setOrderedQuantity(1);
                    ArrayList<Product> list = new ArrayList<>();
                    list.add(p);
                    Order dummy = OrderHub.getOrderHub().newOrder(list);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        view.displayOrders(real);
    }

    public ArrayList<Product> getLowStock(int threshold) throws SQLException {
        return databaseRW.getLowStockProducts(threshold);
    }

    public void updateStock(String productId, int newQty) throws SQLException {
        databaseRW.updateStock(productId, newQty);
        refreshLowStockTable();   // keep table live
    }

    public ArrayList<Product> getAllProducts() throws SQLException {
        return databaseRW.getAllProducts(); // correct spelling
    }
    public void refreshLowStockTable() {
        try {
            // 1. Get all products
            ArrayList<Product> all = getAllProducts();
            System.out.println("DEBUG – getAllProducts returned " + all.size() + " items");

            // 2. If empty, seed 4 products with stock ≤ 5
            if (all.isEmpty()) {
                databaseRW.insertNewProduct("0001", "40 inch TV", 269.0, "0001.jpg", 3);
                databaseRW.insertNewProduct("0002", "Mouse", 19.99, "0002.jpg", 2);
                databaseRW.insertNewProduct("0003", "Cable", 4.99, "0003.jpg", 1);
                databaseRW.insertNewProduct("0004", "Keyboard", 49.99, "0004.jpg", 4);
                all = getAllProducts(); // reload
                System.out.println("DEBUG – seeded 4 products, now " + all.size() + " items");
            }

            // 3. Lower some stock to ≤ 5
            if (all.size() > 0) {
                // Lower stock of 0002, 0003, 0004 to ≤ 5
                databaseRW.updateStock("0002", 2);
                databaseRW.updateStock("0003", 1);
                databaseRW.updateStock("0004", 4);
                all = getAllProducts(); // reload
                System.out.println("DEBUG – lowered some stock, now " + all.size() + " items");
            }

            // 4. Filter to ≤ 5
            ArrayList<Product> low = (ArrayList<Product>) all.stream()
                    .filter(prod -> prod.getStockQuantity() <= 5)
                    .collect(Collectors.toCollection(ArrayList::new));
            System.out.println("DEBUG – low stock filtered to " + low.size() + " items");

            // 5. Send to view
            view.displayLowStock(low);

        } catch (SQLException e) {
            System.out.println("DEBUG – SQLException in refreshLowStockTable: " + e.getMessage());
            view.showError("DB error: " + e.getMessage());
        }
    }

}