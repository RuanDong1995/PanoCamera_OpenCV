package com.project.acer_pc.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraActivity extends Activity implements SensorEventListener{

    private TextureView tv;
    private Button btn,bdone,bImages;
    private String mCameraId = "0";//摄像头id（通常0代表后置摄像头，1代表前置摄像头）
    private final int RESULT_CODE_CAMERA=1;//判断是否有拍照权限的标识码
    private CameraDevice cameraDevice;
    private CameraCaptureSession mPreviewSession;
    private CaptureRequest.Builder mCaptureRequestBuilder,captureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private ImageReader imageReader;
    private int height=0,width=0;
    private Size previewSize;
    private ImageView iv;
    private TextView textView;
    private TextView move;
    private SensorManager sensorManager;
    private List<Bitmap> listImage	= new ArrayList<>();
    private List<String> imageinfo = new ArrayList<>();
    private Bundle bundle;
    public ArrayList<String> mResults = new ArrayList<>();
    private static float direction;//第一张照片的角度
    private static float direction1;//变化的角度值

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        tv = (TextureView) findViewById(R.id.camera);
        btn = (Button) findViewById(R.id.capture);
        //bdone = (Button)findViewById(R.id.bDone);
       // bImages = (Button)findViewById(R.id.bImages);
        iv= (ImageView) findViewById(R.id.iv);
        textView = (TextView)findViewById(R.id.tv);
        move = (TextView) findViewById(R.id.move);
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        //拍照
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
                direction = direction1;
                Log.d("传感器值","z: "+direction);
                move.setText("请向右平稳移动设备");
                Toast.makeText(getApplicationContext(), "拍摄完成点击返回键", Toast.LENGTH_SHORT).show();


            }
        });

       /* bdone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putStringArrayListExtra("result",mResults);
                intent.setClass(CameraActivity.this,StitchActivity.class);
                startActivity(intent);
            }
        });*/


        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putStringArrayListExtra("result", mResults);
                Log.d("result size","result size is"+mResults.size());
                setResult(1000,intent);
                finish();
                Log.d("bimage","返回mainactivity");
            }
        });
        //设置TextureView监听
        tv.setSurfaceTextureListener(surfaceTextureListener);

    }

    @Override
    public void onBackPressed() {
        if(mResults!=null){
            Intent intent = new Intent();
            intent.putStringArrayListExtra("result", mResults);
            Log.d("result size","result size is"+mResults.size());
            setResult(1000,intent);
            super.onBackPressed();
            Log.d("pressback","返回键");
        }
        else {
            super.onBackPressed();

        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraDevice!=null) {
            stopCamera();
            sensorManager.unregisterListener((SensorEventListener) CameraActivity.this);//解除注册监听
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCamera();
        // 为方向传感器注册监听器
    sensorManager.registerListener((SensorEventListener) CameraActivity.this,sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_GAME);
    }



    /**TextureView的监听*/
    private TextureView.SurfaceTextureListener surfaceTextureListener= new TextureView.SurfaceTextureListener() {

        //可用
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            CameraActivity.this.width=width;
            CameraActivity.this.height=height;
            openCamera();
        }


        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        //释放
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            stopCamera();
            return true;
        }

        //更新
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    /**打开摄像头*/
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //设置摄像头特性
        setCameraCharacteristics(manager);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //提示用户开户权限
                String[] perms = {"android.permission.CAMERA"};
                ActivityCompat.requestPermissions(CameraActivity.this,perms, RESULT_CODE_CAMERA);

            }else {
                manager.openCamera(mCameraId, stateCallback, null);
            }

        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }


    /**设置摄像头的参数*/
    private void setCameraCharacteristics(CameraManager manager)
    {
        try
        {
            // 获取指定摄像头的特性
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(mCameraId);
            // 获取摄像头支持的配置属性
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            // 获取摄像头支持的最大尺寸
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),new CompareSizesByArea());
            // 创建一个ImageReader对象，用于获取摄像头的图像数据
            imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                    ImageFormat.JPEG, 2);
            //设置获取图片的监听
            imageReader.setOnImageAvailableListener(imageAvailableListener,null);
            // 获取最佳的预览尺寸
            previewSize = chooseOptimalSize(map.getOutputSizes(
                    SurfaceTexture.class), width, height, largest);
        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
        catch (NullPointerException e)
        {
        }
    }
    private static Size chooseOptimalSize(Size[] choices
            , int width, int height, Size aspectRatio)
    {
        // 收集摄像头支持的大过预览Surface的分辨率
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices)
        {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height)
            {
                bigEnough.add(option);
            }
        }
        // 如果找到多个预览尺寸，获取其中面积最小的
        if (bigEnough.size() > 0)
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else
        {
            //没有合适的预览尺寸
            return choices[0];
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        // 获取传感器类型
        int type = event.sensor.getType();
        StringBuilder sb;

        switch (type){
            case Sensor.TYPE_ORIENTATION:
                sb = new StringBuilder();
                sb.append("\n方向传感器返回数据：");
                sb.append("\n绕Z轴转过的角度：");
                sb.append(values[0]);
                sb.append("\n绕X轴转过的角度：");
                sb.append(values[1]);
                sb.append("\n绕Y轴转过的角度：");
                sb.append(values[2]);
                textView.setText(sb.toString());
                direction1 = values[0];
                break;
        }

        if (values[0]>direction&&values[0]>28.5f){
            while((values[0]-direction)>=28f&&(values[0]-direction)<=28.5f){
                takePicture();
                direction = values[0];
                imageinfo.add(String.valueOf(values[0]));
                Log.d("照片角度","daxiao"+imageinfo.size());

            }

        }else if(values[0]<28.5f){
            while((values[0]+360-direction)>=28f&&(values[0]+360-direction)<=28.5f){
                takePicture();
                direction = values[0];
                imageinfo.add(String.valueOf(values[0]));
                Log.d("照片角度","daxiao"+imageinfo.size());

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // 为Size定义一个比较器Comparator
    static class CompareSizesByArea implements Comparator<Size>
    {
        @Override
        public int compare(Size lhs, Size rhs)
        {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }



    /**摄像头状态的监听*/
    private CameraDevice.StateCallback stateCallback = new CameraDevice. StateCallback()
    {
        // 摄像头被打开时触发该方法
        @Override
        public void onOpened(CameraDevice cameraDevice){
            CameraActivity.this.cameraDevice = cameraDevice;
            // 开始预览
            takePreview();
        }

        // 摄像头断开连接时触发该方法
        @Override
        public void onDisconnected(CameraDevice cameraDevice)
        {
            CameraActivity.this.cameraDevice.close();
            CameraActivity.this.cameraDevice = null;

        }
        // 打开摄像头出现错误时触发该方法
        @Override
        public void onError(CameraDevice cameraDevice, int error)
        {
            cameraDevice.close();
        }
    };

    /**
     * 开始预览
     */
    private void takePreview() {
        SurfaceTexture mSurfaceTexture = tv.getSurfaceTexture();
        //设置TextureView的缓冲区大小
        mSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        //获取Surface显示预览数据
        Surface mSurface = new Surface(mSurfaceTexture);
        try {
            //创建预览请求
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 设置自动对焦模式
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //设置Surface作为预览数据的显示界面
            mCaptureRequestBuilder.addTarget(mSurface);
            //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            cameraDevice.createCaptureSession(Arrays.asList(mSurface,imageReader.getSurface()),new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        //开始预览
                        mCaptureRequest = mCaptureRequestBuilder.build();
                        mPreviewSession = session;
                        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                        mPreviewSession.setRepeatingRequest(mCaptureRequest, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    /**拍照*/
    private void takePicture()
    {
        try
        {
            if (cameraDevice == null)
            {
                return;
            }
            // 创建拍照请求
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 设置自动对焦模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 将imageReader的surface设为目标
            captureRequestBuilder.addTarget(imageReader.getSurface());
            // 获取设备方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION
                    , ORIENTATIONS.get(rotation));

            // 停止连续取景
            mPreviewSession.stopRepeating();
            //拍照
            CaptureRequest captureRequest = captureRequestBuilder.build();
            //设置拍照监听
            mPreviewSession.capture(captureRequest,captureCallback, null);

        }
        catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    /**监听拍照结果*/
    private CameraCaptureSession.CaptureCallback captureCallback= new CameraCaptureSession.CaptureCallback()
    {
        // 拍照成功
        @Override
        public void onCaptureCompleted(CameraCaptureSession session,CaptureRequest request,TotalCaptureResult result)
        {
            // 重设自动对焦模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            // 设置自动曝光模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            try {
                //重新进行预览
                mPreviewSession.setRepeatingRequest(mCaptureRequest, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    /**监听拍照的图片*/
    private ImageReader.OnImageAvailableListener imageAvailableListener= new ImageReader.OnImageAvailableListener()
    {
        // 当照片数据可用时激发该方法
        @Override
        public void onImageAvailable(ImageReader reader) {

            //先验证手机是否有sdcard
            String status = Environment.getExternalStorageState();
            if (!status.equals(Environment.MEDIA_MOUNTED)) {
                Toast.makeText(getApplicationContext(), "你的sd卡不可用。", Toast.LENGTH_SHORT).show();
                return;
            }
            // 获取捕获的照片数据
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            listImage.add(bitmap);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            //matrix.postScale(0.5f,0.5f);//缩小照片像素为原来的一半
            Bitmap bitmap_new = Bitmap.createBitmap(bitmap,0,0,
                    bitmap.getWidth(),bitmap.getHeight(),matrix,true);


            //手机拍照都是存到这个路径
            String filePath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/PanoCam/";
            //String picturePath = System.currentTimeMillis() + ".jpg";
            String picturePath ="Pano_"+ new PhotoCapture(CameraActivity.this).getSystemTime() + ".jpg";

            File file = new File(filePath, picturePath);

            try {
                //存到本地相册
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                bitmap_new.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
               // fileOutputStream.write(data);
                fileOutputStream.flush();
                fileOutputStream.close();
                Log.d("storageimage","imagesaved");
                Uri uri =Uri.fromFile(file);
                mResults.add(String.valueOf(uri));

                //显示图片
                /*BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                listImage.add(bitmap);
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap bitmap_new = Bitmap.createBitmap(bitmap,0,0,
                        bitmap.getWidth(),bitmap.getHeight(),matrix,true);*/
                iv.setImageBitmap(bitmap_new);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                image.close();
            }

            CameraActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" +picturePath)));

        }


    };

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        switch(permsRequestCode){
            case RESULT_CODE_CAMERA:
                boolean cameraAccepted = grantResults[0]==PackageManager.PERMISSION_GRANTED;
                if(cameraAccepted){
                    //授权成功之后，调用系统相机进行拍照操作等
                    openCamera();
                }else{
                    //用户授权拒绝之后，友情提示一下就可以了
                    Toast.makeText(CameraActivity.this,"请开启应用拍照权限",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**启动拍照*/
    private void startCamera(){
        if (tv.isAvailable()) {
            if(cameraDevice==null) {
                openCamera();
            }
        } else {
            tv.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    /**
     * 停止拍照释放资源*/
    private void stopCamera(){
        if(cameraDevice!=null){
            cameraDevice.close();
            cameraDevice=null;
        }

    }
}
