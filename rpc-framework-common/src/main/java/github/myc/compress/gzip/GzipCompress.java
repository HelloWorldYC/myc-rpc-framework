package github.myc.compress.gzip;

import github.myc.compress.Compress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 使用 gzip 压缩和解压缩
 */
public class GzipCompress implements Compress {

    private static final int BUFFER_SIZE = 1024 * 4;

    @Override
    public byte[] compress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(bytes);
            // flush() 方法用于将缓冲区的数据推送到压缩流，但不结束压缩流, 可以用于在继续写入数据之前推送已缓冲的数据。
            gzip.flush();
            // finish() 方法用于完成压缩操作并标记压缩流结束。
            gzip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("gzip github.myc.compress error", e);
        }
    }

    @Override
    public byte[] decompress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPInputStream gunzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            // 从压缩数据流 gunzip 中读取数据到缓冲区 buffer 中（相当于解压缩了），并返回实际读取的字节数。
            while((n = gunzip.read(buffer)) > -1) {
                // 将 buffer 中的数据写出到字节数组输出流中
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("gzip decompress error");
        }
    }
}
