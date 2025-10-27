
package Interfaces;

import Entities.Customer;

import java.time.LocalDate;
import java.util.*;

public interface CustomerDAO {
     boolean isCustomerExistById(long id);
     boolean isCustomerExistByIdentity(String email, String pass);
     void addCustomer(Customer c);
     void updateCustomer(Customer c) throws Exception;
     void removeCustomer(long id)throws Exception;
     Customer getCustomerById(long id)throws Exception;
     Customer getCustomerByIdentity(String email, String pass) throws Exception;
     HashMap<Long, Customer> getAllCustomers()throws Exception;
     ArrayList<Customer> getCustomersByDate(LocalDate ld)throws Exception;
}

