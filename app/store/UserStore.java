package store;

import models.User;

import javax.inject.Singleton;
import java.util.*;

@Singleton
public class UserStore {
    private Map<Integer, User> users = new HashMap<>();

    public Optional<User> create(User newUser) {
        for (User user : users.values()) {
            if (newUser.getName().equals(user.getName()))
                return Optional.empty();
        }
        int id = users.size();
        newUser.setId(id);
        users.put(id, newUser);
        return Optional.ofNullable(newUser);
    }

    public Optional<User> retrieve(int id) {
        return Optional.ofNullable(users.get(id));
    }

    public Optional<User> getByUsername(String username) {
        for (User user : users.values()) {
            if (username.equals(user.getName()))
                return Optional.of(user);
        }
        return Optional.empty();
    }
}
