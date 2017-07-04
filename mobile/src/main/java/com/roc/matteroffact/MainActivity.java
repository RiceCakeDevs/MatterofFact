package com.roc.matteroffact;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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

    // TODO : Fetch image based on date
    private static final String SRC_URL = "gs://matteroffact-3c98e.appspot.com/";
    private static final String SRC_IMAGE_PATH = "Test/displayImage.jpg";

    private boolean mIsDownloadPending = false;
    private boolean mIsWallpaperOptionsVisible = false;

    // Variables to map X & Y position on screen tap
    private int mXPos, mYPos;

    // Firebase storage references to cloud storage object
    private StorageReference mSourceStorageReference, mImageStorageReference;

    // Variables to animate wallpaper options view and buttons
    Animation mWallpaperButtonsFadeInAnim = null;

    ImageButton mDownloadButton, mShareButton, mWallpaperButton;
    ImageView mDisplayView;
    LinearLayout mWallpaperButtons, mWallpaperOptions;
    ProgressBar mProgressBar;
    RelativeLayout mOptionsView;

    // TODO : App logo?
    // TODO : hide options view onResume if download not in progress
    // TODO : Don't use hardcoded strings in java or xml
    // TODO : Create seperate dimens directories for different screen sizes
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_main);

        initViews();
        initStorageReference();
        showFact();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mOptionsView.isShown() && !mProgressBar.isShown()) {
            mOptionsView.setVisibility(View.GONE);
        }
    }

    private void initViews() {

        mDownloadButton   = (ImageButton) findViewById(R.id.button_download_image);
        mShareButton      = (ImageButton) findViewById(R.id.button_share_image);
        mWallpaperButton  = (ImageButton) findViewById(R.id.button_set_wallpaper);

        mDisplayView      = (ImageView) findViewById(R.id.display_image);

        mWallpaperButtons = (LinearLayout) findViewById(R.id.wallpaper_buttons_view);
        mWallpaperOptions = (LinearLayout) findViewById(R.id.wallpaper_options_view);

        mProgressBar      = (ProgressBar) findViewById(R.id.progressBar);

        mOptionsView      = (RelativeLayout) findViewById(R.id.options_view);

        mWallpaperButtonsFadeInAnim = AnimationUtils.loadAnimation(
                this, R.anim.wallpaper_options_reveal_anim);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        // MotionEvent object holds X-Y values
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            mXPos = (int) event.getX();
            mYPos = (int) event.getY();
        }

        return super.dispatchTouchEvent(event);
    }

    private void initStorageReference() {

        // TODO : Handle network connectivity issues.
        mSourceStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(SRC_URL);
        mImageStorageReference  = mSourceStorageReference.child(SRC_IMAGE_PATH);
    }

    private void showFact() {

        // TODO : Invalidate caches when server changes image. How?
        // TODO : Use ARGB_8888 Bitmap Format for better image quality
        // TODO : Add placeholder to handle case where image is not retrieved
        Glide.with(MainActivity.this).using(new FirebaseImageLoader())
                .load(mImageStorageReference).crossFade(750).into(mDisplayView);
    }

    public void handleClick(View view) {

        switch (view.getId()) {

            case R.id.display_image: {

                if (!mOptionsView.isShown()) {
                    fadeInAndShowView(mOptionsView);
                } else {

                    if (mWallpaperOptions.isShown()) {
                        hideWallpaperOptions(false, true);
                    } else {
                        fadeOutAndHideView(mOptionsView);
                    }
                }
            }
            break;

            case R.id.button_share_image: {

                clearAppCache();

                downloadFile(getDestFile(getFilesDir()),
                        mImageStorageReference, R.id.button_share_image);
            }
            break;

            case R.id.button_download_image: {

                if (!isStoragePermissionGranted()) {
                    mIsDownloadPending = true;
                    return;
                }

                downloadFile(getDestFile(getDownloadsDir()),
                        mImageStorageReference, R.id.button_download_image);
            }
            break;

            case R.id.button_set_wallpaper: {

                clearAppCache();

                if (!mIsWallpaperOptionsVisible) {
                    revealWallpaperOptions();
                } else {
                    hideWallpaperOptions(false, false);
                }
            }
            break;

            case R.id.wallpaper_with_fact: {
                hideWallpaperOptions(true, false);
            }
            break;

            case R.id.wallpaper_without_fact: {
                hideWallpaperOptions(true, false);
            }
            break;

            default:
                break;
        }
    }

    /**
     * Deletes all files in application file directory
     */
    private void clearAppCache() {

        File dir1 = getFilesDir();

        String[] children = dir1.list();

        for (String child : children) {
            new File(dir1, child).delete();
        }
    }

    private void revealWallpaperOptions() {

        mIsWallpaperOptionsVisible = true;

        handleClickEvents(false);
        mShareButton.setVisibility(View.INVISIBLE);
        mDownloadButton.setVisibility(View.INVISIBLE);

        Animator wallpaperOptionsRevealAnim = ViewAnimationUtils.createCircularReveal(
                mWallpaperOptions, mXPos, mYPos, 0, getRadius());

        mWallpaperOptions.setVisibility(View.VISIBLE);
        mWallpaperButtons.setVisibility(View.GONE);

        wallpaperOptionsRevealAnim.setDuration(400);
        wallpaperOptionsRevealAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mWallpaperButtons.setVisibility(View.VISIBLE);
                mWallpaperButtons.startAnimation(mWallpaperButtonsFadeInAnim);
                handleClickEvents(true);
                mShareButton.setClickable(false);
                mDownloadButton.setClickable(false);
            }
        });

        wallpaperOptionsRevealAnim.start();
    }

    private void hideWallpaperOptions(final boolean isDownloadRequired, final boolean hideOptionsView) {

        mIsWallpaperOptionsVisible = false;

        handleClickEvents(false);

        Animator wallpaperOptionsHideAnim = ViewAnimationUtils.createCircularReveal(
                mWallpaperOptions, mXPos, mYPos, getRadius(), 0);

        wallpaperOptionsHideAnim.setDuration(400);
        wallpaperOptionsHideAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mWallpaperOptions.setVisibility(View.GONE);
                mShareButton.setVisibility(View.VISIBLE);
                mDownloadButton.setVisibility(View.VISIBLE);

                if (isDownloadRequired) {
                    downloadFile(getDestFile(getFilesDir()),
                            mImageStorageReference, R.id.button_set_wallpaper);
                } else {
                    handleClickEvents(true);
                }

                if (hideOptionsView) {
                    fadeOutAndHideView(mOptionsView);
                    handleClickEvents(true);
                }
            }
        });

        wallpaperOptionsHideAnim.start();
    }


    /**
     * Downloads file from Firebase StorageReference src and stores it in
     * file dest on device.
     * @param dest The destination where file is to be downloaded to.
     * @param src  The StorageReference from where the file is to be downloaded.
     * @param id   The id of the item that has requested for download. This is
     *             used to handle file once download is successful.
     */
    private void downloadFile(final File dest, StorageReference src, final int id) {

        handleClickEvents(false);
        mProgressBar.setVisibility(View.VISIBLE);

        src.getFile(dest)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                        handleOnFileDownloaded(id, dest);

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

                handleOnFileDownloadFailed();
            }
        });
    }

    private void handleOnFileDownloaded(int id, File downloadedFile) {

        initViewsAfterDownloadResult();

        switch (id) {

            case R.id.button_share_image: {

                Uri uri = FileProvider.getUriForFile(getApplicationContext(),
                        getApplicationContext().getPackageName(), downloadedFile);

                share(uri);
            }
            break;

            case R.id.button_download_image: {
                Toast.makeText(getApplicationContext(),
                        "Fact downloaded successfully", Toast.LENGTH_SHORT).show();

                MediaScannerConnection.scanFile(getApplicationContext(),
                        new String[]{downloadedFile.getAbsolutePath()},
                        new String[]{"image/jpeg"}, null);
            }
            break;

            case R.id.button_set_wallpaper: {

                Uri uri = FileProvider.getUriForFile(getApplicationContext(),
                        getApplicationContext().getPackageName(), downloadedFile);

                setWallpaper(uri);
            }
            break;

            default:
                break;
        }
    }

    private void handleOnFileDownloadFailed() {

        initViewsAfterDownloadResult();
        Toast.makeText(getApplicationContext(),
                "Something went wrong. Try again soon.", Toast.LENGTH_SHORT).show();
    }

    private void initViewsAfterDownloadResult() {

        handleClickEvents(true);
        mProgressBar.setVisibility(View.GONE);
        fadeOutAndHideView(mOptionsView);
    }

    private void handleClickEvents(boolean isEnabled) {
        mShareButton.setClickable(isEnabled);
        mDownloadButton.setClickable(isEnabled);
        mDisplayView.setClickable(isEnabled);
        mWallpaperButton.setClickable(isEnabled);
    }

    private void share(Uri result) {

        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Fact for the day");
        intent.putExtra(Intent.EXTRA_TEXT, "Hey! As a matter of fact, did you know this?");
        intent.putExtra(Intent.EXTRA_STREAM, result);

        startActivity(Intent.createChooser(intent, "Share image"));
    }

    private void setWallpaper(Uri result) {

        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);

        intent.setDataAndType(result, "image/*");
        intent.putExtra("mimeType", "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Set as:"));
    }

    private void fadeOutAndHideView(final View view) {

        Animation fadeOut = new AlphaAnimation(1, 0);

        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(350);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });

        view.startAnimation(fadeOut);
    }

    private void fadeInAndShowView(final View view) {

        view.setVisibility(View.VISIBLE);

        Animation fadeIn = new AlphaAnimation(0, 1);

        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(250);

        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });

        view.startAnimation(fadeIn);
    }

    /**
     * Method to check if our application has the required permissions.
     * If required permissions are not granted, request permission from user.
     *
     * @return true,  if required permissions are granted.
     *         false, if required permissions are not granted.
     */
    public boolean isStoragePermissionGranted() {

        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {

                // Permission is already granted. So return true;
                return true;
            } else {

                // We do not have write permission to save file to external storage
                // Request permission from user.
                String[] permissions = new String[] {
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE};

                ActivityCompat.requestPermissions(this, permissions, 1);

                return false;
            }

        } else {

            // Permission is automatically granted on sdk < 23 upon installation,
            // So return true.
            return true;
        }
    }

    /**
     * Callback method for result of requestPermissions.
     * @param requestCode  The request code passed in requestPermissions.
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && mIsDownloadPending) {

            // Required permission is granted.
            // Resume any pending tasks that required this permission.
            downloadFile(getDestFile(getDownloadsDir()),
                    mImageStorageReference, R.id.button_download_image);
        } else {

            // User denied the permission. Show toast. 
            Toast.makeText(getApplicationContext(), "Can't save image. " +
                    "Please provide the required permission.", Toast.LENGTH_SHORT).show();
            initViewsAfterDownloadResult();
        }
    }

    private File getDownloadsDir() {

        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), getString(R.string.app_name));
        dir.mkdirs();

        return dir;
    }

    private File getDestFile(File destDir) {

        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File destFile = new File(destDir, "Fact_" + timeStamp + ".jpg");

        return destFile;
    }

    private float getRadius() {

        float radius = 0.0f;

        if (mOptionsView != null) {

            // The center for the clipping circle
            int cx = mOptionsView.getWidth();
            int cy = mOptionsView.getHeight();

            // The radius for the clipping circle
            radius = (float) Math.hypot(cx, cy);
        }

        return radius;
    }
}
