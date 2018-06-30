package com.project.acer_pc.myapplication;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PhotoCapture {
    private Context context;
    public PhotoCapture(Context context) {
        this.context = context;
    }

    public long getSystemTime() {
        //("yyyy年MM月dd日 HH时MM分ss秒"
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        long times = System.currentTimeMillis();
        System.out.println(times);
        Date date = new Date(times);
        String time = sdf.format(date).toString();
        Log.e("timeintimet", "timeint: " + time.toString());
        long timeint = 0;
        try {
            timeint = Long.valueOf(time).longValue();
        } catch (Exception e) {
            Log.e("exception", "getSystemTime: " + e.toString());
        }
        return timeint;
    }

    /**
     * 获取ContentProvider
     *
     * @param projection
     * @param orderBy
     */
    public ArrayList<String> getContentProvider(Uri uri, String[] projection, String orderBy,
                                             long systemTime1, long systemTime2) throws Exception{
        // TODO Auto-generated method stub

        ArrayList<String> list = new ArrayList<>();
        //List<MyBitmap> lists = new ArrayList<MyBitmap>();
        //HashSet<String> set = new HashSet<String>();
        Cursor cursor = context.getContentResolver().query(uri, projection, null,
                null, orderBy);
        if (null == cursor) {
            return null;
        }

        while (cursor.moveToNext()) {
            Log.e("lengthpro", "getContentProvider: " + projection.length);
            for (int i = 0; i < projection.length; i++) {
                String string = cursor.getString(i);
                if (string != null) {
                    int length = string.length();
                    String ss = null;
                    if (length >= 30) {//根据实际路径得到的。大一点保险
                        ss = string.substring(length - 23, length);
                        String substring = ss.substring(0, 4);//大致判断一下是系统图片，后面严格塞选
                        String hen = ss.substring(12, 13);
                        if (substring.equals("IMG_") && hen.equals("_")) {
                            String laststring = ss.substring(4, 19).replace("_", "");
                            try {
                                long time = Long.valueOf(laststring).longValue();
                                if (time > systemTime1 && time <= systemTime2) {
                                    list.add(string);
                                }
                            } catch (Exception e) {
                                Log.e("exception", "getContentProvider: " + e.toString());
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

}
