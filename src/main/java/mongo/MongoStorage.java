package mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import core.Product;
import core.Store;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.mongodb.client.model.Accumulators.avg;
import static com.mongodb.client.model.Accumulators.max;
import static com.mongodb.client.model.Accumulators.min;
import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.lookup;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.unwind;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Updates.set;
import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoStorage {
    private static final CodecRegistry POJO_CODEC_REGISTRY = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));
    private static final MongoClientSettings SETTINGS = MongoClientSettings.builder()
            .codecRegistry(POJO_CODEC_REGISTRY).build();

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Store> stores;
    private final MongoCollection<Product> products;
    private final MongoCollection<Document> documentsStores;

    public MongoStorage() {
        mongoClient = MongoClients.create(SETTINGS);
        database = mongoClient.getDatabase("database");
        stores = database.getCollection("stores", Store.class);
        products = database.getCollection("products", Product.class);
        documentsStores = database.getCollection("stores");
        stores.deleteMany(new Document());
        products.deleteMany(new Document());
    }

    protected void shutdown() {
        mongoClient.close();
    }

    protected void addStore(Store store) {
        if (isStoreExists(store)) {
            System.out.println("Такой магазин уже добавлен");
        } else {
            stores.insertOne(store);
        }
    }

    private boolean isStoreExists(Store store) {
        return stores.find(eq("storeName", store.getStoreName())).first() != null;
    }

    protected void addProduct(Product product) {
        if (isProductExists(product)) {
            System.out.println("Такой продукт уже добавлен");
        } else {
            products.insertOne(product);
        }
    }

    private boolean isProductExists(Product product) {
        return products.find(eq("productName", product.getProductName())).first() != null;
    }

    protected void addProductIntoStore(String productName, String storeName) {
        try {
            addProductIntoStoreViaMongo(productName, storeName);
        } catch (NullPointerException ex) {
            System.out.println("Магазина или продукта не существует");
        }
    }

    protected void addProductIntoStoreViaMongo(String productName, String storeName)
            throws NullPointerException {
        Product product = products.find(eq("productName", productName)).first();
        Store store = stores.find(eq("storeName", storeName)).first();
        List<String> productsToUpdate = Objects.requireNonNull(store).getProducts();
        productsToUpdate.add(Objects.requireNonNull(product).getProductName());
        stores.updateOne(eq("storeName", storeName), set("products", productsToUpdate));
    }

    protected void overallNumberOfProducts() {
        List<Document> documentList = documentsStores.aggregate(
                getPipelineForOverallNumberOfProducts())
                .into(new ArrayList<>());
        for (Document document : documentList) {
            System.out.println("Общее количество наименований товаров в магазине " +
                    document.get("_id") + " - " + document.get("count") + " позиции(й).");
        }
    }

    private List<Bson> getPipelineForOverallNumberOfProducts() {
        Bson group = group("$storeName", sum("count", 1));
        List<Bson> resultPipeline = new ArrayList<>(getPipelineForJoinCollections());
        resultPipeline.add(group);
        return resultPipeline;
    }

    private List<Bson> getPipelineForJoinCollections() {
        Bson lookup = lookup("products", "products", "productName", "products");
        Bson unwind = unwind("$products");
        Bson exclude = project(exclude("_t", "products._id", "products._t"));
        return asList(lookup, unwind, exclude);
    }

    protected void getAveragePrice() {
        List<Document> documentList = documentsStores.aggregate(
                getPipelineForAveragePrice())
                .into(new ArrayList<>());
        for (Document document : documentList) {
            System.out.println("Средняя цена в магазине " + document.get("_id")
                    + " - " + document.get("avg") + " рублей.");
        }
    }

    private List<Bson> getPipelineForAveragePrice() {
        Bson group = group("$storeName", avg("avg", "$products.price"));
        List<Bson> resultPipeline = new ArrayList<>(getPipelineForJoinCollections());
        resultPipeline.add(group);
        return resultPipeline;
    }

    protected void getMostExpensiveProduct() {
        List<Document> documentList = documentsStores.aggregate(
                getPipelineForMostExpensiveProduct())
                .into(new ArrayList<>());
        for (Document document : documentList) {
            System.out.println("Самые дорогие товары в магазине " + document.get("_id") + " - "
                    + getListOfProductsAsString(document, "max") + "цена - " + document.get("max") + " рублей.");
        }
    }

    private List<Bson> getPipelineForMostExpensiveProduct() {
        Bson group = group("$storeName", max("max", "$products.price"));
        List<Bson> resultPipeline = new ArrayList<>(getPipelineForJoinCollections());
        resultPipeline.add(group);
        return resultPipeline;
    }

    private String getListOfProductsAsString(Document document, String type) {
        int priceToFilter = (int) document.get(type);
        List<Document> filteredCollection = documentsStores
                .aggregate(getPipelineWithFilter(priceToFilter))
                .into(new ArrayList<>());
        StringBuilder builder = new StringBuilder();
        for (Document doc : filteredCollection) {
            Document newDocument = (Document) doc.get("products");
            builder.append(newDocument.get("productName"));
            builder.append(", ");
        }
        return builder.toString();
    }

    private List<Bson> getPipelineWithFilter(int price) {
        Bson filter = match(eq("products.price", price));
        List<Bson> resultPipeline = new ArrayList<>(getPipelineForJoinCollections());
        resultPipeline.add(filter);
        return resultPipeline;
    }

    protected void getMostCheapProduct() {
        List<Document> documentList = documentsStores
                .aggregate(getPipelineForCheapExpensiveProduct())
                .into(new ArrayList<>());
        for (Document document : documentList) {
            System.out.println("Самые дешевые товары в магазине " + document.get("_id") + " - "
                    + getListOfProductsAsString(document, "min") + "цена - " + document.get("min") + " рублей.");
        }
    }

    private List<Bson> getPipelineForCheapExpensiveProduct() {
        Bson group = group("$storeName", min("min", "$products.price"));
        List<Bson> resultPipeline = new ArrayList<>(getPipelineForJoinCollections());
        resultPipeline.add(group);
        return resultPipeline;
    }

    protected void getNumberOfProductsUnder100() {
        List<Document> documentList = documentsStores.aggregate(
                getPipelineForNumberOfProductsUnder100())
                .into(new ArrayList<>());
        for (Document document : documentList) {
            System.out.println("Количество товаров дешевле 100 рублей в магазине " +
                    document.get("_id") + " - " + document.get("count") + " позиций.");
        }
    }

    private List<Bson> getPipelineForNumberOfProductsUnder100() {
        Bson match = match(lte("products.price", 100));
        Bson group = group("$storeName", sum("count", 1));
        List<Bson> resultPipeline = new ArrayList<>(getPipelineForJoinCollections());
        resultPipeline.add(match);
        resultPipeline.add(group);
        return resultPipeline;
    }

    protected void showAllCollections() {
        System.out.println("Коллекция магазинов:");
        stores.find().forEach((Consumer<Store>) System.out::println);
        System.out.println("\nКоллекция продуктов:");
        products.find().forEach((Consumer<Product>) System.out::println);
    }
}
