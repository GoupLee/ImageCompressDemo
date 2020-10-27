package com.gouplee.luban;

import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;

/**
 * author gouplee
 * date 2020/10/23 16:23
 * remarks
 */

enum Checker {
    SINGLE;

    private static final String JPG = ".jpg";

    String extSuffix(InputStreamProvider input) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input.open(), null, options);
            return options.outMimeType.replace("image/", ".");
        } catch (IOException e) {
            e.printStackTrace();
            return JPG;
        }
    }

    boolean needCompress(int leastCompressSize, String path) {
//        File source = new File(path);
//        return source.exists() && source.length() > (leastCompressSize << 10);

        if (leastCompressSize > 0) {
            File source = new File(path);
            return source.exists() && source.length() > (leastCompressSize << 10);
        }
        return true;
    }
}
