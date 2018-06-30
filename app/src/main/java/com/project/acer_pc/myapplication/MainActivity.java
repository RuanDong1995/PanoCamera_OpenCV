package com.project.acer_pc.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.vr.sdk.widgets.pano.VrPanoramaView;
import com.zfdang.multiple_images_selector.ImagesSelectorActivity;
import com.zfdang.multiple_images_selector.SelectorSettings;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final int CLICK_PHOTO = 1;
    private final int SELECT_PHOTO = 2;
    private Uri imageUri;
    private TextView tvResults,imageinfo;
    private VrPanoramaView vr_pan_view;
    private Mat src;
    private List<Mat> listImage	= new ArrayList<>();
    private ArrayList<String> mResults = new ArrayList<>();
    static int REQUEST_READ_EXTERNAL_STORAGE = 11;
    static boolean read_external_storage_granted = false;
    private final String TAG = "VrPanoramaView";
    private long systemTime1;
    private long systemTime2;
    PhotoSelect photoSelect = new PhotoSelect();
    PhotoCapture photoCapture = new PhotoCapture(MainActivity.this);
    private View pb;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    System.loadLibrary("stitcher");
                    //DO YOUR WORK/STUFF HERE
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Fresco.initialize(this);
        vr_pan_view = (VrPanoramaView) findViewById(R.id.vr_pan_view);


        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("permission", "request READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
        }else {
            Log.i("permission", "READ_EXTERNAL_STORAGE already granted");
            read_external_storage_granted = true;
        }

        Button bClickImage, bDone,select;
        bClickImage = (Button)findViewById(R.id.bClick);
        bDone = (Button)findViewById(R.id.bDone);
        select = (Button)findViewById(R.id.select);
        tvResults = (TextView)findViewById(R.id.tvResults);
        imageinfo = (TextView)findViewById(R.id.imageinfo);
        initUI();
       // pb = findViewById(R.id.pb);

        //点击拍照按钮
        bClickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setClass(MainActivity.this,CameraActivity.class);
                //startActivity(intent);
                startActivityForResult(intent, CLICK_PHOTO);


            }
        });


        //点击完成按钮
        bDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                systemTime1 = System.currentTimeMillis();//获取开始拼接的时间
                //systemTime1 = photoCapture.getSystemTime();//获取打开相机的时间
                if(listImage.size()==0){
                    Toast.makeText(getApplicationContext(), "没有选择图片", Toast.LENGTH_SHORT).show();
                } else if(listImage.size()==1){
                    Toast.makeText(getApplicationContext(), "只选择了一张图片", Toast.LENGTH_SHORT).show();
                    Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2BGRA);
                    Bitmap image = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(src, image);

                    /**设置加载VR图片的相关设置**/
                    VrPanoramaView.Options options = new VrPanoramaView.Options();
                    options.inputType = VrPanoramaView.Options.TYPE_MONO;
                    vr_pan_view.loadImageFromBitmap(image,options);
                    vr_pan_view.setInfoButtonEnabled(false);
                } else {
                    createPanorama();
                }
            }
        });

        //图片选择按钮
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listImage.clear();
                mResults.clear();
                // 图片选择
                Intent intent = new Intent(MainActivity.this, ImagesSelectorActivity.class);
                // 最大选择数量
                intent.putExtra(SelectorSettings.SELECTOR_MAX_IMAGE_NUMBER, 20);
                // 最小选择数量
                intent.putExtra(SelectorSettings.SELECTOR_MIN_IMAGE_SIZE, 100000);
                // 显示摄像头
                intent.putExtra(SelectorSettings.SELECTOR_SHOW_CAMERA, false);
                // 将选择图片加入数组
                intent.putStringArrayListExtra(SelectorSettings.SELECTOR_INITIAL_SELECTED_LIST, mResults);
                // 开始选择
                startActivityForResult(intent, SELECT_PHOTO);
            }
        });
    }

    private void initUI(){
        InputStream open = null;
        try {
            open = getAssets().open("andes.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(open);

        /**设置加载VR图片的相关设置**/
        VrPanoramaView.Options options = new VrPanoramaView.Options();
        options.inputType = VrPanoramaView.Options.TYPE_MONO;
        vr_pan_view.loadImageFromBitmap(bitmap,options);
        vr_pan_view.setInfoButtonEnabled(false);
        imageinfo.setText("图片示例");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Log.d("MainActivity", "request code " + requestCode + ", click photo "
                + CLICK_PHOTO + ", result code " + resultCode + ", result ok " + RESULT_OK);
        switch(requestCode) {
            case CLICK_PHOTO:
                if(resultCode == 1000){

                    Log.d("pressback","主界面");
                    //Intent intent = getIntent();
                    //mResults.addAll(intent.getStringArrayListExtra("result"));

                    mResults = imageReturnedIntent.getStringArrayListExtra("result");
                    Log.d("resultsize","result size is"+mResults.size());
                   //Log.d("image1","imagepath1"+mResults.get(0));
                    if(mResults != null){
                    for(int i=mResults.size()-1;i>=0;i--) {
                        select(Uri.parse(mResults.get(i)));
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("选择以下 %d 张图片进行拼接:", mResults.size())).append("\n");
                    for(String result : mResults) {
                        sb.append(result).append("\n");
                    }
                    tvResults.setText(sb.toString());
                    createPanorama();
                        }
                    else {


                    }
                }
                break;

            case SELECT_PHOTO:
                if(resultCode == RESULT_OK) {
                    mResults = imageReturnedIntent.getStringArrayListExtra(SelectorSettings.SELECTOR_RESULTS);
                    assert mResults != null;
                    for(int i=mResults.size()-1;i>=0;i--) {
                            select(photoSelect.getImageContentUri(this,(String)mResults.get(i)));
                        }
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("选择以下 %d 张图片进行拼接:", mResults.size())).append("\n");
                    for(String result : mResults) {
                        sb.append(result).append("\n");
                    }
                    tvResults.setText(sb.toString());
                    createPanorama();
                }
        }
    }


    //加入图片数组
   public void select(Uri uri){
        try {
            final InputStream imageStream = getContentResolver().openInputStream(uri);
            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            src = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC4);
            Imgproc.resize(src, src, new Size(src.rows() / 4, src.cols() / 4));
            Log.d("原来图片像素",selectedImage.getWidth()+"*"+selectedImage.getHeight());
            Log.d("选择图片像素",src.rows()+"*"+src.cols());
            tvResults.append(selectedImage.getWidth()+"*"+selectedImage.getHeight());
            Utils.bitmapToMat(selectedImage, src);
            Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2BGR);
            listImage.add(src);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                Log.i("permission", "READ_EXTERNAL_STORAGE granted");
                read_external_storage_granted = true;
            } else {
                // permission denied
                Log.i("permission", "READ_EXTERNAL_STORAGE denied");
            }
        }
    }

    public void createPanorama(){
        systemTime1 = System.currentTimeMillis();//获取开始拼接的时间
        //拼接过程耗时，在异步线程中进行
        new AsyncTask<Void, Void, Bitmap>() {
            ProgressDialog dialog;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = ProgressDialog.show(MainActivity.this, "正在拼接全景图片", "请稍后...");
            }
            @Override
            protected Bitmap doInBackground(Void... params) {
                int	elems = listImage.size();
                long[] tempobjadr = new long[elems];
                for	(int i=0; i<elems; i++){
                    tempobjadr[i] = listImage.get(i).getNativeObjAddr();
                }
                Mat result = new Mat();
                int stitchstatus = StitchPanorama(tempobjadr, result.getNativeObjAddr());
                Log.d("MainActivity", "result height " + result.rows() + ", result width " + result.cols());
                //tvResults.append("拼接结果像素 " + result.rows() + "*" + result.cols());
                if(stitchstatus != 0){
                    Log.e("MainActivity", "Stitching failed: " + stitchstatus);
                    return null;
                }
                Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2RGBA);
                Bitmap bitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(result, bitmap);
                MediaStore.Images.Media.insertImage(getContentResolver(),bitmap,"title","des");
                return bitmap;
            }
            //在主线程中返回拼接结果
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                dialog.dismiss();
                if(bitmap!=null) {
                    /**设置加载VR图片的相关设置**/
                    VrPanoramaView.Options options = new VrPanoramaView.Options();
                   // options.inputType = VrPanoramaView.Options.TYPE_STEREO_OVER_UNDER;
                    options.inputType = VrPanoramaView.Options.TYPE_MONO;//vr显示模式
                    vr_pan_view.loadImageFromBitmap(bitmap,options);
                    systemTime2 = System.currentTimeMillis();//获取拼接完成的时间ms
                    Log.d("Time","stitching time is "+(systemTime2-systemTime1)+" ms");
                    Toast.makeText(getApplicationContext(), "stitching time is "+(systemTime2-systemTime1)/1000+" s", Toast.LENGTH_LONG).show();
                    tvResults.append("拼接完成用时 "+(systemTime2-systemTime1)/1000+" 秒"+"\n"
                                    +"拼接结果像素 " + bitmap.getWidth() + "*" + bitmap.getHeight());
                    imageinfo.setText("你的作品");
                }
            }
        }.execute();
    }


    @Override
    protected void onResume() {
        super.onResume();
        /*Intent intent = getIntent();
        listImage = (List<Mat>) intent.getSerializableExtra("listimage");*/
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mOpenCVCallBack);
            //mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    public native int StitchPanorama(long[]	imageAddressArray, long	outputAddress);

}