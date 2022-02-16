package com.uncanny.simpleapplication.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageDecoderThread implements Runnable{
    private static final String TAG = "ImageDecoderThread";
    private static final List<String> ACCEPTED_FILES_EXTENSIONS = Arrays.asList("JPG", "JPEG", "DNG","MP4");
    private static final FilenameFilter FILENAME_FILTER = (dir, name) -> {
        int index = name.lastIndexOf(46);
        return ACCEPTED_FILES_EXTENSIONS.contains(-1 == index ? "" : name.substring(index + 1).toUpperCase())
                && new File(dir, name).length() > 0;
    };
    private Bitmap bitmap;
    private File lastImage;

    public Bitmap getBitmap(){
        if(bitmap==null)
            return Bitmap.createBitmap(96,96, Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    public String getLastImage(){
        return lastImage.getAbsolutePath();
    }

    public ImageDecoderThread(){ }

    @Override
    public void run() {
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "DCIM/SimpleApp" + "/";

        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){
            File f = new File(dirPath);
            if(!f.exists()){
                f.mkdirs();
            }
            File[] dcimFiles = f.listFiles(FILENAME_FILTER);

            List<File> filesList = new ArrayList<>(Arrays.asList(dcimFiles != null ? dcimFiles : new File[0]));
            if (!filesList.isEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    filesList.sort((file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()));
                }
                lastImage = filesList.get(0);
                bitmap = BitmapFactory.decodeFile(String.valueOf(lastImage));

                try {
                    ExifInterface exif = new ExifInterface(lastImage.getAbsolutePath());
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
//                    Log.e(TAG, "run: Exif : "+orientation);
                    Matrix matrix = new Matrix();
                    if(orientation == 8) {
                        matrix.postRotate(270);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }
                    else if(orientation == 6) {
                        matrix.postRotate(90);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                Log.e(TAG, "Could not find any Image Files [1]");
            }
        }
        else {
            File f = new File("//storage//emulated//0//DCIM//SimpleApp//");
            if(!f.exists()){
                f.mkdirs();
            }
            File[] dcimFiles = f.listFiles(FILENAME_FILTER);
            List<File> filesList = new ArrayList<>(Arrays.asList(dcimFiles != null ? dcimFiles : new File[0]));
            if (!filesList.isEmpty()) {
                filesList.sort((file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()));

                lastImage = filesList.get(0);
                Log.e(TAG, "latest : "+lastImage);
                bitmap = BitmapFactory.decodeFile(String.valueOf(lastImage));

                try {
                    ExifInterface exif = new ExifInterface(lastImage);
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
                    Log.e(TAG, "run: Exif : "+orientation);
                    Matrix matrix = new Matrix();
                    if(orientation == 8) {
                        matrix.postRotate(270);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                Log.e(TAG, "Could not find any Image Files");
            }
        }
    }

}