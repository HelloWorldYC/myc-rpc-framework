package github.myc.utils;

public class RuntimeUtil {

    /**
     * 获取 CPU 的核心数
     * @return CPU 的核心数
     */
    public static int cpus() {
        return Runtime.getRuntime().availableProcessors();
    }
}
