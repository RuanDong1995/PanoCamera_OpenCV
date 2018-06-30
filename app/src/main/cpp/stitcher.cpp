#include <jni.h>
#include <vector>
#include <android/log.h>

#include "opencv2/opencv.hpp"
#include "include/opencv2/calib3d.hpp"
#include "include/opencv2/core.hpp"
#include "include/opencv2/imgcodecs.hpp"

using namespace cv;
using namespace std;

char filepath1[100] = "/storage/emulated/0/Download/panorama_stitched.jpg";

#define  LOG_TAG    "Stitching"

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)


extern "C"
{
JNIEXPORT jint JNICALL
Java_com_project_acer_1pc_myapplication_MainActivity_StitchPanorama(JNIEnv *env, jobject,
                                                          jlongArray imageAddressArray,
                                                          jlong outputAddress) {
    jsize a_len = env->GetArrayLength(imageAddressArray);
    jlong *imgAddressArr = env->GetLongArrayElements(imageAddressArray,0);
    vector<Mat> imgVec;
    for(int	k=0; k<a_len; k++)
    {
        Mat	&curimage = *(Mat*)imgAddressArr[k];
        Mat	newimage;
        curimage.copyTo(newimage);//把curimage复制到new
        float scale = 3000.0f / curimage.rows;
        //resize(newimage, newimage, Size((int)(scale*curimage.cols), (int)(scale*curimage.rows)));
        resize(newimage, newimage, Size((int)(0.5*curimage.cols), (int)(0.5*curimage.rows)));

        LOGD("Image height %d width %d", newimage.rows, newimage.cols);
        imgVec.push_back(newimage);
    }
    Mat	&result = *(Mat*)outputAddress;

    Stitcher stitcher = Stitcher::createDefault(false);
    //Stitcher stitcher = Stitcher::estimateTransform();
    Stitcher::Status status = stitcher.stitch(imgVec, result);

    LOGD("Result height %d width %d", result.rows, result.cols);

    imwrite(filepath1, result);

    return status;
}
}