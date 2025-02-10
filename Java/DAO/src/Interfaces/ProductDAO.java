package Interfaces;

import Entities.Customer;
import Entities.Product;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public interface ProductDAO {
    boolean isProductExistById(long id);
    Product getProductById(long id)throws Exception;
//    Customer getCustomerByIdentity(Customer.Identity id) throws Exception;
    Set<Product> getAllProducts()throws Exception;
//    ArrayList<Product> getProductsByDate(LocalDate ld)throws Exception;
    void addProduct(Product p);
    void updateProduct(Product p) throws Exception;
    void removeProduct(long id)throws Exception;
}
