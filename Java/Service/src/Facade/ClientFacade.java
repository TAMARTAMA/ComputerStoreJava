package Facade;
import Implements.*;
public abstract class ClientFacade {
    protected CustomerDAODB customerDAODB =CustomerDAODB.getInstance();
    protected ProductDAODB productDAODB =ProductDAODB.getInstance();
    protected PurchaseDAODB purchaseDAODB =PurchaseDAODB.getInstance();
    public abstract boolean  Login(String email, String pass);
}
