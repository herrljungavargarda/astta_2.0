package se.herrljunga.astta.utils;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;

public class ConfigLoader {
    public static Config loadConfig() {
        Yaml yaml = new Yaml();
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream("config.yaml")) {
            if (in == null) {
                throw new RuntimeException("config.yaml not found in classpath");
            }
            return yaml.loadAs(in, Config.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
}

