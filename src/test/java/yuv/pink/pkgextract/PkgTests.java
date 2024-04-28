package yuv.pink.pkgextract;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PkgTests {
    @Test
    public final void PkgReadFiles() throws Exception {
        final File inputFile = new File("NPNA00143_00.pkg");
        InputStream inputStream = new FileInputStream(inputFile);
        Pkg.Read(inputStream, (item, reader) -> {
            System.out.println("item.name: "+item.name);
            if(item.size > 0) {
                byte[] itemData = new byte[(int) item.size];
                readBytes(reader, itemData);
                System.out.println(new String(itemData, 0, 32));
            }
        });
    }

    private static void readBytes(InputStream stream, byte[] out) throws IOException {
        int off = 0;
        int n = out.length;
        while(off < out.length) {
            int read = stream.read(out, off, n);
            n -= read;
            off += read;
        }
    }
}
