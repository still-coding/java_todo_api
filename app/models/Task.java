package models;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
public class Task {
    private int id;

    private int userId;
    private String name;
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date createdAt;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Task() {
        this.createdAt = new Date();
    }

    public Task(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = new Date();
    }

}
