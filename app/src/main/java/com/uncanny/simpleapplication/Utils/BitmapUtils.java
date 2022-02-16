package com.uncanny.simpleapplication.Utils;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BitmapUtils {
    private Bitmap bitmap;

    public BitmapUtils(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public BitmapUtils(){}

    public Bitmap rotateBitmap (float degrees) {
        if(degrees == 0) return bitmap;
        Matrix m = new Matrix();
        m.postRotate(degrees);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
        return bitmap;
    }

    public Bitmap compressBitmap(String data){
        BitmapFactory.Options Options = new BitmapFactory.Options();
        Options.inSampleSize = 4;
        Options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(Uri.parse(data).getPath().startsWith("/raw") ?
                Uri.parse(data).getPath().split("raw")[1]
                : Uri.parse(data).getPath() , Options);
    }

    public static void SaveBitmap(Bitmap bitmap, ContentResolver contentResolver, Uri uri){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                OutputStream outputStream = contentResolver.openOutputStream(uri);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                outputStream.write(byteArray);
                outputStream.close();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }


}
