package org.dp.facedetection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.MatOfRect;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 人脸识别
 *
 * @author yidong
 * @date 2019-11-13
 */
public class FaceDetect {

    static {
        System.loadLibrary("facedetection");
    }

    public static void init() {
        // 调用该方法仅仅是为了加载so库
    }

    private native Face[] faceDetect(long matAddr);

    /**
     * 执行人脸识别
     *
     * @param file 人脸图片文件
     * @return 人脸识别结果，如果发生异常就返回null
     */
    public Face[] doFaceDetect(File file) {
        Bitmap bmp = getBitmapFromFile(file);
        try {
            MatOfRect mat = new MatOfRect();
            Utils.bitmapToMat(bmp, mat);
            return faceDetect(mat.getNativeObjAddr());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * File to Bitmap
     */
    private Bitmap getBitmapFromFile(File file) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            return BitmapFactory.decodeStream(stream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
