import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
// uhs d

public class Config {
    private static String cachedKey = null;

    public static String getApiKey() {
        if (cachedKey != null) return cachedKey;

        String k = System.getenv("STOCK_API_KEY");
        if (k != null && !k.isEmpty()) {
            cachedKey = k;
            return cachedKey;
        }

        Properties props = new Properties();
        try (InputStream in = new FileInputStream("config.properties")) {
            props.load(in);
            k = props.getProperty("stock.api.key", "").trim();
            if (!k.isEmpty()) cachedKey = k;
            return k;
        } catch (IOException e) {
            return "";
        }
    }
}
