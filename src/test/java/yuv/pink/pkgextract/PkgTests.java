package yuv.pink.pkgextract;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class PkgTests {
    @Test
    public final void PkgReadFiles() throws Exception {
        final File inputFile = new File("NPNA00143_00.pkg");
        InputStream inputStream = new FileInputStream(inputFile);
        Pkg.Read(inputStream, (item, reader) -> {
            System.out.println(item.name);
            if(item.size > 0) {
                byte[] itemData = reader.readNBytes((int) item.size);
                System.out.println(new String(itemData, 0, 32));
            }
        });
    }
}
