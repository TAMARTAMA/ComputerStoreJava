package Facade;

import Entities.Customer;
import Entities.HardwareProduct;
import Entities.Product;
import Entities.SoftwareProduct;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;

import static Facade.Kind.Hardware;

public class AdminFacade extends ClientFacade {
    //Login() כניסה למערכת Administrator אין צורך לבדוק שם משתמש וסיסמא מול מסד הנתונים, אלא יש לבדוק אותם כ Hard Code
    //השם משתמש יהיה תמיד admin@admin.com והסיסמא תהיה תמיד admin
    private long Tvach;


    public boolean Login(String email, String pass) {
        if ((email == "admin@admin.com"
        ) && (pass == "adim")) {
            return true;
        }
        return false;
    }

    public void addNewCustomer(Customer customer) throws Exception {
        try {
            if (customer.getLocalDate().compareTo(LocalDate.now()) > Tvach)
                throw new Exception("Customer Biger/smaller date");
            if (customer.getLocalDate() != customer.getLocalDate())
                throw new Exception("Customer with ID " + customer.getId() + " cant this date");
        } catch (Exception e) {
            throw e;
        }
        customerDAODB.addCustomer(customer);
        String a = String.valueOf(customer.getName().charAt(0));
        customerDAODB.getAllCustomers().values().stream().filter(c -> c.getName().startsWith(a)).forEach(c -> System.out.println(c.toString()));
    }

    public void updateCustomer(Customer customer) throws Exception {
        if (!customerDAODB.isCustomerExistById(customer.getId())) {
            throw new Exception("Customer with ID " + customer.getId() + " does not exist.");
        }
        if (customer.getLocalDate() != customer.getLocalDate())
            throw new Exception("Customer cant change date");

    }

    //•	החזרת כל הלקוחות שנולדו בתאריך מסוים, לבדוק טווח תקין.
    public ArrayList<Customer> getCustomersByDate(LocalDate localDate) throws Exception {
        return customerDAODB.getCustomersByDate(localDate);
    }

    //•	מחיקת לקוח קיים.
    public void removeCustomer(long id) throws Exception {
        customerDAODB.removeCustomer(id);
    }

    //            •	החזרת לקוח לפי קוד מסוים.
    public Customer getCustomerById(long id) throws Exception {
        return customerDAODB.getCustomerById(id);
    }

    //•	המנהל שולט על המוצרים של המערכת. ניהול מלאי.
//            •	הוספת מוצר – בונה מוצר חדש, מקבלת כפרמטר האם המוצר החדש יהיה Hardware  או Software  (מטיפוס enum) ואת הנתונים של המוצר החדש.
    public boolean isProductExistById(long id) {
        return productDAODB.isProductExistById(id);
    }

    public Product getProductById(long id) throws Exception {

        return productDAODB.getProductById(id);
    }

    public Set<Product> getAllProducts() throws Exception {
        return productDAODB.getAllProducts();
    }

    public void addProduct(Product p, Kind k, int n) {
        if (k == Hardware)
            p = new HardwareProduct(p.getId(), p.getName(), p.getDescription(), p.getPricePerUnit(), p.getQuantity(), n);
        else
            p = new SoftwareProduct(p.getId(), p.getName(), p.getDescription(), p.getPricePerUnit(), p.getQuantity(), n);
        productDAODB.addProduct(p);
    }
        public void updateProduct(Product p) throws Exception {
            productDAODB.updateProduct(p);
        }

        public void removeProduct ( long id) throws Exception {
            productDAODB.removeProduct(id);
        }
//            •	המנהל שולט על מצב ההזמנות, אילו הזמנות ניתן למחוק וכו'.

    }
