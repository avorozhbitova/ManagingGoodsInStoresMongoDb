package mongo;

import core.Product;
import core.Store;

public class MongoService {
    private final MongoStorage mongoStorage = new MongoStorage();

    public void addStore(String name) {
        Store store = new Store();
        store.setStoreName(name);
        mongoStorage.addStore(store);
    }

    public void addProduct(String name, int price) {
        Product product = new Product();
        product.setProductName(name);
        product.setPrice(price);
        mongoStorage.addProduct(product);
    }

    public void addProductIntoStore(String productName, String storeName) {
        mongoStorage.addProductIntoStore(productName, storeName);
    }

    public void getStatistics() {
        mongoStorage.overallNumberOfProducts();
        mongoStorage.getAveragePrice();
        mongoStorage.getMostExpensiveProduct();
        mongoStorage.getMostCheapProduct();
        mongoStorage.getNumberOfProductsUnder100();
    }

    public void shutdown() {
        mongoStorage.shutdown();
    }
}
