package Entities;

import java.util.ArrayList;
import java.util.Comparator;

public class Cart {
    private ArrayList<Product> arr;
    public Cart() {
        arr = new ArrayList<>();
    }
    public void addProduct(Product p) {
        arr.add(p);
    }
    public void removeProduct(Product p) {
        arr.remove(p);
    }
    public void display() {
        for (Product p : arr) {
            System.out.println(p);
        }
    }

    public ArrayList<Product> getArr() {
        return arr;
    }

    public void setArr(ArrayList<Product> arr) {
        this.arr = arr;
    }
    public void sortArr(){
        arr.sort((o1,o2)->(int)(o1.getId()-o2.getId()));
    }
}
//new Comparator<Product>() {
//            @Override
//            public int compare(Product o1, Product o2) {
//                return (int) (o1.getId()-o2.getId());
//            }
//        };
