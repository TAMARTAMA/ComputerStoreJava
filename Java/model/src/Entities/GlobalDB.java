package Entities;

import Entities.Customer;
import Entities.SoftwareProduct;
import java.util.*;

public class GlobalDB {

    public Map<Long, Customer> _Customers;
    public Set<Product> _Products;
    public List<PurchaseOrder> _PurchaseOrders;
    private GlobalDB() {
        _Customers = new HashMap< >();
        _Products = new HashSet< >();
        _PurchaseOrders = new ArrayList< >();
    }
    public static GlobalDB globalDB;

    public static GlobalDB getInstance() {
        if(globalDB==null)
            globalDB=new GlobalDB();
        return globalDB;
    }
}
