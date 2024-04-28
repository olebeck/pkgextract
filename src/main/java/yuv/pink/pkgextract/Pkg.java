package yuv.pink.pkgextract;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

public class Pkg {
    public static final int PKG_TYPE_VITA_APP = 0;
    public static final int PKG_TYPE_VITA_DLC = 1;
    public static final int PKG_TYPE_VITA_PATCH = 2;
    public static final int PKG_TYPE_VITA_PSM = 3;
    public static final int PKG_TYPE_PSP = 4;
    public static final int PKG_TYPE_PSX = 5;
    private static final byte[] key_pkg_ps3_key = {0x2e, 0x7b, 0x71, (byte) 0xd7, (byte) 0xc9, (byte) 0xc9, (byte) 0xa1, 0x4e, (byte) 0xa3, 0x22, 0x1f, 0x18, (byte) 0x88, 0x28, (byte) 0xb8, (byte) 0xf8};
    private static final byte[] key_pkg_psp_key = {0x07, (byte) 0xf2, (byte) 0xc6, (byte) 0x82, (byte) 0x90, (byte) 0xb5, 0x0d, 0x2c, 0x33, (byte) 0x81, (byte) 0x8d, 0x70, (byte) 0x9b, 0x60, (byte) 0xe6, 0x2b};
    private static final byte[] key_pkg_vita_2 = {(byte) 0xe3, 0x1a, 0x70, (byte) 0xc9, (byte) 0xce, 0x1d, (byte) 0xd7, 0x2b, (byte) 0xf3, (byte) 0xc0, 0x62, 0x29, 0x63, (byte) 0xf2, (byte) 0xec, (byte) 0xcb};
    private static final byte[] key_pkg_vita_3 = {0x42, 0x3a, (byte) 0xca, 0x3a, 0x2b, (byte) 0xd5, 0x64, (byte) 0x9f, (byte) 0x96, (byte) 0x86, (byte) 0xab, (byte) 0xad, 0x6f, (byte) 0xd8, (byte) 0x80, 0x1f};
    private static final byte[] key_pkg_vita_4 = {(byte) 0xaf, 0x07, (byte) 0xfd, 0x59, 0x65, 0x25, 0x27, (byte) 0xba, (byte) 0xf1, 0x33, (byte) 0x89, 0x66, (byte) 0x8b, 0x17, (byte) 0xd9, (byte) 0xea};

    public static void Read(InputStream inputStream, ItemHandler itemHandler) throws Exception {
        DataInputStream dataInputStream = new DataInputStream(inputStream);

        byte[] magic = new byte[4];
        dataInputStream.readFully(magic);
        short revision = dataInputStream.readShort();
        short type = dataInputStream.readShort();
        int metaOffset = dataInputStream.readInt();
        int metaCount = dataInputStream.readInt();
        int metaSize = dataInputStream.readInt();
        int itemCount = dataInputStream.readInt();
        long totalSize = dataInputStream.readLong();
        long encryptedOffset = dataInputStream.readLong();
        long encryptedSize = dataInputStream.readLong();
        byte[] contentIDBytes = new byte[36];
        dataInputStream.readFully(contentIDBytes);
        String contentID = new String(contentIDBytes);
        skipBytes(dataInputStream, 12);

        byte[] digest = new byte[16];
        dataInputStream.readFully(digest);
        byte[] iv = new byte[16];
        dataInputStream.readFully(iv);
        skipBytes(dataInputStream, 103);
        int keyType = dataInputStream.readByte() & 7;

        skipBytes(dataInputStream, metaOffset-232);

        int contentType = 0;
        int itemOffset = 0;
        int itemSize = 0;
        int sfoOffset = 0;
        int sfoSize = 0;

        int metaRead = 0;
        for(int i = 0; i < metaCount; i++) {
            int elementType = dataInputStream.readInt();
            int elementSize = dataInputStream.readInt();
            int readSize = 0;
            switch(elementType) {
                case 2:
                    contentType = dataInputStream.readInt();
                    readSize = 4;
                    break;
                case 13:
                    itemOffset = dataInputStream.readInt();
                    itemSize = dataInputStream.readInt();
                    readSize = 8;
                    break;
                case 14:
                    sfoOffset = dataInputStream.readInt();
                    sfoSize = dataInputStream.readInt();
                    readSize = 8;
                    break;
            }
            metaRead += elementSize + 8;
            skipBytes(dataInputStream, elementSize-readSize);
        }
        skipBytes(dataInputStream, metaSize-metaRead);

        int pkgType = 0;
        switch (contentType) {
            case 6:
                pkgType = PKG_TYPE_PSX;
                break;
            case 7:
            case 0xe:
            case 0xf:
                pkgType = PKG_TYPE_PSP;
                break;
            case 0x15:
                pkgType = PKG_TYPE_VITA_APP;
                break;
            case 0x16:
                pkgType = PKG_TYPE_VITA_DLC;
                break;
            case 0x18:
            case 0x1d:
                pkgType = PKG_TYPE_VITA_PSM;
                break;
            default:
                throw new Exception("Unknown ContentType " + contentType);
        }

        byte[] mainKey;
        switch (keyType) {
            case 1:
                mainKey = key_pkg_psp_key;
                break;
            case 2:
                mainKey = encryptECB(iv, key_pkg_vita_2);
                break;
            case 3:
                mainKey = encryptECB(iv, key_pkg_vita_3);
                break;
            case 4:
                mainKey = encryptECB(iv, key_pkg_vita_4);
                break;
            default:
                throw new Exception("Unknown key type");
        }

        skipBytes(dataInputStream, (int) (encryptedOffset-(metaOffset+metaSize)));
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(mainKey, "AES"), new IvParameterSpec(iv));
        CipherInputStream decryptedStream = new CipherInputStream(inputStream, cipher);

        skipBytes(dataInputStream, itemOffset);
        byte[] itemData = new byte[itemSize];
        readBytes(decryptedStream, itemData);
        InputStream itemStream = new ByteArrayInputStream(itemData);
        DataInputStream itemDataStream = new DataInputStream(itemStream);

        ArrayList<Item> items = new ArrayList<>();
        for(int i = 0; i < itemCount; i++) {
            int nameOffset = itemDataStream.readInt();
            int nameSize = itemDataStream.readInt();
            long dataOffset = itemDataStream.readLong();
            long dataSize = itemDataStream.readLong();
            byte[] flagData = new byte[8];
            readBytes(itemDataStream, flagData);
            byte pspType = flagData[0];
            byte flags = flagData[3];

            itemDataStream.mark(itemSize);
            skipBytes(itemDataStream, nameOffset - (i+1)* 32);
            byte[] itemNameBytes = new byte[nameSize];
            readBytes(itemDataStream, itemNameBytes);
            String itemName = new String(itemNameBytes);
            itemDataStream.reset();

            items.add(new Item(itemName, dataOffset, dataSize, flags));
        }

        //noinspection Java8ListSort
        Collections.sort(items, (o1, o2) -> Math.toIntExact(o1.offset - o2.offset));

        long currentOffset = (itemSize+itemOffset);
        for (Item item : items) {
            long toSkip = item.offset - currentOffset;
            skipBytes(decryptedStream, (int)(toSkip));
            itemHandler.call(item, decryptedStream);
            currentOffset += toSkip + item.size;
        }
    }

    private static byte[] encryptECB(byte[] data, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void skipBytes(InputStream stream, int n) throws IOException {
        while(n > 0) {
            n -= (int) stream.skip(n);
        }
    }

    private static void readBytes(InputStream stream, byte[] out) throws  IOException {
        int off = 0;
        int n = out.length;
        while(off < out.length) {
            int read = stream.read(out, off, n);
            n -= read;
            off += read;
        }
    }
}
