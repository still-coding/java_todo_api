package models;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
public class User {
    private int id;
    private String name;

    private String password;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
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



    public User(int id, String name, String password) {
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
