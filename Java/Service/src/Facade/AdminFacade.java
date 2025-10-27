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

    public ArrayList<Customer> getCustomersByDate(LocalDate localDate) throws Exception {
        return customerDAODB.getCustomersByDate(localDate);
    }

    public void removeCustomer(long id) throws Exception {
        customerDAODB.removeCustomer(id);
    }

    public Customer getCustomerById(long id) throws Exception {
        return customerDAODB.getCustomerById(id);
    }

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

    }

