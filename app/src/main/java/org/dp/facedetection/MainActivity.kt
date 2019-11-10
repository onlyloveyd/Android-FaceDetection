package org.dp.facedetection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.Utils
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.io.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example of a call to facedetect method
        testFacedetect()
    }

    fun testFacedetect() {
        val bmp = getImageFromAssets("test2.jpg") ?: return
        var str = "image size = ${bmp.width}x${bmp.height}\n"
        imageView.setImageBitmap(bmp)
        val mat = MatOfRect()
        val bmp2 = bmp.copy(bmp.config, true)
        Utils.bitmapToMat(bmp, mat)
        val FACE_RECT_COLOR = Scalar(255.0, 0.0, 0.0, 0.0)
        val startTime = System.currentTimeMillis()
        val facesArray = facedetect(mat.nativeObjAddr)
        str = str + "face num = ${facesArray.size}\n"


        for (face in facesArray) {
            val start = Point(face.faceRect.x.toDouble(), face.faceRect.y.toDouble())
            val end = Point(
                face.faceRect.x.toDouble() + face.faceRect.width,
                face.faceRect.y.toDouble() + face.faceRect.height
            )
            Imgproc.rectangle(mat, start, end, FACE_RECT_COLOR)
        }
        str = str + "detectTime = ${System.currentTimeMillis() - startTime}ms\n"
        Utils.matToBitmap(mat, bmp2)
        imageView.setImageBitmap(bmp2)
        textView.text = str
    }

    /**
     * A native method that is implemented by the 'libfacedetection' native library,
     * which is packaged with this application.
     */
    external fun facedetect(matAddr: Long): Array<Face>


    companion object {

        // Used to load the 'facedetection' library on application startup.
        init {
            System.loadLibrary("facedetection")
        }
    }


    /**
     * read from Assets
     */
    private fun getImageFromAssets(fileName: String): Bitmap? {
        var image: Bitmap? = null
        try {
            val stream = resources.assets.open(fileName)
            image = BitmapFactory.decodeStream(stream)
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            return image
        }
    }
}