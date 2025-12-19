import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private static String cachedKey = null;

    /**
     * @return 
     */
    public static String getApiKey() {
        if (cachedKey != null) return cachedKey;

        // 1) Try environment variable (preferred when running from a shell/CI)
        String k = System.getenv("STOCK_API_KEY");
        if (k != null && !k.isEmpty()) {
            cachedKey = k;
            return cachedKey;
        }

        // 2) Try reading `config.properties` (useful for local development)
        Properties props = new Properties();
        try (InputStream in = new FileInputStream("config.properties")) {
            props.load(in);
            k = props.getProperty("stock.api.key", "").trim();
            if (!k.isEmpty()) cachedKey = k;
            return k;
        } catch (IOException e) {
            // If file doesn't exist or can't be read, return empty string
            return "";
        }
    }
}
