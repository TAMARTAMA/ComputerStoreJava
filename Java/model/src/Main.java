package Entities;

import java.time.LocalDate;
import java.util.ArrayList;

import Implements.CustomerDAODB;
import Implements.ProductDAODB;
import Implements.PurchaseDAODB;
import Interfaces.CustomerDAO;
import Interfaces.ProductDAO;


public class Main {
    public static void main(String[] args) {
        CustomerDAODB customerDAO = CustomerDAODB.getInstance()._customerDAO;
        ProductDAODB productDAO = ProductDAODB.getInstance()._productDAO;
        PurchaseDAODB purchaseDAODB = PurchaseDAODB.getInstance()._purchaseOrder;

        try {
            Customer customer1 = new Customer("Alice", "123 Main St", 1001l, "aliceUser", "pass123", 1990, 5, 10);
            Customer customer2 = new Customer("Bob", "456 Park Ave", 1002L, "bobUser", "pass456", 1985, 8, 15);
            customerDAO.addCustomer(customer1);
            customerDAO.addCustomer(customer2);
            System.out.println("Added Customers: " + customerDAO.getAllCustomers());

            System.out.println("Customer by ID 1001: " + customerDAO.getCustomerById(1001L));

            System.out.println("Customer by username and password: " + customerDAO.getCustomerByIdentity(customer2.getIdentity()));

            customer1.setAddress("789 Broadway");
            customerDAO.updateCustomer(customer1);
            System.out.println("Updated Customer: " + customerDAO.getCustomerById(1001L));

            customerDAO.removeCustomer(1002l);
            System.out.println("Remaining Customers: " + customerDAO.getAllCustomers());

            HardwareProduct hProduct = new HardwareProduct(2001L, "Graphics Card", "High-performance graphics card", 500, 10, 24);
            SoftwareProduct sProduct = new SoftwareProduct(3001L, "Antivirus Software", "Protect your computer from viruses", 100, 50, 5);
            productDAO.addProduct(hProduct);
            productDAO.addProduct(sProduct);
            System.out.println("Added Products: " + productDAO.getAllProducts());

            System.out.println("Product by ID 2001: " + productDAO.getProductById(2001L));

            hProduct.setQuantity(hProduct.getQuantity() - 2); 
            productDAO.updateProduct(hProduct);
            System.out.println("Updated Product Quantity: " + productDAO.getProductById(2001L));

            PurchaseOrder order = new PurchaseOrder(1, customer1);
            purchaseDAODB.addPurchaseOrder(order);
            Cart c = new Cart();
            c.addProduct(hProduct); 
            c.addProduct(sProduct);  
            c.getArr().forEach(o -> order.addOrderItem(new OrderItem(o.getId(), o.getQuantity())));
            System.out.println("Created Purchase Order: " + order);


            PurchaseOrder order2 = new PurchaseOrder(2, customer2);
            purchaseDAODB.addPurchaseOrder(order2);
            Cart c2 = new Cart();
            c2.addProduct(sProduct);  
            c2.addProduct(hProduct);  
            c2.getArr().forEach(o -> order.addOrderItem(new OrderItem(o.getId(), o.getQuantity())));
            System.out.println("Created Purchase Order: " + order);

            System.out.println("Orders for Customer 1001: " + purchaseDAODB.getPurchaseByCustomerId(1001L));

            purchaseDAODB.removePurchaseOrder(1);
            System.out.println("Remaining Orders after cleanup: " + purchaseDAODB.getAllPurchaseOrders());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

