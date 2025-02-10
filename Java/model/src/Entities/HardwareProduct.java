package Entities;

public class HardwareProduct extends Product {

    private int warrantyPeriod;

    public HardwareProduct(long id, String name, String description, float pricePerUnit, int quantity, int warrantyPeriod) {
        super(id, name, description, pricePerUnit, quantity);
        this.warrantyPeriod = warrantyPeriod;
    }
    public int getWarrantyPeriod() {
        return warrantyPeriod;
    }
    public void setWarrantyPeriod(int warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
    }

    @Override
    public float getPrice() {
        return warrantyPeriod*pricePerUnit;
    }

    @Override
    public String toString() {
        return "HardwareProduct{" +
                super.toString()+
                "WarrantyPeriod=" + warrantyPeriod +
                '}';
    }

}
