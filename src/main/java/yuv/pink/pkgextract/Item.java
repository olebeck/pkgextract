package yuv.pink.pkgextract;

public class Item {
    public String name;
    public long offset;
    public long size;
    public byte flags;
    Item(String name, long dataOffset, long dataSize, byte flags) {
        this.name = name;
        this.offset = dataOffset;
        this.size = dataSize;
        this.flags = flags;
    }
}
