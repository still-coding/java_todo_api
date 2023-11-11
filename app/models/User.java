package models;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;


@Entity("users")
public class User {
    private String name;
    @Id
    private ObjectId id;

    private String password;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date createdAt;

    public String getId() {
        return id.toHexString();
    }

    public void setId(ObjectId id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public Date getCreatedAt() {
        return createdAt;
    }


    public User() {
        this.createdAt = new Date();
    }



    public User(ObjectId id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.createdAt = new Date();
    }

    @Override
    public String toString() {
        return "User{id=" + id + '\'' +
                ", name='" + name + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
