package Implements;

import Entities.GlobalDB;
import Entities.Product;
import Entities.PurchaseOrder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PurchaseDAODB {
    private List<PurchaseOrder> purchaseOrders = GlobalDB.getInstance()._PurchaseOrders;
    public static PurchaseDAODB _purchaseOrder;
    public static PurchaseDAODB getInstance() {
        if(_purchaseOrder==null)
            _purchaseOrder=new PurchaseDAODB();
        return _purchaseOrder;
    }
    public boolean isPurchaseOrdersExistById(long id) {
        return purchaseOrders.stream().anyMatch(p-> p.getId()==id);
    }
    public PurchaseOrder getPurchaseExistById(long id)throws Exception
    {
        Optional<PurchaseOrder> p=purchaseOrders.stream().filter(pp -> (pp.getId() == id)).findFirst();
        if(p.isEmpty())
            throw new Exception("PurchaseOrder with ID " + id + " does not exist.");
        return p.get();
    }
    public List<PurchaseOrder> getPurchaseByCustomerId(long id) throws Exception {
        // סינון הרשימה והמרתה לרשימה חדשה
        List<PurchaseOrder> p = purchaseOrders.stream()
                .filter(pp -> pp.getOrderingCustomer().getId() == id)
                .collect(Collectors.toList());
        // בדיקה אם הרשימה ריקה
        if (p.isEmpty()) {
            throw new Exception("PurchaseOrder with ID " + id + " does not exist.");
        }
        return p;
    }

    public void addPurchaseOrder(PurchaseOrder p) {
        purchaseOrders.add(p);
    }

    public void updatePurchaseOrder(PurchaseOrder p) throws Exception {
        try {
            removePurchaseOrder(p.getId());
        }
        catch (Exception e){
            throw  e;
        }
        addPurchaseOrder(p);
    }

    public void removePurchaseOrder(long id) throws Exception {
        boolean i=purchaseOrders.remove(getPurchaseExistById(id));
        if (!i)
            throw new Exception("PurchaseOrder with ID " + id + " does not exist.");
    }


    public List<PurchaseOrder> getAllPurchaseOrders() throws Exception {
        return purchaseOrders;
    }

}
