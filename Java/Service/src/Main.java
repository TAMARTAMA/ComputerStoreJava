//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.


import Entities.*;
import Facade.*;

import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            // מנהל המערכת
            LoginManager loginManager = new LoginManager();
            AdminFacade adminFacade = (AdminFacade) loginManager.login("admin@admin.com", "admin", ClientTpye.Administrator);

            // הוספת לקוחות
            Customer customer1 = new Customer( "John Doe","elad" ,1,"customer1@example.com","password",1990, 5, 15);
            Customer customer2 = new Customer( "Jane Smith","elad" ,1,"J","1234",1992, 6, 14);
            adminFacade.addNewCustomer(customer1);
            adminFacade.addNewCustomer(customer2);

            // עדכון לקוח
            customer1.setName("John Updated");
            adminFacade.updateCustomer(customer1);

            // קבלת לקוח לפי ID
            Customer fetchedCustomer = adminFacade.getCustomerById(1);
            System.out.println("Fetched Customer: " + fetchedCustomer);

            // החזרת כל הלקוחות שנולדו בתאריך מסוים
            List<Customer> customersByDate = adminFacade.getCustomersByDate(LocalDate.of(1990, 5, 15));
            System.out.println("Customers born on 1990-05-15: " + customersByDate);

            // מחיקת לקוח
            adminFacade.removeCustomer(2);

            // הוספת מוצרים
            Product product1 = new SoftwareProduct(1l, "Laptop", "High-end gaming laptop", 2000.0f, 10,2);
            Product product2 = new HardwareProduct(2l, "Antivirus", "Premium antivirus software", 50.0f, 100,5);
            adminFacade.addProduct(product1, Kind.Hardware, 2);
            adminFacade.addProduct(product2, Kind.Software, 5);

            // קבלת מוצר לפי ID
            Product fetchedProduct = adminFacade.getProductById(1);
            System.out.println("Fetched Product: " + fetchedProduct);

            // עדכון מוצר
            product1.setPricePerUnit(1800);
            adminFacade.updateProduct(product1);

            // מחיקת מוצר
            adminFacade.removeProduct(2);

            // לקוח רגיל
            CustomerFacade customerFacade = (CustomerFacade) loginManager.login("customer1@example.com", "password", ClientTpye.Customer);

            // הוספת מוצר לעגלה
            customerFacade.addProductToCart(product1);

            // הוספת הזמנה
            PurchaseOrder purchaseOrder = customerFacade.CompletionOrder();
            System.out.println("Order completed: " + purchaseOrder);

            // הצגת כל ההזמנות של הלקוח
            List<PurchaseOrder> customerOrders = customerFacade.getCustomerPurchaseOrders();
            System.out.println("Customer Orders: " + customerOrders);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}