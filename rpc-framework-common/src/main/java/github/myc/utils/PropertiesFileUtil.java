package github.myc.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;


@Slf4j
public final class PropertiesFileUtil {

    private PropertiesFileUtil() {
    }

    public static Properties readPropertiesFile(String fileName) {
        // 获取当前线程的上下文类加载器的资源根路径
        URL url  = Thread.currentThread().getContextClassLoader().getResource("");
        String rpcConfigPath = "";
        if(url != null) {
            rpcConfigPath = url.getPath() + fileName;
        }
        Properties properties = null;
        try (InputStreamReader inputStreamReader = new InputStreamReader(
                new FileInputStream(rpcConfigPath), StandardCharsets.UTF_8)) {
            properties = new Properties();
            properties.load(inputStreamReader);
        } catch (IOException e){
            log.error("occur github.myc.exception when read properties file [{}]", fileName);
        }
        return properties;
    }
}
