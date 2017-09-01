package harshpreetsingh.cameraapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Camera extends AppCompatActivity implements AdapterView.OnItemSelectedListener {


    private Button click, camera_face;
    String camerafaceButtonText = "Back";
    private Spinner spinner;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public CameraManager cameraManager;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private android.util.Size[] imageDimension;
    private ImageReader imageReader;
    private File file;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    public SurfaceView surfaceview;
    public int cameraWidth,cameraHeight;

    FaceRectangle facerectangle;
    public int orientation_offset;

    public static final String CAMERA_BACK = "0";
    public static final String CAMERA_FRONT = "1";
    public String choosen_camera_id = CAMERA_BACK, TAG = "logs";
    public int index = 0;

    public int width , height;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);



        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);






        camera_face = (Button) findViewById(R.id.camera_face);
        camera_face.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (camera_face.getText().equals("Back")) {
                    camera_face.setText("Front");
                    camerafaceButtonText = "Front";
                } else {

                    camera_face.setText("Back");
                    camerafaceButtonText = "Back";

                }


                switchCamera();

            }


        });



        spinner = (Spinner) findViewById(R.id.spinner2);
        // Spinner Drop down elements

        //set spinner function for resolutions
        setspinnerfunction();

        spinner.setOnItemSelectedListener(this);





        click = (Button) findViewById(R.id.btn_takepicture);
        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    takePicture(width,height);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

            }
        });


        //set the texture view

        setTextureView();


        // set the surface view for face detection

        surfaceview = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceview.setZOrderOnTop(true);
        surfaceview.getHolder().setFormat(PixelFormat.TRANSLUCENT); //for making it not visible on camera preview






    }


    private List<String> getCameraResolutions() throws CameraAccessException {


        CameraCharacteristics cameracharact = cameraManager.getCameraCharacteristics(choosen_camera_id);
        Size[] resolutionsavailable = cameracharact.get(cameracharact.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

        Log.d("resolutiontag", resolutionsavailable[0].toString());

        List<String> resolutions = new ArrayList<String>();

        for (int i = 0; i < resolutionsavailable.length; i++) {

            float resolutionconversion = (resolutionsavailable[i].getWidth() * resolutionsavailable[i].getHeight()) / 1000000;
            resolutions.add(i, resolutionsavailable[i].toString() + " ("+resolutionconversion+" MPix)");
        }

        return resolutions;
    }


    private void setTextureView() {

        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                //open your camera here
                openCamera();

                try {
                    CameraCharacteristics charac = cameraManager.getCameraCharacteristics(choosen_camera_id);

                    Rect recCameraBounds = charac.get(
                            CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                    cameraWidth = recCameraBounds.right;
                    cameraHeight = recCameraBounds.bottom;



                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }



            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                // Transform you image captured size according to the surface width and height


            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }


            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {


//                HashMap result = new HashMap();
//                Face[] faces = (Face[]) result.get(CaptureResult.STATISTICS_FACES);
//
//                if(faces != null)
//                Toast.makeText(Camera.this,faces[0].toString(),Toast.LENGTH_LONG).show();


       //        facerectangle = new FaceRectangle("facedrawthread",surfaceview,orientation_offset,faces,cameraWidth,cameraHeight);


            }
        });


    }


    private void openCamera() {

        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(choosen_camera_id);

            android.util.Size[] jpegsizes = null;

            assert characteristics != null;

            if (characteristics != null)
                jpegsizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

            imageDimension = jpegsizes.clone();

            orientation_offset = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            // Add permission for camera and let user grant the permission

            checkappPermissions();

            cameraManager.openCamera(choosen_camera_id, stateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(this, "harsh exception", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }


    final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };


    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension[index].getWidth(), imageDimension[index].getHeight());

            //set width and height globally
            width = imageDimension[index].getWidth();
            height = imageDimension[index].getHeight();


            CameraCharacteristics ch = cameraManager.getCameraCharacteristics(choosen_camera_id);


            Surface surface = new Surface(texture);


            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(Camera.this, "Configuration change failed", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

//    private void configureTransform(int viewWidth, int viewHeight) {
//        Activity activity = getActivity();
//        if (null == textureView || null == mPreviewSize || null == activity) {
//            return;
//        }
//        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
//        Matrix matrix = new Matrix();
//        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
//        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
//        float centerX = viewRect.centerX();
//        float centerY = viewRect.centerY();
//        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
//            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
//            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
//            float scale = Math.max(
//                    (float) viewHeight / mPreviewSize.getHeight(),
//                    (float) viewWidth / mPreviewSize.getWidth());
//            matrix.postScale(scale, scale, centerX, centerY);
//            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
//        } else if (Surface.ROTATION_180 == rotation) {
//            matrix.postRotate(180, centerX, centerY);
//        }
//        mTextureView.setTransform(matrix);
//    }
//









    protected void takePicture(int width , int height) throws CameraAccessException {
        if(cameraDevice ==  null) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }


            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));


            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
            String date = dateFormat.format(new Date());
            String photoFile = "Harsh_" + date + ".jpg";



            file = new File(Environment.getExternalStorageDirectory()+photoFile);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);


            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(Camera.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        }



















    public void switchCamera() {
        if (choosen_camera_id.equals(CAMERA_FRONT)) {
            choosen_camera_id = CAMERA_BACK;
            onPause();
            onResume();
        } else if (choosen_camera_id.equals(CAMERA_BACK)) {
            choosen_camera_id = CAMERA_FRONT;
            onPause();
            onResume();
        }


        //set the spinner again bcoz camera id is changed now

        setspinnerfunction();


    }


    private void checkappPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Camera.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            return;
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {

            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                // Permission was denied or request was cancelled
                // }

            }


        }
    }



    private void setspinnerfunction() {

        List<String> categories = null;
        try {
            categories = getCameraResolutions();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(dataAdapter);


    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();

        index = position;

        onPause();
        onResume();



        // Showing selected spinner item
        Toast.makeText(parent.getContext(), "Selected: " + item, Toast.LENGTH_LONG).show();


    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }




    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        reopenCamera();



    }



    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }


    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }


    public void reopenCamera() {
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureView.getSurfaceTextureListener());
        }
    }








    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}