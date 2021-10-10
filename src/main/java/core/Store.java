package core;

import lombok.Data;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@Data
@BsonDiscriminator
public class Store {
    @BsonProperty("_id")
    @BsonId
    private ObjectId id;
    private String storeName;
    private List<String> products = new ArrayList<>();
}
