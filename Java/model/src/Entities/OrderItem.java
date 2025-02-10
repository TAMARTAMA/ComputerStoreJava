package Entities;

public class OrderItem {
   private   long id;
   private int amount;
    public OrderItem(long id, int amount) {
        this.id = id;
        this.amount = amount;
    }
    public long getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getAmount() {
        return amount;
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }

}
