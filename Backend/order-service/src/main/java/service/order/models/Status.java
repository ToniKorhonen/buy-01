package service.order.models;

public enum Status {
    ADDED, // when item is added to cart
    STARTED, // initial status when order is created
    ONGOING, // when the order is being processed( started by seller)
    RECEIVED, // when the buyer has received the order(set by buyer)
    CANCELLED, // when the order is cancelled(by buyer or seller)
}
