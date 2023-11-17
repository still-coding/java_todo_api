package store;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.javatuples.Pair;
import utils.PdfImageUtil;
import utils.Settings;

import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ImageStore {
    private static MongoClient mongoClient = MongoClients.create(Settings.getMongoDbUri());
    private static GridFSBucket gridFSBucket = GridFSBuckets.create(mongoClient.getDatabase(Settings.getMongoDbDatabaseName()), "pdfPages");
    private static GridFSUploadOptions options = new GridFSUploadOptions()
            .metadata(new Document("contentType", "image/png"));

    public ObjectId create(byte[] imageBytes, String filename) {
        try (GridFSUploadStream uploadStream = gridFSBucket.openUploadStream(filename, options)) {
            ObjectId result = uploadStream.getObjectId();
            uploadStream.write(imageBytes);
            uploadStream.flush();
            uploadStream.close();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public void delete(ObjectId fileId) {
        gridFSBucket.delete(fileId);
    }


    public <T> List<ObjectId> createList(List<T> imageList, String fileName) {
        fileName = PdfImageUtil.getFileNameWithoutExtension(fileName);
        List<ObjectId> result = new ArrayList<>(imageList.size());
        for (int i = 0; i < imageList.size(); i++) {
            T image = imageList.get(i);
            byte[] bytes = null;
            if (image instanceof BufferedImage)
                bytes = PdfImageUtil.convertImageToBytes((BufferedImage) image);
            if (image instanceof byte[])
                bytes = (byte[]) image;
            result.add(create(bytes, fileName + "_page" + i + ".png"));
        }
        return result;
    }


    public void deleteList(List<ObjectId> idList) {
        if (idList != null) {
            for (ObjectId objectId : idList) {
                delete(objectId);
            }
        }
    }


    public Pair<String, byte[]> retrieve(ObjectId id) {
        GridFSFile found = gridFSBucket.find(new Document("_id", id)).first();
        if (found != null) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                gridFSBucket.downloadToStream(found.getId(), outputStream);
                return Pair.with(found.getFilename(), outputStream.toByteArray());
            }
            catch (IOException e) {
                return null;
            }
        }
        return null;
    }
}
