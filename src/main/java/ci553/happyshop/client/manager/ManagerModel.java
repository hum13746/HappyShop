package ci553.happyshop.client.manager;
import java.util.ArrayList;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.storageAccess.DatabaseRWFactory;
import ci553.happyshop.orderManagement.OrderHub;
import java.util.Collection;
import java.util.Comparator;
import ci553.happyshop.catalogue.Order;
import java.sql.SQLException;
import java.util.List;

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
        // REAL orders from hub
        Collection<Order> real = OrderHub.getOrderHub().getAllOrders().values();
        if (real.isEmpty()) {
            // create one dummy order for demo
            try {
                Product p = databaseRW.searchByProductId("0001");
                if (p != null) {
                    p.setOrderedQuantity(1);
                    ArrayList<Product> list = new ArrayList<>();
                    list.add(p);
                    Order dummy = OrderHub.getOrderHub().newOrder(list);
                    real = List.of(dummy);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        view.displayOrders(real);
    }

    public List<Product> getLowStock(int threshold) throws SQLException {
        return databaseRW.getLowStockProducts(threshold);
    }

    public void updateStock(String productId, int newQty) throws SQLException {
        databaseRW.updateStock(productId, newQty);
        refreshLowStockTable();   // keep table live
    }
    public List<Product> getAllProducts() throws SQLException {
        return databaseRW.getAllProducts(); // correct method name // every item in DB
    }

    public void refreshLowStockTable() {
        try {
            List<Product> all = getAllProducts();          // load everything
            List<Product> low = all.stream()
                    .filter(p -> p.getStockQuantity() <= 5)
                    .toList();              // keep ≤ 5
            view.displayLowStock(low);
        } catch (SQLException e) {
            view.showError("DB error: " + e.getMessage());
        }
    }
}
