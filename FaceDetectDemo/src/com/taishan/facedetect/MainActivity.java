package com.taishan.facedetect;

import java.io.File;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity
{

    private ImageView imageViewFace;
    private Button clickBtnLocal;
    private Button clickBtnCamera;

    private Uri photoUri;

    private final int PIC_FROM_CAMERA = 1;
    private final int PIC_FROM＿LOCALPHOTO = 0;

    /******* Detect face *******/

    static final String TAG = "Face";
    
    Button clickBtnDetectFace = null;
    final int N_MAX = 1;
    ProgressBar progressBar = null;

    Bitmap srcImg = null;
    
    private class DetectFaceAsyncTask extends AsyncTask<String, Integer, Integer>
    {
        private Bitmap bitmap;
        private Bitmap faceBitmap;

        private DetectFaceAsyncTask(Bitmap bitmap)
        {
            this.bitmap = bitmap;
        }

        @Override
        protected Integer doInBackground(String... arg0)
        {
            return detectFace(this.bitmap);
        }

        @Override
        protected void onPostExecute(Integer faceCount)
        {
            super.onPostExecute(faceCount);

            progressBar.setVisibility(View.GONE);
            clickBtnDetectFace.setEnabled(true);
            imageViewFace.setImageBitmap(this.faceBitmap);

            Toast.makeText(getApplicationContext(), "检测到人脸: " + faceCount, Toast.LENGTH_SHORT)
                    .show();
        }

        private int detectFace(Bitmap bitmap)
        {
            Log.i(TAG, "Begin face detect");
            if (bitmap == null)
            {
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test);
            }

            this.faceBitmap = bitmap.copy(Config.RGB_565, true);
            int width = this.faceBitmap.getWidth();
            int height = this.faceBitmap.getHeight();
            Log.i(TAG, "待检测图像: w = " + width + ", h = " + height);
           
            int detectedFaceCount = 0;
            
            Log.i(TAG, "Start face detect");
            FaceDetector.Face[] faces = new FaceDetector.Face[N_MAX];
            FaceDetector faceDetector = new FaceDetector(width, height, N_MAX);
            detectedFaceCount = faceDetector.findFaces(this.faceBitmap, faces);
            Log.i(TAG, "检测到人脸：" + detectedFaceCount);
            Log.i(TAG, "Stop face detect");
            
            drawFaces(faces, detectedFaceCount);
            
            Log.i(TAG, "End face detect");

            return detectedFaceCount;
        }

        private void drawFaces(FaceDetector.Face[] faces, int detectedFaceCount)
        {
            for (int i = 0; i < detectedFaceCount; i++)
            {
                Face f = faces[i];
                PointF midPoint = new PointF();
                float dis = f.eyesDistance();
                f.getMidPoint(midPoint);
                int dd = (int) (dis);
                Point eyeLeft = new Point((int) (midPoint.x - dis / 2), (int) midPoint.y);
                Point eyeRight = new Point((int) (midPoint.x + dis / 2), (int) midPoint.y);
                Rect faceRect = new Rect((int) (midPoint.x - dd), (int) (midPoint.y - dd),
                        (int) (midPoint.x + dd), (int) (midPoint.y + dd));
                Log.i(TAG, "左眼坐标 x = " + eyeLeft.x + ", y = " + eyeLeft.y);

                Canvas canvas = new Canvas(this.faceBitmap);
                Paint p = new Paint();
                p.setAntiAlias(true);
                p.setStrokeWidth(8);
                p.setStyle(Paint.Style.STROKE);
                p.setColor(Color.GREEN);
                canvas.drawCircle(eyeLeft.x, eyeLeft.y, 20, p);
                canvas.drawCircle(eyeRight.x, eyeRight.y, 20, p);
                canvas.drawRect(faceRect, p);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageViewFace = (ImageView) findViewById(R.id.imageFace);
        clickBtnLocal = (Button) findViewById(R.id.click_local);
        clickBtnCamera = (Button) findViewById(R.id.click_camera);
        clickBtnDetectFace = (Button) findViewById(R.id.buttonDetectFace);
        progressBar = (ProgressBar) findViewById(R.id.progressBarDetect);
        

        clickBtnDetectFace.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                progressBar.setVisibility(View.VISIBLE);
                clickBtnDetectFace.setEnabled(false);
                new DetectFaceAsyncTask(srcImg).execute();
            }
        });

        // 本地选择
        clickBtnLocal.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                doHandlerPhoto(PIC_FROM＿LOCALPHOTO);// 从相册中去获取
            }
        });

        // 拍照
        clickBtnCamera.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                doHandlerPhoto(PIC_FROM_CAMERA);// 用户点击了从照相机获取
            }
        });
    }

    /**
     * 根据不同方式选择图片设置ImageView
     * 
     * @param type 0-本地相册选择，非0为拍照
     */
    private void doHandlerPhoto(int type)
    {
        try
        {
            // 保存裁剪后的图片文件
            File pictureFileDir = new File(Environment.getExternalStorageDirectory(), "/upload");
            if (!pictureFileDir.exists())
            {
                pictureFileDir.mkdirs();
            }
            File picFile = new File(pictureFileDir, "upload.jpeg");
            if (!picFile.exists())
            {
                picFile.createNewFile();
            }
            photoUri = Uri.fromFile(picFile);

            if (type == PIC_FROM＿LOCALPHOTO)
            {
                Intent intent = getCropImageIntent();
                startActivityForResult(intent, PIC_FROM＿LOCALPHOTO);
            }
            else
            {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(cameraIntent, PIC_FROM_CAMERA);
            }

        }
        catch (Exception e)
        {
            Log.i("HandlerPicError", "处理图片出现错误");
        }
    }

    /**
     * 调用图片剪辑程序
     */
    public Intent getCropImageIntent()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        setIntentParams(intent);
        return intent;
    }

    /**
     * 启动裁剪
     */
    private void cropImageUriByTakePhoto()
    {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(photoUri, "image/*");
        setIntentParams(intent);
        startActivityForResult(intent, PIC_FROM＿LOCALPHOTO);
    }

    /**
     * 设置公用参数
     */
    private void setIntentParams(Intent intent)
    {
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 600);
        intent.putExtra("outputY", 600);
        intent.putExtra("noFaceDetection", false); // no face detection
        intent.putExtra("scale", true);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case PIC_FROM_CAMERA: // 拍照
                try
                {
                    cropImageUriByTakePhoto();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
            case PIC_FROM＿LOCALPHOTO:
                try
                {
                    if (photoUri != null)
                    {
                        Bitmap bitmap = decodeUriAsBitmap(photoUri);
                        imageViewFace.setImageBitmap(bitmap);
                        srcImg = bitmap;
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                break;
        }
    }

    private Bitmap decodeUriAsBitmap(Uri uri)
    {
        Bitmap bitmap = null;
        try
        {
            bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }
}
