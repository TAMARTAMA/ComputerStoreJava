package Interfaces;

import Entities.*;
import Entities.PurchaseOrder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface PurchaseOrdersDAO {
    boolean isPurchaseOrdersExistById(long id);
    PurchaseOrder getPurchaseExistById(long id)throws Exception;
    void addPurchaseOrder(PurchaseOrder p);
    public PurchaseOrder getPurchaseByCustomerId(long id)throws Exception;
    void updatePurchaseOrder(PurchaseOrder p) throws Exception;
    void removePurchaseOrder(long id)throws Exception;
    PurchaseOrder getPurchaseOrderById(long id)throws Exception;
//    Customer getCustomerByIdentity(Customer.Identity id) throws Exception;
    List<PurchaseOrder> getAllPurchaseOrders()throws Exception;

//    ArrayList<PurchaseOrder> getPurchaseOrdersByDate(LocalDate ld)throws Exception;
}
