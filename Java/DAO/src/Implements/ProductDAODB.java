package Implements;

import Entities.Customer;
import Entities.GlobalDB;
import Entities.Product;
import Interfaces.CustomerDAO;
import Interfaces.ProductDAO;

import java.util.Optional;
import java.util.Set;

public class ProductDAODB implements ProductDAO {
    private Set<Product> products = GlobalDB.getInstance()._Products;
    public static ProductDAODB _productDAO;
    public static ProductDAODB getInstance() {
        if(_productDAO==null)
            _productDAO=new ProductDAODB();
        return _productDAO;
    }
    public boolean isProductExistById(long id) {
        return products.stream().anyMatch(p -> (p.getId() == id));
    }

    public Product getProductById(long id) throws Exception {

        Product p=  products.stream().filter(pp -> (pp.getId() == id)).findFirst().get();
        //TODO expitions
        if(p==null)
            throw new Exception("Product with ID " + id + " does not exist.");
        return  p;
    }

    public Set<Product> getAllProducts() throws Exception {
        return products;
    }
    public void addProduct(Product p) {
        products.add(p);
    }

    public void updateProduct(Product p) throws Exception {
        try {
            removeProduct(p.getId());
        }
        catch (Exception e){
            throw  e;
        }
        addProduct(p);
    }

    public void removeProduct(long id) throws Exception {
       boolean i= products.remove(getProductById(id));
       if (!i)
           throw new Exception("Product with ID " + id + " does not exist.");
//        if (!mapCus.containsKey(id)) {
//            throw new Exception("Customer with ID " + id + " does not exist.");
//        }
//        mapCus.remove(id);
    }
}
