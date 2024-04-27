package yuv.pink.pkgextract;

import java.io.IOException;
import java.io.InputStream;

public interface ItemHandler {
    void call(Item item, InputStream reader) throws IOException;
}
