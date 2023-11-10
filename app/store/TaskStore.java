package store;

import models.Task;
import models.User;

import java.util.*;

public class TaskStore {
    private Map<Integer, Task> tasks = new HashMap<>();

    public Optional<Task> create(Task task) {
        int id = tasks.size();
        task.setId(id);
        tasks.put(id, task);
        return Optional.ofNullable(task);
    }

    public Optional<Task> retrieve(int id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public Set<Task> retrieveAll(int user_id) {
        HashSet<Task> result = new HashSet<>();
        for (Task task : tasks.values()) {
            if (task.getUserId() == user_id)
                result.add(task);
        }
        return result;
    }

    public Optional<Task> update(Task task) {
        int id = task.getId();
        if (tasks.containsKey(id)) {
            tasks.put(id, task);
            return Optional.ofNullable(task);
        }
        return Optional.empty();
    }

    public boolean delete(int id) {
        return tasks.remove(id) != null;
    }
}
