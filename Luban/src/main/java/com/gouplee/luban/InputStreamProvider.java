package com.gouplee.luban;

import java.io.IOException;
import java.io.InputStream;

/**
 * author gouplee
 * date 2020/10/23 10:26
 * remarks
 */

public interface InputStreamProvider {

    InputStream open() throws IOException;

    String getPath();
}
