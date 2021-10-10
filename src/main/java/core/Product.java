package core;

import lombok.Data;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

@Data
@BsonDiscriminator
public class Product {
    @BsonProperty("_id")
    @BsonId
    private ObjectId id;
    private String productName;
    private int price;
}
