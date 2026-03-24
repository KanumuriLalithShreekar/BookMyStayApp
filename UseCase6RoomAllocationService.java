import java.util.*;

// Reservation (Booking Request)
class Reservation {
    private String guestName;
    private String roomType;

    public Reservation(String guestName, String roomType) {
        this.guestName = guestName;
        this.roomType = roomType;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getRoomType() {
        return roomType;
    }
}

// Inventory Service (State Holder)
class InventoryService {
    private Map<String, Integer> availability;

    public InventoryService() {
        availability = new HashMap<>();
    }

    public void addRoom(String type, int count) {
        availability.put(type, count);
    }

    public int getAvailableRooms(String type) {
        return availability.getOrDefault(type, 0);
    }

    // Update inventory immediately after allocation
    public void decrementRoom(String type) {
        availability.put(type, availability.get(type) - 1);
    }
}

// Booking Request Queue (FIFO)
class BookingRequestQueue {
    private Queue<Reservation> queue = new LinkedList<>();

    public void addRequest(Reservation r) {
        queue.offer(r);
    }

    public Reservation getNextRequest() {
        return queue.poll(); // FIFO
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}

// Booking Service (Core Allocation Logic)
class BookingService {

    private InventoryService inventory;

    // Track all allocated room IDs (global uniqueness)
    private Set<String> allocatedRoomIds;

    // Map room type -> allocated room IDs
    private Map<String, Set<String>> roomAllocations;

    public BookingService(InventoryService inventory) {
        this.inventory = inventory;
        this.allocatedRoomIds = new HashSet<>();
        this.roomAllocations = new HashMap<>();
    }

    public void processBookings(BookingRequestQueue queue) {

        while (!queue.isEmpty()) {
            Reservation request = queue.getNextRequest();

            String type = request.getRoomType();
            String guest = request.getGuestName();

            System.out.println("\nProcessing booking for: " + guest);

            int available = inventory.getAvailableRooms(type);

            if (available > 0) {

                // Generate unique room ID
                String roomId = generateRoomId(type);

                // Ensure uniqueness (defensive check)
                if (!allocatedRoomIds.contains(roomId)) {

                    // Atomic operation start
                    allocatedRoomIds.add(roomId);

                    roomAllocations
                            .computeIfAbsent(type, k -> new HashSet<>())
                            .add(roomId);

                    inventory.decrementRoom(type);
                    // Atomic operation end

                    System.out.println("Booking CONFIRMED");
                    System.out.println("Guest: " + guest);
                    System.out.println("Room Type: " + type);
                    System.out.println("Allocated Room ID: " + roomId);

                } else {
                    // Extremely rare fallback (in case of collision)
                    System.out.println("Error: Duplicate Room ID detected!");
                }

            } else {
                System.out.println("Booking FAILED - No rooms available for type: " + type);
            }
        }
    }

    // Simple unique ID generator
    private String generateRoomId(String type) {
        return type.substring(0, 1).toUpperCase() + UUID.randomUUID().toString().substring(0, 5);
    }
}

// Main Class
public class UseCase6RoomAllocationService {

    public static void main(String[] args) {

        // Step 1: Setup Inventory
        InventoryService inventory = new InventoryService();
        inventory.addRoom("Single", 2);
        inventory.addRoom("Double", 1);

        // Step 2: Setup Booking Queue
        BookingRequestQueue queue = new BookingRequestQueue();

        queue.addRequest(new Reservation("Alice", "Single"));
        queue.addRequest(new Reservation("Bob", "Single"));
        queue.addRequest(new Reservation("Charlie", "Single")); // Should fail
        queue.addRequest(new Reservation("David", "Double"));

        // Step 3: Process Bookings
        BookingService bookingService = new BookingService(inventory);
        bookingService.processBookings(queue);
    }
}