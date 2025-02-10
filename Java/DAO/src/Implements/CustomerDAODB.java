package Implements;

import Entities.Customer;
import Entities.GlobalDB;
import Interfaces.CustomerDAO;

import java.time.LocalDate;
import java.util.*;


public  class CustomerDAODB implements CustomerDAO {
    private Map<Long, Customer> mapCus = GlobalDB.getInstance()._Customers;
    public static CustomerDAODB _customerDAO;
    public static CustomerDAODB getInstance() {
        if(_customerDAO==null)
            _customerDAO=new CustomerDAODB();
        return _customerDAO;
    }
    public boolean isCustomerExistById(long id) {
        return mapCus.containsKey(id);
    }

    public boolean isCustomerExistByIdentity(Customer.Identity id) {
        return mapCus.values().stream()
                .anyMatch(customer -> customer.getIdentity().equals(id));

    }
  public   boolean isCustomerExistByIdentity(String email, String pass){
        try{
            getCustomerByIdentity(email,pass);
        }
        catch (Exception e)
        {
            return false;
        }
        return true;
    }
    public void addCustomer(Customer c) {
        mapCus.put(c.getId(), c);
    }

    public void updateCustomer(Customer c) throws Exception {
        if (!mapCus.containsKey(c.getId())) {
            throw new Exception("Customer with ID " + c.getId() + " does not exist.");
        }
        mapCus.put(c.getId(), c);
    }

    public void removeCustomer(long id) throws Exception {
        if (!mapCus.containsKey(id)) {
            throw new Exception("Customer with ID " + id + " does not exist.");
        }
        mapCus.remove(id);
    }

    public Customer getCustomerById(long id) throws Exception {
        if (!mapCus.containsKey(id)) {
            throw new Exception("Customer with ID " + id + " does not exist.");
        }
        return mapCus.get(id);
    }

//    public Customer getCustomerByIdentity(Customer.Identity identity) throws Exception {
//        Optional<Customer> customer = mapCus.values().stream()
//                .filter(c -> c.getIdentity().equals(identity))  // מסננים לקוחות שמתאימים
//                .findFirst();
//        if (customer.isEmpty()) {
//            throw new Exception("Customer with identity " + identity + " not found.");
//        }
//        return customer.get();
//
//    }
public Customer getCustomerByIdentity(String email, String pass) throws Exception {
    Optional<Customer> customer = mapCus.values().stream()
            .filter(c -> c.getIdentity().getPassword().equals(pass)
                    &&c.getIdentity().getUserName().equals(email))  // מסננים לקוחות שמתאימים
            .findFirst();
    if (customer.isEmpty()) {
        throw new Exception("Customer with identity " + email +" " +pass+ " not found.");
    }
    return customer.get();

}

    public HashMap<Long, Customer> getAllCustomers() throws Exception {
        return (HashMap<Long, Customer>) mapCus;
    }

    //TODO if isnt good change!
    public ArrayList<Customer> getCustomersByDate(LocalDate ld) throws Exception {

        return (ArrayList<Customer>) mapCus.values().stream().filter(c -> c.getLocalDate().equals(ld));
    }
}

