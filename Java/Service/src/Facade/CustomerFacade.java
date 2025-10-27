package Facade;

import Entities.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CustomerFacade extends ClientFacade {
    private Customer customer;
    private Cart cart;
    public boolean Login(String username, String password) {
        if (customerDAODB.isCustomerExistByIdentity(username, password))
            try {
                this.customer =customerDAODB.getCustomerByIdentity(username,password);
            }
        catch(Exception e){
                e.printStackTrace();
                return false;
        }
        return true;
    }

    public Customer getCustomerDetails()  {
        return customer;
    }

    public void updateCustomer(Customer customer2) throws Exception {
        if (customer.getLocalDate() != customer2.getLocalDate())
            throw new Exception("Customer with ID " + customer2.getId() + " cant change date");
        customerDAODB.updateCustomer(customer2);
    }

    public List<PurchaseOrder> getCustomerPurchaseOrders() throws Exception {
        return purchaseDAODB.getPurchaseByCustomerId(customer.getId());
    }

    public void addProductToCart(Product product)  {
        cart.addProduct(product);
    }
    public void removeProductFromCart(Product product)  {
        cart.removeProduct(product);
    }
       public PurchaseOrder CompletionOrder() {

           cart.sortArr();
           ArrayList<Product> arr = cart.getArr();
           boolean flag = false;
           long currentId = -1;
           int count = 0;
           //TODO fix id
           PurchaseOrder po = new PurchaseOrder(100, customer);
           for (Product p : arr) {
               if (p.getId() != currentId) {
                   if (!p.equals(arr.get(0))) {
                       OrderItem orderItem = new OrderItem(currentId, count);
                       po.addOrderItem(orderItem);
                   }
                   currentId = p.getId();
                   count = 0;
               } else {
                   count++;
               }
               if (p.equals(arr.get(arr.size()-1))) {
                   OrderItem orderItem = new OrderItem(currentId, count);
                   po.addOrderItem(orderItem);
               }

           }
           purchaseDAODB.addPurchaseOrder(po);
           return po;
       }
}

