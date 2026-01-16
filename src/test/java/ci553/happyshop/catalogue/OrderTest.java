package ci553.happyshop.catalogue;

import ci553.happyshop.orderManagement.OrderState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void constructor_setsFields_andGettersReturnCorrectValues() {
        // Arrange
        Product p1 = new Product("0002", "DAB Radio", "0002.jpg", 29.99, 10);
        Product p2 = new Product("0004", "Watch", "0004.jpg", 29.99, 10);
        p1.setOrderedQuantity(1);
        p2.setOrderedQuantity(2);

        ArrayList<Product> list = new ArrayList<>();
        list.add(p1);
        list.add(p2);

        // Act
        Order order = new Order(10, OrderState.Ordered, "2025-05-03 16:52:24", list);

        // Assert
        assertEquals(10, order.getOrderId());
        assertEquals(OrderState.Ordered, order.getState());
        assertEquals("2025-05-03 16:52:24", order.getOrderedDateTime());
        assertEquals(2, order.getProductList().size());
        assertEquals("0002", order.getProductList().get(0).getProductId());
    }

    @Test
    void setState_changesOrderState() {
        // Arrange
        ArrayList<Product> list = new ArrayList<>();
        Order order = new Order(1, OrderState.Ordered, "2025-01-01 10:00:00", list);

        // Act
        order.setState(OrderState.Progressing);

        // Assert
        assertEquals(OrderState.Progressing, order.getState());
    }

    @Test
    void orderDetails_containsExpectedHeaderLinesAndItemsSection() {
        // Arrange
        Product p = new Product("0007", "USB drive", "0007.jpg", 6.99, 10);
        p.setOrderedQuantity(1);

        ArrayList<Product> list = new ArrayList<>();
        list.add(p);

        Order order = new Order(10, OrderState.Ordered, "2025-05-03 16:52:24", list);

        // Act
        String details = order.orderDetails();

        // Assert (match your exact strings/labels)
        assertTrue(details.contains("Order ID: 10"));
        assertTrue(details.contains("State: Ordered"));
        assertTrue(details.contains("OrderedDateTime: 2025-05-03 16:52:24"));
        assertTrue(details.contains("ProgressingDateTime:"));
        assertTrue(details.contains("CollectedDateTime:"));
        assertTrue(details.contains("Items:"));

        // Item info should appear somewhere in formatted list
        assertTrue(details.contains("0007"));
        assertTrue(details.toLowerCase().contains("usb"));
    }

    @Test
    void getOrderTotal_calculatesTotalUsingUnitPriceTimesOrderedQuantity() {
        // Arrange
        Product p1 = new Product("0002", "DAB Radio", "0002.jpg", 29.99, 10);
        Product p2 = new Product("0007", "USB drive", "0007.jpg", 6.99, 10);

        p1.setOrderedQuantity(2); // 59.98
        p2.setOrderedQuantity(1); // 6.99

        ArrayList<Product> list = new ArrayList<>();
        list.add(p1);
        list.add(p2);

        Order order = new Order(99, OrderState.Ordered, "2025-05-03 16:52:24", list);

        // Act
        double total = order.getOrderTotal();

        // Assert (use delta for floating point)
        assertEquals(66.97, total, 0.001);
    }
}

