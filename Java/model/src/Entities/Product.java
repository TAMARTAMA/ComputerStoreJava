package Entities;

public abstract class Product {
    protected long id;
    protected String name;
    protected String description;
    protected float pricePerUnit;
    protected int quantity;
    public Product(int id, String name, String description, float pricePerUnit, int quantity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public float getPricePerUnit() {
        return pricePerUnit;
    }
    public void setPricePerUnit(float pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Product() {
        this.id = 0;
        this.name = "product";
        this.description = "";
        this.pricePerUnit = 0;
        this.quantity = 0;
    }

    @Override
    public String toString() {
        return "Entities.Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", pricePerUnit=" + pricePerUnit +
                ", quantity=" + quantity +
                '}';
    }
    public abstract float getPrice();
    public enum kindWare{HARDWARE, SOFTWARE };
}
