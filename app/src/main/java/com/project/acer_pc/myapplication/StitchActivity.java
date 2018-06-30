package com.project.acer_pc.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StitchActivity extends Activity{
    private Mat image_new;
    private ImageView stitchimage,stitch1,stitch2;
    private List<String> listimage= new ArrayList<>();
    private Bitmap bitmap1;
    private Bitmap bitmap2;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stitch);
        stitchimage = (ImageView)findViewById(R.id.stitched);
        stitch1 = (ImageView)findViewById(R.id.stitche_01);
        stitch2 = (ImageView)findViewById(R.id.stitch_02);
        Intent intent = getIntent();
        listimage = intent.getStringArrayListExtra("result");
        bitmap1 = uritobitmap(Uri.parse(listimage.get(0)));
        stitch1.setImageBitmap(bitmap1);
        bitmap2 = uritobitmap(Uri.parse(listimage.get(1)));
        stitch2.setImageBitmap(bitmap2);
        panostitch(bitmap1,bitmap2);

    }


    public void panostitch(Bitmap bitmap1, Bitmap bitmap2){
        Mat image_one = new Mat(bitmap1.getHeight(),bitmap1.getWidth(),CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap1,image_one);
        Imgproc.cvtColor(image_one,image_one,Imgproc.COLOR_BGR2BGRA);
        Mat image_two = new Mat(bitmap2.getHeight(),bitmap2.getWidth(),CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap2,image_two);
        Imgproc.cvtColor(image_two,image_two,Imgproc.COLOR_BGR2BGRA);
        int totalcols = image_one.cols()+(image_two.cols()/45)*2;
        image_new = new Mat(image_one.rows(),totalcols,image_one.type());
        Mat submat = image_new.colRange(0,image_one.cols());
        image_one.copyTo(submat);
        submat = image_new.colRange(image_one.cols(),totalcols);
        image_two.colRange(image_two.cols()-(image_two.cols()/45)*2,image_two.cols()).copyTo(submat);
        Log.d("图片像素","图片一像素"+image_one.cols()+"图片二"+image_two.cols()+"新图："+image_new.cols());

        Bitmap bitmap = Bitmap.createBitmap(image_new.cols(), image_new.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image_new, bitmap);
        Imgproc.cvtColor(image_new, image_new, Imgproc.COLOR_BGR2RGBA);
        stitchimage.setImageBitmap(bitmap);
    }

    public Bitmap uritobitmap(Uri uri){

        try {
            final InputStream imageStream;
            imageStream = getContentResolver().openInputStream(uri);
            final Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            return bitmap;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
