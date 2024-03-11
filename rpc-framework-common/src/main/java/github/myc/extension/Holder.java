package github.myc.extension;

/**
 * 该类作为缓存使用，存储已经加载的扩展类
 * @param <T>
 */
public class Holder<T> {

    // 使用 volatile 修饰，该变量存入主存，对所有线程都可见，共享
    private volatile T value;

    public T get(){
        return value;
    }

    public void set(T value){
        this.value = value;
    }
}
