package org.dp.facedetection;

import android.app.Application;

/**
 * 描述
 *
 * @author jiangyujiang
 * @date 2019-11-13
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FaceDetect.init();
    }
}
