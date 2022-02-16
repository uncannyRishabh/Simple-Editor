package com.uncanny.simpleapplication;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.uncanny.simpleapplication.Utils.BitmapUtils;
import com.uncanny.simpleapplication.Utils.ImageDecoderThread;
import com.uncanny.simpleapplication.Utils.ImageSaverThread;
import com.uncanny.simpleapplication.Utils.UndoStack;
import com.uncanny.simpleapplication.Views.CropView;

import java.io.IOException;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class EditActivity extends AppCompatActivity {
    private String TAG = "EditActivity";
    private static final String[] PERMISSION_STRING = {Manifest.permission.WRITE_EXTERNAL_STORAGE
            ,Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_PERMISSIONS = 200;

    private int count;
    private String data;
    private Bitmap bitmap, compressedBmp;
    private BitmapUtils bmpUtil;
    private ImageDecoderThread idt;

    private CropView cropView;
    private ImageView imageView, close;
    private RelativeLayout rotate, undo, crop, save;
    private UndoStack<String> stack;

    private final Runnable onClose = () -> {
        Intent i1;
        if(data == null) i1 = new Intent(EditActivity.this, CameraActivity.class);
        else i1 = new Intent(EditActivity.this, MainActivity.class);
        startActivity(i1);
    };

    private final Runnable onBack = () -> {
        Intent i1 = new Intent(EditActivity.this, MainActivity.class);
        startActivity(i1);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        imageView = findViewById(R.id.imageView);
        close = findViewById(R.id.close);
        rotate = findViewById(R.id.Rotate);
        undo = findViewById(R.id.Undo);
        crop = findViewById(R.id.Crop);
        save = findViewById(R.id.Save);
        cropView = findViewById(R.id.cropView);

        ShapeableImageView btn1 = rotate.findViewById(R.id.icon);
        btn1.setImageDrawable(Objects.requireNonNull(ResourcesCompat.getDrawable(getResources()
                , R.drawable.ic_rotate_clockwise, null)));

        ShapeableImageView btn2 = undo.findViewById(R.id.icon);
        btn2.setImageDrawable(Objects.requireNonNull(ResourcesCompat.getDrawable(getResources()
                , R.drawable.ic_baseline_undo, null)));

        ShapeableImageView btn3 = crop.findViewById(R.id.icon);
        btn3.setImageDrawable(Objects.requireNonNull(ResourcesCompat.getDrawable(getResources()
                , R.drawable.ic_baseline_crop, null)));

        ShapeableImageView btn4 = save.findViewById(R.id.icon);
        btn4.setImageDrawable(Objects.requireNonNull(ResourcesCompat.getDrawable(getResources()
                , R.drawable.ic_baseline_save, null)));

        TextView tv1 = rotate.findViewById(R.id.label);
        tv1.setText(getResources().getString(R.string.Rotate));

        TextView tv2 = undo.findViewById(R.id.label);
        tv2.setText(getResources().getString(R.string.Undo));

        TextView tv3 = crop.findViewById(R.id.label);
        tv3.setText(getResources().getString(R.string.Crop));

        TextView tv4 = save.findViewById(R.id.label);
        tv4.setText(getResources().getString(R.string.Save));

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        stack = new UndoStack<>(25);

        data = getIntent().getStringExtra("imageUri");
        if(data != null) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver() , Uri.parse(data));
                Log.e(TAG, "onPostCreate: data: "+data+" parsedData : "+Uri.parse(data));
            } catch (IOException e) {
                e.printStackTrace();
            }

//            Log.e("TAG", "onPostCreate: w : "+bitmap.getWidth()+" h : "+bitmap.getHeight());

            if(bitmap.getWidth() > 3000){   //condition for applying compression
                bmpUtil = new BitmapUtils();
                compressedBmp = bmpUtil.compressBitmap(data);
                imageView.setImageBitmap(compressedBmp);
            }
            else
                imageView.setImageBitmap(bitmap);

        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){

                    Completable c1 = Completable.fromRunnable(idt = new ImageDecoderThread())
                            .subscribeOn(Schedulers.io());
                    c1.observeOn(AndroidSchedulers.mainThread())
                      .andThen(Completable.fromRunnable(() -> {
                        bitmap = idt.getBitmap();
                        imageView.setImageBitmap(bitmap);
                        Log.e("TAG", "onPostCreate: ia "+ImageSaverThread.dfh.getFile());
                    })).andThen(Completable.fromRunnable(() -> ImageSaverThread.dfh.deleteFile()))
                       .doOnError((e) -> Log.e("onError", "onPostCreate: "+e))
                       .subscribe();
                }
                else {
                    requestRuntimePermission();
                }
            }
        }

        close.setOnClickListener((view) -> {
            if(!stack.isEmpty()){
                showExitDialogue(onClose);
            }
            else {
                new Handler().post(onClose);
            }
        });

        Matrix m = new Matrix();

        rotate.setOnClickListener((view) -> {
            count++;
            rotateImage(imageView,true);
            rotateImage(cropView,true);
            stack.push("ROTATE");
        });

        crop.setOnClickListener((view) -> {
            cropView.setVisibility(View.VISIBLE);
            cropView.setViewBounds(getImageBounds(imageView));


//            imageView.setImageBitmap(cropBmp);


//            bmpUtil = new BitmapUtils(idt.getBitmap());
//            Bitmap newBmp = bmpUtil.rotateBitmap((count*90f)%360);
//            if(count%2!=0) m.postScale(.7f,.7f,1f,1f);
//            else m.postScale(1.3f,1.3f,1f,1f);
        });

        undo.setOnClickListener((view) -> {
            Log.e("TAG", "onPostCreate: " + stack);
            if(stack.isEmpty()) {
                Toast.makeText(EditActivity.this, "No more undo operations", Toast.LENGTH_SHORT).show();
                return;
            }
            switch (stack.peek()) {
                case "ROTATE" :
                    rotateImage(imageView,false);
                    rotateImage(cropView,true);
                    break;
                case "CROP" :
//                    cropImage();
                    break;
                default:
                    break;
            }
            stack.pop();
        });

        save.setOnClickListener((view) -> {
            Bitmap saveBmp;
            if(cropView.getDeltaCoords()!=null){
                int[] coords = cropView.getDeltaCoords();
                float[] tf = translateCoords(coords);

                saveBmp = Bitmap.createBitmap(bitmap,
                        (int) tf[1] , (int) tf[0],
                        (int) tf[3], (int) tf[2]);

                bmpUtil = new BitmapUtils(saveBmp);
            }
            else bmpUtil = new BitmapUtils(bitmap);

            saveBmp = bmpUtil.rotateBitmap((count*90f)%360);
            ImageSaverThread imageSaverThread = new ImageSaverThread(saveBmp, getContentResolver(), true);
            Completable.fromRunnable(imageSaverThread)
                    .subscribeOn(Schedulers.io())
                    .subscribe(() -> {
                        Intent i1 = new Intent(EditActivity.this, MainActivity.class);
                        i1.putExtra("loadImage",true);
                        startActivity(i1);
                        finish();
                    });
        });
    }

    private float[] translateCoords(int[] coords) {
        float[] scaled = new float[4];
        float sh = (getImageBounds(imageView).right-getImageBounds(imageView).left);
        float sw = (getImageBounds(imageView).bottom-getImageBounds(imageView).top);
        scaled[0] = coords[0]* (bitmap.getWidth()/sh);
        scaled[1] = coords[1]* (bitmap.getHeight()/sw);
        scaled[2] = coords[2]* (bitmap.getWidth()/sh) - scaled[0];
        scaled[3] = coords[3]* (bitmap.getHeight()/sw) - scaled[1];

//        Log.e(TAG, "translateCoords: sb h : "+sh+" w : "+sw);
//        Log.e(TAG, "translateCoords: ob h : "+bitmap.getHeight()+" w : "+bitmap.getWidth());
        Log.e(TAG, "translateCoords: c1 : "+coords[0]+" c2 : "+coords[1]+" c3 : "+coords[2]+" c4 : "+coords[3]);
        return scaled;
    }

    private void rotateImage(View view, boolean clockwise) {
        if(!clockwise) count--;
        view.animate().rotation((count*90f));
    }

    public static RectF getImageBounds(ImageView imageView) {
        RectF bounds = new RectF();
        Drawable drawable = imageView.getDrawable();
        if (drawable != null) {
            imageView.getImageMatrix().mapRect(bounds, new RectF(drawable.getBounds()));
        }
        return bounds;
    }

    private void requestRuntimePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(EditActivity.this
                    , PERMISSION_STRING
                    , REQUEST_PERMISSIONS);
        }
    }

    private void showExitDialogue(Runnable runnable) {
        Dialog dialog = new Dialog(EditActivity.this);
        dialog.setContentView(R.layout.exit_dialogue);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        MaterialButton responseL = dialog.findViewById(R.id.dialogueResponse2);
        MaterialButton responseR = dialog.findViewById(R.id.dialogueResponse1);
        TextView confText = dialog.findViewById(R.id.dialogueSubheading);

        responseL.setText(new String("Dismiss"));
        responseR.setText(new String("Continue"));
        confText.setText(new String("Your unsaved changes will be lost"));
        responseL.setOnClickListener(v -> dialog.dismiss());

        responseR.setOnClickListener(v -> {
            new Handler().post(runnable);
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    @Override
    public void onBackPressed() {
        if(!stack.isEmpty()){
            showExitDialogue(onBack);
        }
        else {
            Intent i1 = new Intent(EditActivity.this, MainActivity.class);
            startActivity(i1);
            finish();
        }
    }
}