package ci553.happyshop.catalogue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void constructorAndGettersShouldStoreValuesCorrectly() {
        Product p = new Product("0002", "DAB Radio", "0002.jpg", 29.99, 12);

        assertEquals("0002", p.getProductId());
        assertEquals("DAB Radio", p.getProductDescription());
        assertEquals("0002.jpg", p.getProductImageName());
        assertEquals(29.99, p.getUnitPrice(), 0.0001);
        assertEquals(12, p.getStockQuantity());

        // default in your class:
        assertEquals(1, p.getOrderedQuantity());
    }

    @Test
    void setOrderedQuantityShouldUpdateQuantity() {
        Product p = new Product("0003", "Cable", "0003.jpg", 4.99, 50);

        p.setOrderedQuantity(5);
        assertEquals(5, p.getOrderedQuantity());

        p.setOrderedQuantity(1);
        assertEquals(1, p.getOrderedQuantity());
    }

    @Test
    void compareToShouldSortByProductIdAscending() {
        Product p1 = new Product("0001", "TV", "0001.jpg", 269.0, 3);
        Product p2 = new Product("0002", "Mouse", "0002.jpg", 19.99, 2);
        Product p3 = new Product("0010", "USB", "0010.jpg", 6.99, 10);

        assertTrue(p1.compareTo(p2) < 0);  // "0001" < "0002"
        assertTrue(p2.compareTo(p1) > 0);  // "0002" > "0001"
        assertEquals(0, p2.compareTo(new Product("0002", "Other", "x.jpg", 1.0, 1)));

        assertTrue(p2.compareTo(p3) < 0);  // "0002" < "0010"
    }

    @Test
    void toStringShouldContainFormattedPriceAndFields() {
        Product p = new Product("0004", "Keyboard", "0004.jpg", 49.9, 4);

        String s = p.toString();

        // check key parts rather than the whole string (less fragile)
        assertTrue(s.contains("Id: 0004"));
        assertTrue(s.contains("£49.90/uint"));     // 2 decimal places expected
        assertTrue(s.contains("stock: 4"));
        assertTrue(s.contains("Keyboard"));
    }
}


