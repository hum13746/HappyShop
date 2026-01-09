package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.ProductListFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomerModel {

    public CustomerView cusView;
    public DatabaseRW databaseRW;

    private Product theProduct = null;
    private ArrayList<Product> trolley = new ArrayList<>();
    private Map<String, Product> trolleyMap = new LinkedHashMap<>();

    /* ---------- UI display strings ---------- */
    private String imageName = "imageHolder.jpg";
    private String displayLaSearchResult = "No Product was searched yet";
    private String displayTaTrolley = "";
    private String displayTaReceipt = "";

    /* ---------- search ---------- */
    void search() throws SQLException {
        String productId = cusView.tfId.getText().trim();
        if (!productId.isEmpty()) {
            theProduct = databaseRW.searchByProductId(productId);
            if (theProduct != null && theProduct.getStockQuantity() > 0) {
                double unitPrice = theProduct.getUnitPrice();
                String description = theProduct.getProductDescription();
                int stock = theProduct.getStockQuantity();
                String baseInfo = String.format("Product_Id: %s\n%s,\nPrice: £%.2f", productId, description, unitPrice);
                String quantityInfo = stock < 100 ? String.format("\n%d units left.", stock) : "";
                displayLaSearchResult = baseInfo + quantityInfo;
                System.out.println(displayLaSearchResult);
            } else {
                theProduct = null;
                displayLaSearchResult = "No Product was found with ID " + productId;
                System.out.println("No Product was found with ID " + productId);
            }
        } else {
            theProduct = null;
            displayLaSearchResult = "Please type ProductID";
            System.out.println("Please type ProductID.");
        }
        updateView();
    }

    /* ---------- add ---------- */
    void addToTrolley() {
        if (theProduct != null) {
            String id = theProduct.getProductId();
            if (trolleyMap.containsKey(id)) {
                Product inTrolley = trolleyMap.get(id);
                inTrolley.setOrderedQuantity(inTrolley.getOrderedQuantity() + 1);
            } else {
                Product copy = new Product(theProduct.getProductId(),
                        theProduct.getProductDescription(),
                        theProduct.getProductImageName(),
                        theProduct.getUnitPrice(),
                        theProduct.getStockQuantity());
                copy.setOrderedQuantity(1);
                trolleyMap.put(id, copy);
            }
            rebuildTrolleyList();
            displayTaTrolley = ProductListFormatter.buildString(trolley);
            displayLaSearchResult = "Added to trolley (qty " + trolleyMap.get(id).getOrderedQuantity() + ")";
        } else {
            displayLaSearchResult = "Please search for an available product before adding it to the trolley";
        }
        displayTaReceipt = "";
        updateView();
    }

    /* ---------- checkout ---------- */
    void checkOut() throws IOException, SQLException {
        rebuildTrolleyList();
        System.out.println("DEBUG – checkOut() started, trolley size = " + trolley.size());

        if (!trolley.isEmpty()) {
            ArrayList<Product> groupedTrolley = groupProductsById(trolley);
            ArrayList<Product> insufficientProducts = databaseRW.purchaseStocks(groupedTrolley);

            if (insufficientProducts.isEmpty()) {
                OrderHub orderHub = OrderHub.getOrderHub();
                Order theOrder = orderHub.newOrder(trolley);
                trolley.clear();
                trolleyMap.clear();
                displayTaTrolley = "";
                displayTaReceipt = String.format(
                        "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                        theOrder.getOrderId(),
                        theOrder.getOrderedDateTime(),
                        ProductListFormatter.buildString(theOrder.getProductList())
                );
                System.out.println(displayTaReceipt);
            } else {
                StringBuilder errorMsg = new StringBuilder();
                for (Product p : insufficientProducts) {
                    errorMsg.append("\u2022 ").append(p.getProductId()).append(", ")
                            .append(p.getProductDescription()).append(" (Only ")
                            .append(p.getStockQuantity()).append(" available, ")
                            .append(p.getOrderedQuantity()).append(" requested)\n");
                }
                theProduct = null;
                displayLaSearchResult = "Checkout failed due to insufficient stock for the following products:\n" + errorMsg.toString();
                System.out.println("stock is not enough");
            }
        } else {
            displayTaTrolley = "Your trolley is empty";
            System.out.println("Your trolley is empty");
        }
        updateView();
        cusView.bringReceiptToFront();   // <<<  bring Receipt window to front
    }

    /* ---------- remove one ---------- */
    public boolean removeOneFromTrolley(String productId) {
        Product p = trolleyMap.get(productId);
        if (p == null) return false;
        int qty = p.getOrderedQuantity();
        if (qty > 1) {
            p.setOrderedQuantity(qty - 1);
        } else {
            trolleyMap.remove(productId);
        }
        rebuildTrolleyList();
        displayTaTrolley = ProductListFormatter.buildString(trolley);
        updateView();
        return true;
    }

    /* ---------- cancel ---------- */
    void cancel() {
        trolley.clear();
        trolleyMap.clear();
        displayTaTrolley = "";
        updateView();
    }

    void closeReceipt() {
        displayTaReceipt = "";
    }

    /* ---------- view update ---------- */
    void updateView() {
        if (theProduct != null) {
            imageName = theProduct.getProductImageName();
            String relativeImageUrl = StorageLocation.imageFolder + imageName;
            Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
            imageName = imageFullPath.toUri().toString();
        } else {
            imageName = "imageHolder.jpg";
        }
        cusView.update(imageName, displayLaSearchResult, displayTaTrolley, displayTaReceipt);
        cusView.refreshTrolleyRows();
    }

    /* ---------- helpers ---------- */
    private void rebuildTrolleyList() {
        trolley.clear();
        trolley.addAll(trolleyMap.values());
    }

    private ArrayList<Product> groupProductsById(ArrayList<Product> proList) {
        Map<String, Product> grouped = new HashMap<>();
        for (Product p : proList) {
            String id = p.getProductId();
            if (grouped.containsKey(id)) {
                Product existing = grouped.get(id);
                existing.setOrderedQuantity(existing.getOrderedQuantity() + p.getOrderedQuantity());
            } else {
                grouped.put(id, new Product(p.getProductId(), p.getProductDescription(),
                        p.getProductImageName(), p.getUnitPrice(), p.getStockQuantity()));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    /* ---------- test hooks ---------- */
    public ArrayList<Product> getTrolley() {
        return trolley;
    }

    public double getTrolleyTotal() {
        return trolleyMap.values().stream()
                .mapToDouble(p -> p.getUnitPrice() * p.getOrderedQuantity())
                .sum();
    }
}