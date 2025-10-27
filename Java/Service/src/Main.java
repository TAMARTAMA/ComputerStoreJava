import Entities.*;
import Facade.*;

import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            LoginManager loginManager = new LoginManager();
            AdminFacade adminFacade = (AdminFacade) loginManager.login("admin@admin.com", "admin", ClientTpye.Administrator);

            Customer customer1 = new Customer( "John Doe","elad" ,1,"customer1@example.com","password",1990, 5, 15);
            Customer customer2 = new Customer( "Jane Smith","elad" ,1,"J","1234",1992, 6, 14);
            adminFacade.addNewCustomer(customer1);
            adminFacade.addNewCustomer(customer2);

            customer1.setName("John Updated");
            adminFacade.updateCustomer(customer1);

            Customer fetchedCustomer = adminFacade.getCustomerById(1);
            System.out.println("Fetched Customer: " + fetchedCustomer);

            List<Customer> customersByDate = adminFacade.getCustomersByDate(LocalDate.of(1990, 5, 15));
            System.out.println("Customers born on 1990-05-15: " + customersByDate);

            adminFacade.removeCustomer(2);

            Product product1 = new SoftwareProduct(1l, "Laptop", "High-end gaming laptop", 2000.0f, 10,2);
            Product product2 = new HardwareProduct(2l, "Antivirus", "Premium antivirus software", 50.0f, 100,5);
            adminFacade.addProduct(product1, Kind.Hardware, 2);
            adminFacade.addProduct(product2, Kind.Software, 5);

            Product fetchedProduct = adminFacade.getProductById(1);
            System.out.println("Fetched Product: " + fetchedProduct);

            product1.setPricePerUnit(1800);
            adminFacade.updateProduct(product1);

            adminFacade.removeProduct(2);

            CustomerFacade customerFacade = (CustomerFacade) loginManager.login("customer1@example.com", "password", ClientTpye.Customer);

            customerFacade.addProductToCart(product1);

            PurchaseOrder purchaseOrder = customerFacade.CompletionOrder();
            System.out.println("Order completed: " + purchaseOrder);

            List<PurchaseOrder> customerOrders = customerFacade.getCustomerPurchaseOrders();
            System.out.println("Customer Orders: " + customerOrders);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
