package ci553.happyshop.orderManagement;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.client.orderTracker.OrderTracker;
import ci553.happyshop.client.picker.PickerModel;
import ci553.happyshop.storageAccess.OrderFileManager;
import ci553.happyshop.utility.ProductListFormatter;
import ci553.happyshop.utility.StorageLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class OrderHub {

    private static OrderHub orderHub;

    private final Path orderedPath = StorageLocation.orderedPath;
    private final Path progressingPath = StorageLocation.progressingPath;
    private final Path collectedPath = StorageLocation.collectedPath;

    private final TreeMap<Integer, OrderState> orderMap = new TreeMap<>();

    private final ArrayList<OrderTracker> orderTrackerList = new ArrayList<>();
    private final ArrayList<PickerModel> pickerModelList = new ArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private OrderHub() {
    }

    public static OrderHub getOrderHub() {
        if (orderHub == null) orderHub = new OrderHub();
        return orderHub;
    }

    // -------------------- create new order --------------------
    public Order newOrder(ArrayList<Product> trolley) throws IOException, SQLException {
        int orderId = OrderCounter.generateOrderId();
        String orderedDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Order theOrder = new Order(orderId, OrderState.Ordered, orderedDateTime, trolley);

        // write to file ONCE
        String itemsOnly = ProductListFormatter.buildString(theOrder.getProductList());
        OrderFileManager.createOrderFile(orderedPath, orderId, itemsOnly);


        // update in-memory state
        orderMap.put(orderId, theOrder.getState());
        notifyOrderTrackers();
        notifyPickerModels();

        return theOrder;
    }

    // -------------------- observers --------------------
    public void registerOrderTracker(OrderTracker orderTracker) {
        orderTrackerList.add(orderTracker);
    }

    public void notifyOrderTrackers() {
        for (OrderTracker orderTracker : orderTrackerList) {
            orderTracker.setOrderMap(orderMap);
        }
    }

    public void registerPickerModel(PickerModel pickerModel) {
        pickerModelList.add(pickerModel);
    }

    public void notifyPickerModels() {
        TreeMap<Integer, OrderState> orderMapForPicker = new TreeMap<>();
        for (Map.Entry<Integer, OrderState> e : orderMap.entrySet()) {
            if (e.getValue() == OrderState.Ordered || e.getValue() == OrderState.Progressing) {
                orderMapForPicker.put(e.getKey(), e.getValue());
            }
        }
        for (PickerModel pickerModel : pickerModelList) {
            pickerModel.setOrderMap(orderMapForPicker);
        }
    }

    // -------------------- state change --------------------
    public void changeOrderStateMoveFile(int orderId, OrderState newState) throws IOException {
        if (!orderMap.containsKey(orderId)) return;
        if (orderMap.get(orderId) == newState) return;

        OrderState current = orderMap.get(orderId);

        // update map first
        orderMap.put(orderId, newState);
        notifyOrderTrackers();
        notifyPickerModels();

        // update file + move according to current->new
        if (newState == OrderState.Progressing) {
            OrderFileManager.updateAndMoveOrderFile(orderId, newState, orderedPath, progressingPath);
        } else if (newState == OrderState.Collected) {
            // allow Ordered -> Collected OR Progressing -> Collected
            if (current == OrderState.Ordered) {
                OrderFileManager.updateAndMoveOrderFile(orderId, newState, orderedPath, collectedPath);
            } else {
                OrderFileManager.updateAndMoveOrderFile(orderId, newState, progressingPath, collectedPath);
            }
            removeCollectedOrder(orderId);
        }
    }

    private void removeCollectedOrder(int orderId) {
        scheduler.schedule(() -> {
            orderMap.remove(orderId);
            notifyOrderTrackers();
        }, 10, TimeUnit.SECONDS);
    }

    public String getOrderDetailForPicker(int orderId) throws IOException {
        OrderState state = orderMap.get(orderId);
        if (state == OrderState.Progressing) {
            return OrderFileManager.readOrderFile(progressingPath, orderId);
        }
        return "This function is only for picker (Progressing orders).";
    }

    // -------------------- init from disk --------------------
    public void initializeOrderMap() {
        ArrayList<Integer> orderedIds = orderIdsLoader(orderedPath);
        ArrayList<Integer> progressingIds = orderIdsLoader(progressingPath);

        for (Integer id : orderedIds) orderMap.put(id, OrderState.Ordered);
        for (Integer id : progressingIds) orderMap.put(id, OrderState.Progressing);

        notifyOrderTrackers();
        notifyPickerModels();
    }

    private ArrayList<Integer> orderIdsLoader(Path dir) {
        ArrayList<Integer> orderIds = new ArrayList<>();
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            try (Stream<Path> fileStream = Files.list(dir)) {
                for (Path file : fileStream.filter(Files::isRegularFile).toList()) {
                    String fileName = file.getFileName().toString();
                    if (fileName.endsWith(".txt")) {
                        try {
                            int id = Integer.parseInt(fileName.substring(0, fileName.lastIndexOf('.')));
                            orderIds.add(id);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid file name: " + fileName);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading " + dir + ", " + e.getMessage());
            }
        }
        return orderIds;
    }

    // -------------------- history for Manager/Customer --------------------
    public Map<Integer, Order> getAllOrders() {
        Map<Integer, Order> result = new LinkedHashMap<>();

        // read from all 3 folders every time (this is what makes it seamless)
        Path[] folders = {orderedPath, progressingPath, collectedPath};

        for (Path folder : folders) {
            try (Stream<Path> files = Files.list(folder)) {
                files.filter(p -> p.getFileName().toString().endsWith(".txt"))
                        .forEach(p -> {
                            String name = p.getFileName().toString();
                            String idPart = name.substring(0, name.lastIndexOf('.'));
                            try {
                                int id = Integer.parseInt(idPart);
                                Order o = readOrderFromDisk(id);
                                if (o != null) result.put(id, o);
                            } catch (Exception ignored) {
                            }
                        });
            } catch (IOException ignored) {
            }
        }

        return result;
    }


    private void readFolderIntoMap(Path folder, Map<Integer, Order> out) {
        try {
            if (!Files.exists(folder) || !Files.isDirectory(folder)) return;

            try (Stream<Path> stream = Files.list(folder)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".txt"))
                        .forEach(p -> {
                            String name = p.getFileName().toString().replace(".txt", "");
                            try {
                                int id = Integer.parseInt(name);
                                Order o = readOrderFromDisk(id);
                                if (o != null) out.put(id, o);
                            } catch (Exception ignored) {
                            }
                        });
            }
        } catch (Exception e) {
            System.out.println("Error reading folder " + folder + ": " + e.getMessage());
        }
    }

    private Order readOrderFromDisk(int orderId) throws IOException {
        Path[] folders = {orderedPath, progressingPath, collectedPath};

        for (Path folder : folders) {
            Path file = folder.resolve(orderId + ".txt");
            if (!Files.exists(file)) continue;

            List<String> lines = Files.readAllLines(file);
            if (lines.isEmpty()) return null;

            int id = -1;
            String date = null;
            OrderState st = null;

            boolean inItems = false;
            ArrayList<Product> items = new ArrayList<>();

            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty()) continue;

                // ---------------- header parsing ----------------
                if (!inItems) {

                    if (line.startsWith("Order ID:")) {
                        String s = line.substring("Order ID:".length()).trim();
                        if (!s.isEmpty() && s.matches("\\d+")) id = Integer.parseInt(s);
                        continue;
                    }

                    if (line.startsWith("State:")) {
                        String s = line.substring("State:".length()).trim();
                        if (!s.isEmpty()) {
                            try {
                                st = OrderState.valueOf(s);
                            } catch (Exception ignored) {
                            }
                        }
                        continue;
                    }

                    if (line.startsWith("OrderedDateTime:")) {
                        String s = line.substring("OrderedDateTime:".length()).trim();
                        if (!s.isEmpty()) date = s;
                        continue;
                    }

                    if (line.equalsIgnoreCase("Items:")) {
                        inItems = true;
                        continue;
                    }

                    // ignore other header lines
                    continue;
                }

                // ---------------- item parsing ----------------
                // skip totals line
                if (line.toLowerCase().startsWith("total")) continue;

                // must start with 4-digit product id
                if (!line.matches("^\\d{4}.*")) continue;

                String productId = line.substring(0, 4);

                // qty (supports "x2" or "(2)")
                int qty = 1;
                var mX = java.util.regex.Pattern.compile("\\bx\\s*(\\d+)\\b").matcher(line);
                var mP = java.util.regex.Pattern.compile("\\((\\d+)\\)").matcher(line);
                if (mX.find()) qty = Integer.parseInt(mX.group(1));
                else if (mP.find()) qty = Integer.parseInt(mP.group(1));

                // price (supports £3.00)
                double price = 0.0;
                var mPrice = java.util.regex.Pattern.compile("£\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(line);
                if (mPrice.find()) price = Double.parseDouble(mPrice.group(1));

                // description = remove qty and price parts
                String desc = line.substring(4).trim();
                desc = desc.replaceAll("\\bx\\s*\\d+\\b", "").trim();
                desc = desc.replaceAll("\\(\\d+\\)", "").trim();
                desc = desc.replaceAll("£\\s*[0-9]+(?:\\.[0-9]+)?", "").trim();

                Product pr = new Product(productId, desc, "", price, 0);
                pr.setOrderedQuantity(qty);
                items.add(pr);
            }

            if (id < 0 || date == null || st == null) return null;
            return new Order(id, st, date, items);
        }

        return null;
    }
}
