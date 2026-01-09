package ci553.happyshop.client.manager;
import java.sql.SQLException;
public class ManagerController {

    private ManagerModel model;

    public void setModel(ManagerModel model) {
        this.model = model;
    }

    public void refreshOrders() {
        model.refreshOrderList();
    }

    public void updateStock(String productId, int newQty) throws SQLException {
        model.updateStock(productId, newQty);
    }
}