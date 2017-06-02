package com.roc.matteroffact;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    // TODO : Replace this with our URL
    public static final String SRC_URL = "our_url_here";
    private static final String SRC_IMAGE_PATH = "Test/displayImage.jpg";
    //private static final String DISPLAY_IMAGE_NAME = "displayImage.jpg";
    private StorageReference mStorageReference;

    Bitmap mDisplayImage = null;
    ImageView mDisplayView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        // TODO : Handle network connectivity issues.
        initViews();
        initStorageReference();
        getDisplayImage();
    }

    private void initViews() {

        mDisplayView = (ImageView) findViewById(R.id.display_image);
    }
    private void initStorageReference() {
        mStorageReference =  FirebaseStorage.getInstance().getReferenceFromUrl(SRC_URL);
    }

    private void getDisplayImage() {

        // TODO : Uncomment if needed
        //final File displayImage = new File(getFilesDir(), DISPLAY_IMAGE_NAME);

        StorageReference riversRef = mStorageReference.child(SRC_IMAGE_PATH);

        // TODO : Invalidate caches when server changes image. How?
        // TODO : Use ARGB_8888 Bitmap Format for better image quality
        // TODO : Add placeholder to handle case where image is not retrieved
        Glide.with(MainActivity.this).using(new FirebaseImageLoader())
                .load(riversRef).crossFade(750).into(mDisplayView);

        // TODO : Handle failures. Add null checks
        // TODO : Do we need this code?
        /*riversRef.getFile(displayImage)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                        // Successfully downloaded data to local file
                        mDisplayImage = BitmapFactory.decodeFile(
                                displayImage.getAbsolutePath());
                        mDisplayView.setImageBitmap(mDisplayImage);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // TODO : Handle failed download
            }
        });*/
    }
}
