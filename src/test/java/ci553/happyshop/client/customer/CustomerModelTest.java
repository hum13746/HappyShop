package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerModelTest {

    static class FakeDatabaseRW implements DatabaseRW {
        Product productToReturn;
        ArrayList<Product> purchaseResult = new ArrayList<>();
        ArrayList<Product> allProducts = new ArrayList<>();

        @Override
        public ArrayList<Product> searchProduct(String keyword) throws SQLException {
            return new ArrayList<>();
        }

        @Override
        public Product searchByProductId(String productId) throws SQLException {
            return productToReturn;
        }

        @Override
        public ArrayList<Product> purchaseStocks(ArrayList<Product> proList) throws SQLException {
            return purchaseResult;
        }

        @Override public void updateProduct(String id, String des, double price, String imageName, int stock) throws SQLException {}
        @Override public void updateStock(String productId, int newQty) throws SQLException {}
        @Override public void deleteProduct(String id) throws SQLException {}
        @Override public void insertNewProduct(String id, String des, double price, String image, int stock) throws SQLException {}
        @Override public boolean isProIdAvailable(String productId) throws SQLException { return true; }
        @Override public ArrayList<Product> getLowStockProducts(int threshold) throws SQLException { return new ArrayList<>(); }
        @Override public ArrayList<Product> getAllProducts() { return allProducts; }
    }

    @Test
    void dummyTestJustToCompile() {
        assertTrue(true);
    }
}

