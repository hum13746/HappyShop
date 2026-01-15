package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.orderManagement.OrderState;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;

public class CustomerController {

    public CustomerModel cusModel;
    public CustomerView  cusView;   // wired by setView

    public CustomerModel getModel() {
        return cusModel;
    }

    public void setView(CustomerView view) {
        this.cusView = view;
    }

    public void doAction(String action) throws SQLException, IOException {
        switch (action) {
            case "Search"        -> cusModel.search();
            case "Add to Trolley"-> cusModel.addToTrolley();
            case "Cancel"        -> cusModel.cancel();
            case "Check Out"     -> {
                cusModel.checkOut();          // creates order, clears trolley, fills receipt text
                cusView.bringReceiptToFront(); // pop receipt window to top
            }
            case "Pay"           -> payOrder();
            case "OK & Close"    -> cusView.closeAllBricks();
            default -> {
                if (action.startsWith("Remove one:")) {
                    String productId = action.substring(11);
                    cusModel.removeOneFromTrolley(productId);
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Pay button: mark the newest Ordered order as Paid/Collected        */
    /* ------------------------------------------------------------------ */
    private void payOrder() {
        OrderHub hub = OrderHub.getOrderHub();

        hub.getAllOrders().values()
                .stream()
                .filter(o -> o.getState() == OrderState.Ordered)
                .max(Comparator.comparing(Order::getOrderedDateTime))
                .ifPresentOrElse(o -> {
                    try {
                        hub.changeOrderStateMoveFile(o.getOrderId(), OrderState.Collected);
                        cusView.refreshOrderTable();
                        cusView.showInfo("Payment accepted – order #" + o.getOrderId() + " completed.");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        cusView.showInfo("Payment failed: " + ex.getMessage());
                    }
                }, () -> cusView.showInfo("No unpaid orders found."));
    }

}