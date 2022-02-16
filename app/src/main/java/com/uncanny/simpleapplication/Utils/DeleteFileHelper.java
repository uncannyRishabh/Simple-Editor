package com.uncanny.simpleapplication.Utils;

import android.util.Log;

import java.io.File;

public class DeleteFileHelper {
    private File file;

    public void setFile(File file){
        this.file = file;
    }

    public File getFile(){
        return file;
    }

    public void deleteFile(){
        if(file.exists()) {
            if (file.delete()) {
                Log.e("DFH", "File deleted.");
            }else {
                Log.e("DFH", "Failed to delete file!");
            }
        }else {
            Log.e("DFH", "File does not exist!");
        }
    }
}
