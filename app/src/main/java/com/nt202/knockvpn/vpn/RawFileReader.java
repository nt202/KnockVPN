package com.nt202.knockvpn.vpn;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RawFileReader {

    public static String getRawFileAsString(Context context, int resId) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = context.getResources().openRawResource(resId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toString("UTF-8");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Log or handle the exception if needed
                }
            }
        }
    }
}