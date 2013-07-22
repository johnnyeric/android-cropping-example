package com.choiboi.imagecroppingexample;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.choiboi.imagecroppingexample.gestures.MoveGestureDetector;
import com.choiboi.imagecroppingexample.gestures.RotateGestureDetector;

public class CropActivity extends Activity implements OnTouchListener {
    
    private ImageView mImg;
    private ImageView mTemplateImg;
    private int mScreenWidth;
    private int mScreenHeight;
    
    private Matrix mMatrix = new Matrix();
    private float mScaleFactor = 0.8f;
    private float mRotationDegrees = 0.f;
    private float mFocusX = 0.f;
    private float mFocusY = 0.f;
    private int mImageHeight, mImageWidth;
    private ScaleGestureDetector mScaleDetector;
    private RotateGestureDetector mRotateDetector;
    private MoveGestureDetector mMoveDetector;
    
    // Constants
    private final String TEMP_JPEG_FILENAME = "img_temp.jpg";
    private final String APP_NAME = "ImageCropExample";
    private final String STORAGE_MISSING_MSG = "Your Phone is Currently Not Connected to Any Storage Device!";
    public static final String SELECTED_MEDIA = "SELECTED_MEDIA";
    
    public static final int MEDIA_SELECT_DIALOG = 0;
    public static final int MEDIA_CAMERA = 1;
    public static final int MEDIA_GALLERY = 2;
    
    private final static int IMG_MAX_SIZE = 1000;
    private final static int IMG_MAX_SIZE_MDPI = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        
        mImg = (ImageView) findViewById(R.id.cp_img);
        mTemplateImg = (ImageView) findViewById(R.id.cp_face_template);
        mImg.setOnTouchListener(this);
        
        // Get screen size in pixels.
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenHeight = metrics.heightPixels;
        mScreenWidth = metrics.widthPixels;
        
        // View is scaled by matrix, so scale initially
        mMatrix.postScale(mScaleFactor, mScaleFactor);
        mImg.setImageMatrix(mMatrix);

        mImageHeight = 0;//photoImg.getHeight();
        mImageWidth = 0;//photoImg.getWidth();

        // Setup Gesture Detectors
        mScaleDetector = new ScaleGestureDetector(getApplicationContext(), new ScaleListener());
        mRotateDetector = new RotateGestureDetector(getApplicationContext(), new RotateListener());
        mMoveDetector = new MoveGestureDetector(getApplicationContext(), new MoveListener());
    }
    
    public void onCropImageButton(View v) {
        
    }
    
    public void onChangeTemplateButton(View v) {
        
    }
    
    public void onChangeImageButton(View v) {
        Intent intent = new Intent(this, MediaSelectDialog.class);
        startActivityForResult(intent, MEDIA_SELECT_DIALOG);
    }
    
    /*
     * Adjust the size of bitmap before loading it to memory.
     */
    private void setSelectedImage(String path) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        if (mScreenWidth == 320 && mScreenHeight == 480) {
            options.inSampleSize = calculateImageSize(options, IMG_MAX_SIZE_MDPI);
        } else {
            options.inSampleSize = calculateImageSize(options, IMG_MAX_SIZE);
        }
        options.inJustDecodeBounds = false;
        Bitmap photoImg = BitmapFactory.decodeFile(path, options);
        mImageHeight = photoImg.getHeight();
        mImageWidth = photoImg.getWidth();
        mImg.setImageBitmap(photoImg);
    }
    
    private File getOutputMediaFile(String filename) {
        return new File(getOutputLink(filename));
    }

    private String getOutputLink(String filename) {
        String directory = "";

        // Check if storage is mounted.
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), APP_NAME);

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null;
                }
            }
            directory = mediaStorageDir.getPath() + File.separator + filename;
        }
        return directory;
    }
    
    private String getGalleryImagePath(Intent data) {
        Uri imgUri = data.getData();
        String filePath = "";
        if (data.getType() == null) {
            // For getting images from gallery.
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(imgUri, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            cursor.close();
        } else if (data.getType().equals("image/jpeg") || data.getType().equals("image/png")) {
            // For getting images from dropbox.
            filePath = imgUri.getPath();
        }
        return filePath;
    }
    
    private int calculateImageSize(BitmapFactory.Options opts, int threshold) {
        int scaleFactor = 1;
        final int height = opts.outHeight;
        final int width = opts.outWidth;

        if (width >= height) {
            scaleFactor = Math.round((float) width / threshold);
        } else {
            scaleFactor = Math.round((float) height / threshold);
        }

        return scaleFactor;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == MEDIA_SELECT_DIALOG) {
                String mediaSelected = data.getExtras().getString(SELECTED_MEDIA);
                
                if (mediaSelected.equals(MediaSelectDialog.CAMERA)) {
                    File dir = getOutputMediaFile(TEMP_JPEG_FILENAME);
                    if (dir == null) {
                        // Signal user is external storage is not connected.
                        Toast.makeText(this, STORAGE_MISSING_MSG, Toast.LENGTH_SHORT).show();
                    } else {
                        // Start Camera App.
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(dir));
                        startActivityForResult(cameraIntent, MEDIA_CAMERA);
                    }
                } else if (mediaSelected.equals(MediaSelectDialog.GALLERY)) {
                    // Start Gallery App.
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent , MEDIA_GALLERY);
                }
            } else if (requestCode == MEDIA_CAMERA) {
                String path = getOutputLink(TEMP_JPEG_FILENAME);
                setSelectedImage(path);
            } else if (requestCode == MEDIA_GALLERY) {
                String path = getGalleryImagePath(data);
                setSelectedImage(path);
            }
        }
    }
    
    public boolean onTouch(View v, MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        mRotateDetector.onTouchEvent(event);
        mMoveDetector.onTouchEvent(event);

        float scaledImageCenterX = (mImageWidth * mScaleFactor) / 2;
        float scaledImageCenterY = (mImageHeight * mScaleFactor) / 2;

        mMatrix.reset();
        mMatrix.postScale(mScaleFactor, mScaleFactor);
        mMatrix.postRotate(mRotationDegrees, scaledImageCenterX, scaledImageCenterY);
        mMatrix.postTranslate(mFocusX - scaledImageCenterX, mFocusY - scaledImageCenterY);

        ImageView view = (ImageView) v;
        view.setImageMatrix(mMatrix);
        return true;
    }
    
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));

            return true;
        }
    }

    private class RotateListener extends RotateGestureDetector.SimpleOnRotateGestureListener {
        @Override
        public boolean onRotate(RotateGestureDetector detector) {
            mRotationDegrees -= detector.getRotationDegreesDelta();
            return true;
        }
    }

    private class MoveListener extends MoveGestureDetector.SimpleOnMoveGestureListener {
        @Override
        public boolean onMove(MoveGestureDetector detector) {
            PointF d = detector.getFocusDelta();
            mFocusX += d.x;
            mFocusY += d.y;

            return true;
        }
    }
}
