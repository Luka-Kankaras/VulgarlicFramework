package main;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class EngineProperties {

    private static String engineConfigPath;
    private static Properties properties;

    public static void init(String engineConfigPath) {
        EngineProperties.engineConfigPath = engineConfigPath;
        EngineProperties.properties = new Properties();
        try(FileInputStream inputStream = new FileInputStream(engineConfigPath)) {
            properties.load(inputStream);
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static String get(String propertyName) {
        return properties.getProperty(propertyName);
    }

    public static String get(String propertyName, String defaultValue) {
        return properties.getProperty(propertyName, defaultValue);
    }

    public static int getI(String propertyName) {
        try {
            return Integer.parseInt(properties.getProperty(propertyName));
        }
        catch (NumberFormatException exception) {
            exception.printStackTrace();
            return 0;
        }
    }

    public static int getI(String propertyName, String defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(propertyName, defaultValue));
        }
        catch (NumberFormatException exception) {
            exception.printStackTrace();
            return 0;
        }
    }

    public static double getD(String propertyName) {
        try {
            return Double.parseDouble(properties.getProperty(propertyName));
        }
        catch (NumberFormatException exception) {
            exception.printStackTrace();
            return 0;
        }
    }

    public static double getD(String propertyName, String defaultValue) {
        try {
            return Double.parseDouble(properties.getProperty(propertyName, defaultValue));
        }
        catch (NumberFormatException exception) {
            exception.printStackTrace();
            return 0;
        }
    }

    public static double getF(String propertyName) {
        try {
            return Float.parseFloat(properties.getProperty(propertyName));
        }
        catch (NumberFormatException exception) {
            exception.printStackTrace();
            return 0;
        }
    }

    public static double getF(String propertyName, String defaultValue) {
        try {
            return Float.parseFloat(properties.getProperty(propertyName, defaultValue));
        }
        catch (NumberFormatException exception) {
            exception.printStackTrace();
            return 0;
        }
    }

}
