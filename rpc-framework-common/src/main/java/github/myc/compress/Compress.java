package github.myc.compress;

import github.myc.annotation.SPI;

@SPI
public interface Compress {

    byte[] compress(byte[] bytes);

    byte[] decompress(byte[] bytes);
}
