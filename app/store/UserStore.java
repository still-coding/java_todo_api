package store;

import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import models.User;
import org.bson.types.ObjectId;
import utils.Settings;

import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class UserStore {
    private final Datastore datastore = Morphia.createDatastore(
            MongoClients.create(Settings.getMongoDbUri()), Settings.getMongoDbDatabaseName()
    );

    public Optional<User> create(User newUser) {
        return Optional.of(datastore.save(newUser));
    }

    public Optional<User> retrieve(ObjectId id) {
        Query<User> query = datastore.find(User.class).filter(Filters.eq("_id", id));
        return Optional.ofNullable(query.first());
    }

    public Optional<User> getByUsername(String username) {
        Query<User> query = datastore.find(User.class).filter(Filters.eq("name", username));
        return Optional.ofNullable(query.first());
    }

    public boolean delete(ObjectId id) {
        return datastore.find(User.class).filter(Filters.eq("_id", id)).delete().getDeletedCount() == 1;
    }
}
