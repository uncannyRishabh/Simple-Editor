package com.uncanny.simpleapplication.Utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageSaverThread implements Runnable {
    private Image mImage;
    private Bitmap bitmap;
    private String cameraId;
    private boolean isBitmap = false;
    private final ContentResolver contentResolver;
    private Uri uri;

    public static DeleteFileHelper dfh = new DeleteFileHelper();
    String currentDateandTime;
    File file;
//    Executor executor = new SerialExecutor(Executors.newFixedThreadPool(2));

    public ImageSaverThread(Image image, String cameraId, ContentResolver contentResolver) {
        this.mImage = image;
        this.cameraId = cameraId;
        this.contentResolver = contentResolver;
    }

    public ImageSaverThread(Bitmap bitmap, ContentResolver contentResolver, boolean isBitmap) {
        this.bitmap = bitmap;
        this.contentResolver = contentResolver;
        this.isBitmap = isBitmap;
    }

    @Override
    public void run() {
        ContentValues values = new ContentValues();
        File DIR_DCIM = new File(Environment.getExternalStorageDirectory()+"//DCIM//SimpleApp//");
        if(!DIR_DCIM.exists()){
            DIR_DCIM.mkdirs();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        currentDateandTime = sdf.format(new Date());

        file = new File(Environment.getExternalStorageDirectory()+"//DCIM//SimpleApp//"
                ,"SimpleApp"+currentDateandTime+cameraId+".jpg");
        dfh.setFile(file);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/SimpleApp/");
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "SimpleApp"+currentDateandTime+cameraId+".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            values.put( MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis() );
            values.put(MediaStore.Images.Media.TITLE, "Image.jpg");

            Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            uri = contentResolver.insert(external, values);
        }

        if(isBitmap){
            BitmapUtils.SaveBitmap(bitmap, contentResolver, uri);
        }
        else {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            saveByteBuffer(bytes, file);
        }

    }

    private void saveByteBuffer(byte[] bytes, File file) {
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                OutputStream outputStream = contentResolver.openOutputStream(uri);
                outputStream.write(bytes);
                outputStream.close();
            }
            else {
                try{
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(bytes);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            mImage.close();
        }
    }
}