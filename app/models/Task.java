package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity("tasks")
public class Task {
    @Id
    private ObjectId id;

    private ObjectId userId;
    private String name;
    private String description;

    private HashSet<String> labels;

    private String pdfName;
    private List<ObjectId> pdfPages;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date createdAt;


    public String getId() {
        return id.toHexString();
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUserId() {
        return userId.toHexString();
    }

    public void setUserId(ObjectId userId) {
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


    public HashSet<String> getLabels() {
        return labels;
    }

    public void setLabels(HashSet<String> labels) {
        this.labels = labels;
    }

    public List<String> getPdfPages() {
        if (pdfPages == null)
            return new ArrayList<String>();
        return pdfPages.stream()
                .map(pId -> pId.toHexString())
                .collect(Collectors.toList());
    }

    public void setPdfPages(List<ObjectId> pdfPages) {
        this.pdfPages = pdfPages;
    }

    public String getPdfName() {
        return pdfName;
    }

    public void setPdfName(String pdfName) {
        this.pdfName = pdfName;
    }

    public Task() {
        this.createdAt = new Date();
    }

    public List<ObjectId> truePdfPages() {
        return pdfPages;
    }

    public ObjectId trueId() {
        return id;
    }

    public ObjectId trueUserId() {
        return userId;
    }
}
