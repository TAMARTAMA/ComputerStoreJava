package Entities;

import java.time.LocalDate;
import java.util.ArrayList;

public class PurchaseOrder {
    private long id;
    private ArrayList<OrderItem> orderItems;
    private Customer orderingCustomer;
    private LocalDate orderDate;

    public PurchaseOrder(long id, ArrayList<OrderItem> orderItems, Customer orderingCustomer) {
        this.id = id;
        this.orderItems = orderItems;
        this.orderingCustomer = orderingCustomer;
        this.orderDate = LocalDate.now();
    }

    public void addOrderItem(OrderItem o) {
        this.orderItems.add(o);
    }

    public PurchaseOrder(long id, Customer orderingCustomer) {
        this.id = id;
        this.orderingCustomer = orderingCustomer;
        this.orderDate = LocalDate.now();
        this.orderItems = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ArrayList<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(ArrayList<OrderItem> orderItems) {
        this.orderItems = orderItems;

    }

    public Customer getOrderingCustomer() {
        return orderingCustomer;

    }

    public void setOrderingCustomer(Customer orderingCustomer) {
        this.orderingCustomer = orderingCustomer;

    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    @Override
    public String toString() {
        return "Entities.SoftwareProduct.PurchaseOrder{" +
                "id=" + id +
                ", orderItems=" + orderItems +
                ", orderingCustomer=" + orderingCustomer +
                ", orderDate=" + orderDate +
                '}';
    }
}

