package utils;

import io.github.cdimascio.dotenv.Dotenv;

public class Settings {
    private static Dotenv dotenv = Dotenv.load();

    public Settings(){
        this.dotenv = Dotenv.load();
    }

    public static String getJwtSecretKey() {
        return dotenv.get("JWT_SECRET_KEY");
    }

    public static int getJwtExpirationMinutes() {
        return Integer.parseInt(dotenv.get("JWT_EXPIRATION_MINUTES"));
    }

    public static String getMongoDbUri() {
        return dotenv.get("MONGODB_URI");
    }

    public static String getMongoDbDatabaseName() {
        return dotenv.get("MONGODB_DB_NAME");
    }

}
