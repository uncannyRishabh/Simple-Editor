package com.uncanny.simpleapplication;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.uncanny.simpleapplication.Utils.BitmapUtils;
import com.uncanny.simpleapplication.Utils.ImageDecoderThread;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView;
    private MaterialButton takeSelfie, upload;
    private ImageDecoderThread idt;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        takeSelfie = findViewById(R.id.takeSelfie);
        upload     = findViewById(R.id.galleryUpload);
        imageView = findViewById(R.id.editedImagePreview);

        takeSelfie.setOnClickListener(view -> {
            Intent openCamera = new Intent(this, CameraActivity.class);
            startActivity(openCamera);
        });

        upload.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePicker.launch(intent);
        });

    }

    ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Intent passData = new Intent(MainActivity.this,EditActivity.class);
                    passData.putExtra("imageUri",data.getDataString());
                    startActivity(passData);
                }
            });

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if(getIntent().hasExtra("loadImage")) {
            boolean s = getIntent().getBooleanExtra("loadImage",false);
            if(s) {
                Completable c1 = Completable.fromRunnable(idt = new ImageDecoderThread())
                        .subscribeOn(Schedulers.io());
                c1.andThen(Completable.fromRunnable(() -> {
                    bitmap = idt.getBitmap();
                    if(bitmap.getWidth() > 3000){
                        BitmapUtils utils = new BitmapUtils();
                        bitmap = utils.compressBitmap(idt.getLastImage());
                    }
                }))
                .doOnError(error -> Log.e("TAG", "onPostCreate: " + error.getMessage()))
                .observeOn(AndroidSchedulers.mainThread())
                .andThen(Completable.fromRunnable(() -> imageView.setImageBitmap(bitmap)))
                .subscribe();

            }
        }
    }

    @Override
    public void onBackPressed() {
        showExitDialogue();
    }

    private void showExitDialogue() {
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.exit_dialogue);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        MaterialButton responseL = dialog.findViewById(R.id.dialogueResponse2);
        MaterialButton responseR = dialog.findViewById(R.id.dialogueResponse1);

        responseL.setOnClickListener(v -> dialog.dismiss());

        responseR.setOnClickListener(v -> {
            dialog.dismiss();
            finishAffinity();
        });

        dialog.show();
    }
}