package store;

import com.mongodb.client.MongoClients;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperators;
import dev.morphia.query.updates.UpdateOperators.*;
import models.Task;
import models.User;
import org.bson.types.ObjectId;
import utils.Settings;

import javax.inject.Singleton;
import java.util.*;

@Singleton
public class TaskStore {
//    private Map<Long, Task> tasks = new HashMap<>();
    private final Datastore datastore = Morphia.createDatastore(
            MongoClients.create(Settings.getMongoDbUri()), Settings.getMongoDbDatabaseName()
    );
    public Optional<Task> create(Task newTask) {
        return Optional.of(datastore.save(newTask));
    }

    public Optional<Task> retrieve(ObjectId id) {
        Query<Task> query = datastore.find(Task.class).filter(Filters.eq("_id", id));
        return Optional.ofNullable(query.first());
    }

    public List<Task> retrieveAll(ObjectId userId) {
        Query<Task> query = datastore.find(Task.class).filter(Filters.eq("userId", userId));
        return query.iterator().toList();
    }

    public Optional<Task> update(Task task) {
        String id = task.getId();
        Query<Task> query = datastore.find(Task.class).filter(Filters.eq("_id", new ObjectId(id)));
        if (query.count() != 1)
            return Optional.empty();
        UpdateResult results = query.update(
                UpdateOperators.set("name", task.getName()),
                UpdateOperators.set("description", task.getDescription()),
                UpdateOperators.set("labels", task.getLabels())
        ).execute();
        return Optional.ofNullable(datastore.find(Task.class).filter(Filters.eq("_id", new ObjectId(id))).first());
    }

    public boolean delete(ObjectId id) {
        return datastore.find(Task.class).filter(Filters.eq("_id", id)).delete().getDeletedCount() == 1;
    }
}
