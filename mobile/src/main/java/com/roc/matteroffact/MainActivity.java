package com.roc.matteroffact;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    // TODO : Replace this with our URL
    public static final String SRC_URL = "our_url_here";
    private static final String SRC_IMAGE_PATH = "Test/displayImage.jpg";
    private static final String DISPLAY_IMAGE_NAME = "displayImage.jpg";

    private boolean mIsDownloadPending = false;

    private StorageReference mSourceStorageReference, mImageStorageReference;

    Bitmap mDisplayImage = null;
    RelativeLayout mOptionsView;
    ImageView mDisplayView;
    ImageButton mShareButton, mDownloadButton;
    ProgressBar mProgressBar;

    // TODO : Implement splash screen
    // TODO : App logo?
    // TODO : hide options view onResume if download not in progress
    // TODO : Don't use hardcoded strings in java or xml
    // TODO : Create seperate dimens directories for different screen sizes
    // TODO : Clean up code
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        initViews();
        initStorageReference();
        showFact();
    }

    private void initViews() {

        mDisplayView = (ImageView) findViewById(R.id.display_image);
        mOptionsView = (RelativeLayout) findViewById(R.id.options_view);
        mShareButton = (ImageButton) findViewById(R.id.button_share_image);
        mDownloadButton = (ImageButton) findViewById(R.id.button_download_image);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private void initStorageReference() {

        // TODO : Handle network connectivity issues.
        mSourceStorageReference =  FirebaseStorage.getInstance().getReferenceFromUrl(SRC_URL);
        mImageStorageReference = mSourceStorageReference.child(SRC_IMAGE_PATH);
    }

    private void showFact() {

        // TODO : Invalidate caches when server changes image. How?
        // TODO : Use ARGB_8888 Bitmap Format for better image quality
        // TODO : Add placeholder to handle case where image is not retrieved
        Glide.with(MainActivity.this).using(new FirebaseImageLoader())
                .load(mImageStorageReference).crossFade(750).into(mDisplayView);
    }

    public void handleClick(View v) {

        switch(v.getId()) {

            case R.id.display_image : {
                if (!mOptionsView.isShown()) {
                    fadeInAndShowImage(mOptionsView);
                } else {
                    fadeOutAndHideImage(mOptionsView);
                }
            }
            break;

            case R.id.button_share_image: {

                File dir1 = getFilesDir();
                String[] children = dir1 .list();
                for (String child : children) {
                        new File(dir1 , child).delete();
                    }

                final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                final File displayImage = new File(getFilesDir(), "Fact_" + timeStamp + ".jpg");
                mShareButton.setClickable(false);
                mDownloadButton.setClickable(false);
                mDisplayView.setClickable(false);
                mProgressBar.setVisibility(View.VISIBLE);

                mImageStorageReference.getFile(displayImage)
                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                                Uri uri = FileProvider.getUriForFile(getApplicationContext(),
                                        getApplicationContext().getPackageName(), displayImage);
                                share(uri); // startActivity probably needs UI thread
                                mShareButton.setClickable(true);
                                mDownloadButton.setClickable(true);
                                mDisplayView.setClickable(true);
                                mProgressBar.setVisibility(View.GONE);
                                fadeOutAndHideImage(mOptionsView);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // TODO : Handle failed download
                        mShareButton.setClickable(true);
                        mDownloadButton.setClickable(true);
                        mDisplayView.setClickable(true);
                        mProgressBar.setVisibility(View.GONE);
                        fadeOutAndHideImage(mOptionsView);
                    }
                });
            }
            break;

            case R.id.button_download_image : {

                if(!isStoragePermissionGranted()) {
                    mIsDownloadPending = true;
                    return;
                }

                downloadImage();
            }
            break;

            default:
                break;
        }
    }

    private void downloadImage() {

        final File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "Matter of Fact");

        dir.mkdirs();

        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final File displayImage = new File(dir, "Fact_" + timeStamp + ".jpg");
        mShareButton.setClickable(false);
        mDownloadButton.setClickable(false);
        mDisplayView.setClickable(false);
        mProgressBar.setVisibility(View.VISIBLE);

        mImageStorageReference.getFile(displayImage)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                        Toast.makeText(getApplicationContext(), "Fact downloaded successfully", Toast.LENGTH_SHORT).show();
                        mShareButton.setClickable(true);
                        mDownloadButton.setClickable(true);
                        mDisplayView.setClickable(true);
                        mProgressBar.setVisibility(View.GONE);
                        fadeOutAndHideImage(mOptionsView);

                        MediaScannerConnection.scanFile(getApplicationContext(),
                                new String[] {displayImage.getAbsolutePath()}, new String[] {"image/jpeg"}, null);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(getApplicationContext(), "Failed to download fact", Toast.LENGTH_SHORT).show();
                mShareButton.setClickable(true);
                mDownloadButton.setClickable(true);
                mDisplayView.setClickable(true);
                mProgressBar.setVisibility(View.GONE);
                fadeOutAndHideImage(mOptionsView);
            }
        });
    }

    private void share(Uri result) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Fact for the day");
        intent.putExtra(Intent.EXTRA_TEXT, "Hey! As a matter of fact, did you know this?");
        intent.putExtra(Intent.EXTRA_STREAM, result);

        startActivity(Intent.createChooser(intent, "Share image"));
    }

    private void fadeOutAndHideImage(final View img)
    {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(350);

        fadeOut.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
                img.setVisibility(View.GONE);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });

        img.startAnimation(fadeOut);
    }

    private void fadeInAndShowImage(final View img) {
        img.setVisibility(View.VISIBLE);

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(250);

        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation) {}
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });

        img.startAnimation(fadeIn);
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED && mIsDownloadPending){
            //resume tasks needing this permission
            downloadImage();
        } else {
            Toast.makeText(getApplicationContext(), "Can't save image. Please provide the required permission.", Toast.LENGTH_SHORT).show();
            mShareButton.setClickable(true);
            mDownloadButton.setClickable(true);
            mDisplayView.setClickable(true);
            mProgressBar.setVisibility(View.GONE);
            fadeOutAndHideImage(mOptionsView);
        }
    }
}
