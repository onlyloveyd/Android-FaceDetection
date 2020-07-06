package org.dp.facedetection

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import org.dp.facedetection.databinding.ActivityMainBinding
import org.luban.Luban
import org.luban.OnCompressListener
import org.opencv.android.Utils
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSION_REQUEST_CAMERA = 2

    private var mTakePhotoSavePath: String = ""


    private val REQUEST_CODE_CHOOSE_IMAGE = 1001
    private val REQUEST_CODE_TAKE_PHOTO = 1002
    lateinit var faceDetect: FaceDetect

    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        faceDetect = FaceDetect()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pick, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.take_photo -> {
                if (checkCameraPermission()) {
                    takePhoto()
                }
            }
            R.id.choose_image -> {
                if (checkExternalStoragePermission()) {
                    chooseImage()
                }
            }
        }
        return true
    }

    private fun checkExternalStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            true
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), PERMISSION_REQUEST_EXTERNAL_STORAGE
                )
                false
            } else {
                true
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            true
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.CAMERA
                    ), PERMISSION_REQUEST_CAMERA
                )
                false
            } else {
                true
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseImage()
            }
        } else if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            }
        }
    }

    private fun takePhoto() {
        mTakePhotoSavePath =
            cacheDir.absolutePath + File.separator + System.currentTimeMillis() + ".jpg"
        MediaStoreUtils.takePhoto(this, REQUEST_CODE_TAKE_PHOTO, mTakePhotoSavePath)
    }

    private fun chooseImage() {
        MediaStoreUtils.pickImage(this, REQUEST_CODE_CHOOSE_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode == REQUEST_CODE_TAKE_PHOTO) {
            compressPic(mTakePhotoSavePath)
            mTakePhotoSavePath = ""
        } else if (requestCode == REQUEST_CODE_CHOOSE_IMAGE) {
            val uri = data?.data
            val path = MediaStoreUtils.getMediaPath(this, uri)
            if (path != null) {
                compressPic(path)
            }
        }
    }

    private fun compressPic(picPath: String) {
        Luban.with(this)
            .load(picPath)
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
    private fun getBitmapFromFile(file: File): Bitmap? {
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
        mBinding.imageView.setImageBitmap(bmp)
        val mat = MatOfRect()
        val bmp2 = bmp?.copy(bmp.config, true)
        Utils.bitmapToMat(bmp, mat)
        val FACE_RECT_COLOR = Scalar(255.0, 0.0, 0.0, 0.0)
        val startTime = System.currentTimeMillis()
        val facesArray = faceDetect.doFaceDetect(file)
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
        mBinding.imageView.setImageBitmap(bmp2)
        mBinding.textView.text = str
    }

    private fun getFileSize(file: File): String {
        val size: String
        if (file.exists() && file.isFile) {
            val fileS = file.length()
            val df = DecimalFormat("#.00")
            size = when {
                fileS < 1024 -> {
                    df.format(fileS.toDouble()) + "BT"
                }
                fileS < 1048576 -> {
                    df.format(fileS.toDouble() / 1024) + "KB"
                }
                fileS < 1073741824 -> {
                    df.format(fileS.toDouble() / 1048576) + "MB"
                }
                else -> {
                    df.format(fileS.toDouble() / 1073741824) + "GB"
                }
            }
        } else if (file.exists() && file.isDirectory) {
            size = ""
        } else {
            size = "0BT"
        }
        return size
    }
}