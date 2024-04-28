package yuv.pink.pkgextract;

import java.io.IOException;
import java.io.InputStream;

public interface ItemHandler {
    boolean call(Item item, InputStream reader) throws IOException;
}
