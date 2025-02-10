package Entities;

import java.time.LocalDate;
import java.util.ArrayList;

import Implements.CustomerDAODB;
import Implements.ProductDAODB;
import Implements.PurchaseDAODB;
import Interfaces.CustomerDAO;
import Interfaces.ProductDAO;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
//public class Main {
    //    public static void main(String[] args) {
//        HardwareProduct hardwareProduct = new HardwareProduct(1, "Laptop", "High-performance laptop", 1500, 10, 2);
//        SoftwareProduct softwareProduct = new SoftwareProduct(2, "Antivirus", "Antivirus software for protection", 100, 5, 1);
//
//        // הדפסת מוצרים
//        System.out.println("Hardware Entities.Customer.Entities.Product: " + hardwareProduct);
//        System.out.println("Software Entities.Customer.Entities.Product: " + softwareProduct);
//
//        // יצירת לקוח
//        Customer customer=new Customer("mimi","rambam",21345,"abcd","1234",2000,1,1);
//
//        // הדפסת פרטי לקוח
//        System.out.println("Entities.Customer: " + customer);
//
//        // יצירת פריט בהזמנה
//        OrderItem orderItem1 = new OrderItem((int) hardwareProduct.getId(), 1);
//        OrderItem orderItem2 = new OrderItem((int) softwareProduct.getId(), 2);
//
//        // יצירת הזמנה והוספת פריטים להזמנה
//        ArrayList<OrderItem> orderItems = new ArrayList<>();
//        orderItems.add(orderItem1);
//        orderItems.add(orderItem2);
//
//        PurchaseOrder purchaseOrder = new PurchaseOrder(1, orderItems,customer);
//
//        // הדפסת פרטי ההזמנה
//        System.out.println("Purchase Order: " + purchaseOrder);
////        CustomerDAODB.getInstance().CustomerDAO;
//        CustomerDAODB.getInstance().customerDAO;
//        }
    public class Main {
        public static void main(String[] args) {
            // יצירת DAO-ים
            CustomerDAODB customerDAO = CustomerDAODB.getInstance()._customerDAO;
            ProductDAODB productDAO = ProductDAODB.getInstance()._productDAO;
            PurchaseDAODB purchaseDAODB = PurchaseDAODB.getInstance()._purchaseOrder;
//            OrderDAO orderDAO = new OrderDAODB();

            try {
                // בדיקה של מחלקת Customer
                Customer customer1 = new Customer("Alice", "123 Main St", 1001l, "aliceUser", "pass123", 1990, 5, 10);
                Customer customer2 = new Customer("Bob", "456 Park Ave", 1002L, "bobUser", "pass456", 1985, 8, 15);
                customerDAO.addCustomer(customer1);
                customerDAO.addCustomer(customer2);
                System.out.println("Added Customers: " + customerDAO.getAllCustomers());

                // בדיקת שליפת לקוח לפי ID
                System.out.println("Customer by ID 1001: " + customerDAO.getCustomerById(1001L));

                // בדיקת שליפת לקוח לפי שם משתמש וסיסמא
                System.out.println("Customer by username and password: " + customerDAO.getCustomerByIdentity(customer2.getIdentity()));

                // עדכון פרטי לקוח
                customer1.setAddress("789 Broadway");
                customerDAO.updateCustomer(customer1);
                System.out.println("Updated Customer: " + customerDAO.getCustomerById(1001L));

                // מחיקת לקוח
                customerDAO.removeCustomer(1002l);
                System.out.println("Remaining Customers: " + customerDAO.getAllCustomers());

                // בדיקה של מחלקת Product
                HardwareProduct hProduct = new HardwareProduct(2001L, "Graphics Card", "High-performance graphics card", 500, 10, 24);
                SoftwareProduct sProduct = new SoftwareProduct(3001L, "Antivirus Software", "Protect your computer from viruses", 100, 50, 5);
                productDAO.addProduct(hProduct);
                productDAO.addProduct(sProduct);
                System.out.println("Added Products: " + productDAO.getAllProducts());

                // שליפת מוצר לפי ID
                System.out.println("Product by ID 2001: " + productDAO.getProductById(2001L));

                // בדיקת כמות מלאי ועידכון כמות
                hProduct.setQuantity(hProduct.getQuantity() - 2); // הפחתת 2 יחידות מהמלאי
                productDAO.updateProduct(hProduct);
                System.out.println("Updated Product Quantity: " + productDAO.getProductById(2001L));

                // יצירת הזמנה
                PurchaseOrder order = new PurchaseOrder(1, customer1);
                purchaseDAODB.addPurchaseOrder(order);
                Cart c = new Cart();
                c.addProduct(hProduct);  // הוספת 2 יחידות של חומרה
                c.addProduct(sProduct);  // הוספת 2 יחידות של חומרה
                c.getArr().forEach(o -> order.addOrderItem(new OrderItem(o.getId(), o.getQuantity())));
                System.out.println("Created Purchase Order: " + order);


                PurchaseOrder order2 = new PurchaseOrder(2, customer2);
                purchaseDAODB.addPurchaseOrder(order2);
                Cart c2 = new Cart();
                c2.addProduct(sProduct);  // הוספת 2 יחידות של חומרה
                c2.addProduct(hProduct);  // הוספת 2 יחידות של חומרה
                c2.getArr().forEach(o -> order.addOrderItem(new OrderItem(o.getId(), o.getQuantity())));
                System.out.println("Created Purchase Order: " + order);
                // בדיקת שליפת הזמנות ללקוח מסוים
                System.out.println("Orders for Customer 1001: " + purchaseDAODB.getPurchaseByCustomerId(1001L));

                // בדיקת מחיקת הזמנות ישנות
                purchaseDAODB.removePurchaseOrder(1);
                System.out.println("Remaining Orders after cleanup: " + purchaseDAODB.getAllPurchaseOrders());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

