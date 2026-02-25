package service.order.models;

public enum Status {
    ADDED,      // product added to cart
    STARTED,    // buyer clicks "pay on delivery"
    ONGOING,    // seller confirms the order
    DELIVERED,  // buyer confirms delivery
    CANCELLED   // cancelled by buyer or seller
}
