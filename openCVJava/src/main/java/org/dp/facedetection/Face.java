package org.dp.facedetection;

import org.opencv.core.Rect;

/**
 * 识别出的人脸信息
 * <p>
 * Stay Hungry Stay Foolish
 * Author: dp on 2019/3/25 12:51
 */
public class Face {
    public Rect faceRect;
    public int faceConfidence;
    public int faceAngle;
}
