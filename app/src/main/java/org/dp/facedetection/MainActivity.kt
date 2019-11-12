package org.dp.facedetection

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.PicassoEngine
import com.zhihu.matisse.internal.entity.CaptureStrategy
import org.opencv.android.Utils
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var tvMessage: TextView
    private lateinit var btTakePhoto: Button
    private lateinit var ivImage: ImageView

    /**
     * 手机外部存储目录
     */
    val SDCARD = Environment.getExternalStorageDirectory().absolutePath

    /**
     * 应用根目录，配置FileProvider支持
     */
    val APP_ROOT_DIR = SDCARD + File.separator + "pvms"

    val TEMP_DIR = APP_ROOT_DIR + File.separator + "temp"

    private val REQUEST_CODE_CHOOSE = 1001

    lateinit var selectedPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvMessage = findViewById(R.id.textView)
        ivImage = findViewById(R.id.imageView)
        btTakePhoto = findViewById(R.id.btPhoto)
        btTakePhoto.setOnClickListener {
            Matisse.from(this)
                .choose(MimeType.ofImage())
                .countable(true)
                .capture(true)
                .captureStrategy(
                    CaptureStrategy(
                        true,
                        "org.dp.facedetection.fileprovider",
                        "facedetection"
                    )
                )
                .maxSelectable(1)
                .imageEngine(PicassoEngine())
                .forResult(REQUEST_CODE_CHOOSE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_CHOOSE) {
            selectedPath = Matisse.obtainPathResult(data)[0]
            compressPic(selectedPath)
        }
    }

    fun compressPic(picPath: String) {
        val tempDir = File(TEMP_DIR)
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            Toast.makeText(this, "创建临时目录失败，不能压缩图片", Toast.LENGTH_SHORT).show()
            return
        }

        Luban.with(this)
            .load(picPath)
            .ignoreBy(180)
            .setFocusAlpha(true)
            .setTargetDir(TEMP_DIR)
            .filter { path -> !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif")) }
            .setCompressListener(object : OnCompressListener {
                override fun onStart() {
                }

                override fun onSuccess(file: File?) {
                    file?.let {
                        fileFaceDetection(it)
                    }
                }

                override fun onError(e: Throwable?) {
                    Toast.makeText(
                        this@MainActivity,
                        "图片压缩失败：" + e.toString(),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }).launch()
    }

    /**
     * File to Bitmap
     */
    fun getBitmapFromFile(file: File): Bitmap? {
        var image: Bitmap? = null
        try {
            val stream = FileInputStream(file)
            image = BitmapFactory.decodeStream(stream)
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            return image
        }
    }

    fun fileFaceDetection(file: File) {
        val bmp = getBitmapFromFile(file)
        var str = "image size = ${bmp?.width}x${bmp?.height} ${getFileSize(file)}\n"
        ivImage.setImageBitmap(bmp)
        val mat = MatOfRect()
        val bmp2 = bmp?.copy(bmp.config, true)
        Utils.bitmapToMat(bmp, mat)
        val FACE_RECT_COLOR = Scalar(255.0, 0.0, 0.0, 0.0)
        val startTime = System.currentTimeMillis()
        val facesArray = facedetect(mat.nativeObjAddr)
        str += "face num = ${facesArray.size}\n"
        for (face in facesArray) {
            str += "confidence = ${face.faceConfidence} x = ${face.faceRect.x} y = ${face.faceRect.y} width = ${face.faceRect.width} height = ${face.faceRect.height}\n"
            val start = Point(face.faceRect.x.toDouble(), face.faceRect.y.toDouble())
            val end = Point(
                face.faceRect.x.toDouble() + face.faceRect.width,
                face.faceRect.y.toDouble() + face.faceRect.height
            )
            Imgproc.rectangle(mat, start, end, FACE_RECT_COLOR)
        }

        str += "detectTime = ${System.currentTimeMillis() - startTime}ms\n"
        Utils.matToBitmap(mat, bmp2)
        ivImage.setImageBitmap(bmp2)
        tvMessage.text = str
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

    fun getFileSize(file: File): String {
        var size: String
        if (file.exists() && file.isFile) {
            val fileS = file.length()
            val df = DecimalFormat("#.00")
            if (fileS < 1024) {
                size = df.format(fileS.toDouble()) + "BT"
            } else if (fileS < 1048576) {
                size = df.format(fileS.toDouble() / 1024) + "KB"
            } else if (fileS < 1073741824) {
                size = df.format(fileS.toDouble() / 1048576) + "MB"
            } else {
                size = df.format(fileS.toDouble() / 1073741824) + "GB"
            }
        } else if (file.exists() && file.isDirectory) {
            size = ""
        } else {
            size = "0BT"
        }
        return size
    }
}