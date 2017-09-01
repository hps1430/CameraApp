package harshpreetsingh.cameraapp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.camera2.params.Face;
import android.view.SurfaceView;

/**
 * Created by harsh singh on 31-08-2017.
 */

public class FaceRectangle implements Runnable {

    Thread thread;
    String ThreadName;
    SurfaceView surfaceview;
    int cameraWidth,cameraHeight;
    Rect rectangleFace;
    int orientation_offset;

    Face detectedFace;


    public FaceRectangle(String ThreadName, SurfaceView surfaceview, int orientation_offset, Face[] faces, int cameraWidth, int cameraHeight) {

        this.ThreadName = ThreadName;

        thread = new Thread(this,ThreadName);

        this.orientation_offset = orientation_offset;


        if (faces.length > 0)
        {
            detectedFace = faces[0];
            rectangleFace = detectedFace.getBounds();
        }

        this.surfaceview = surfaceview;
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;


        thread.start();

    }

    @Override
    public void run() {

        Paint paint = new Paint();
        paint.setColor(Color.rgb(175,0,255));
        paint.setStyle(Paint.Style.STROKE);





        Canvas currentCanvas = surfaceview.getHolder().lockCanvas();
        if (currentCanvas != null) {

            currentCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            if (detectedFace != null && rectangleFace.height() > 0) {

                int canvasWidth = currentCanvas.getWidth();
                int canvasHeight = currentCanvas.getHeight();
                int faceWidthOffset = rectangleFace.width()/8;
                int faceHeightOffset = rectangleFace.height()/8;

                currentCanvas.save();
                currentCanvas.rotate(360 - orientation_offset, canvasWidth / 2,
                        canvasHeight / 2);

                int l = rectangleFace.right;
                int t = rectangleFace.bottom;
                int r = rectangleFace.left;
                int b = rectangleFace.top;
                int left = (canvasWidth - (canvasWidth*l)/cameraWidth)-(faceWidthOffset);
                int top  = (canvasHeight*t)/cameraHeight - (faceHeightOffset);
                int right = (canvasWidth - (canvasWidth*r)/cameraWidth) + (faceWidthOffset);
                int bottom = (canvasHeight*b)/cameraHeight + (faceHeightOffset);

                currentCanvas.drawRect(left, top, right, bottom, paint);
                currentCanvas.restore();
            }
        }
        surfaceview.getHolder().unlockCanvasAndPost(currentCanvas);

    }


}
