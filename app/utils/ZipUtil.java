package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.javatuples.Pair;
import play.libs.Files;
import play.libs.Json;
import store.ImageStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    private static final ImageStore imageStore = new ImageStore();
    public static Optional<File> zip(String filename, String extension,  JsonNode jsonNode, List<ObjectId> pageIds) {
        Files.TemporaryFile tempFile = Files.singletonTemporaryFileCreator().create(filename, extension);
        File tempZipFile = tempFile.path().toFile();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(java.nio.file.Files.newOutputStream(tempFile.path()))) {

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonContent = objectMapper.writeValueAsString(jsonNode);
            zipOutputStream.putNextEntry(new ZipEntry(filename + ".json"));
            zipOutputStream.write(jsonContent.getBytes());
            zipOutputStream.closeEntry();
            if (pageIds != null) {
                for (ObjectId pageId : pageIds) {
                    Pair<String, byte[]> image = imageStore.retrieve(pageId);
                    if (image != null) {
                        zipOutputStream.putNextEntry(new ZipEntry(pageId.toHexString() + "_" +  image.getValue0()));
                        zipOutputStream.write(image.getValue1());
                        zipOutputStream.closeEntry();
                    }
                }
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.of(tempZipFile);
    }


    public static Pair<Optional<JsonNode>, Optional<List<byte[]>>> unzip(File zip) {
        JsonNode jsonTask = null;
        List<byte[]> images = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                String fileName = zipEntry.getName();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                byte[] fileContent = baos.toByteArray();
                // TODO figure out better way
                if (fileName.endsWith("json"))
                {
                    jsonTask = Json.parse(fileContent);
                }
                if (fileName.endsWith("png"))
                {
                    images.add(fileContent);
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        } catch (IOException exc) {
            return new Pair(Optional.empty(), Optional.empty());
        }
        return new Pair(Optional.ofNullable(jsonTask), Optional.of(images));
    }

}
