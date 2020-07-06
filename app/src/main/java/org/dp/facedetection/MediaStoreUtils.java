package org.dp.facedetection;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * 媒体库工具
 *
 * @author Created by jiang on 2017/3/19.
 */

public class MediaStoreUtils {
    private static final String IMAGE_MEDIA = "image/*";
    private static final String AUDIO_MEDIA = "audio/*";
    private static final String VIDEO_MEDIA = "video/*";

    /**
     * 使用Intent.ACTION_PICK选择照片
     *
     * @param presenter   当前界面activity或者fragment
     * @param requestCode 请求码
     */
    public static void pickImageUsePick(Object presenter, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_MEDIA);
        launchForResult(presenter, intent, requestCode);
    }

    /**
     * 选择相册照片
     *
     * @param presenter   当前界面activity或者fragment
     * @param requestCode 请求码
     */
    public static void pickImage(Object presenter, int requestCode) {
        pickMedia(presenter, requestCode, IMAGE_MEDIA);
    }

    /**
     * 选择手机内的音频文件
     *
     * @param presenter   当前界面activity或者fragment
     * @param requestCode 请求码
     */
    public static void pickAudio(Object presenter, int requestCode) {
        pickMedia(presenter, requestCode, AUDIO_MEDIA);
    }

    /**
     * 选择手机内的视频文件
     *
     * @param presenter   当前界面activity或者fragment
     * @param requestCode 请求码
     */
    public static void pickVideo(Object presenter, int requestCode) {
        pickMedia(presenter, requestCode, VIDEO_MEDIA);
    }

    private static void pickMedia(Object presenter, int requestCode, String type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // 同时选择多种类型，使用';'分隔
        intent.setType(type);
        launchForResult(presenter, intent, requestCode);
    }

    /**
     * 调用系统相机拍照，注意：路径必须配置{@link FileProvider}支持，否则在>=7.0的某些手机上将出现
     * {@link android.os.FileUriExposedException}异常
     * <p>
     * 如果应用目标版本>=23，又在Manifest中申明了CAMERA权限，但是CAMERA被禁止，则会报SecurityException异常
     *
     * @param presenter   当前界面activity或者fragment
     * @param requestCode 请求码
     * @param savePath    照片保存路径
     */
    public static void takePhoto(Object presenter, int requestCode, String savePath) {
        Context context = getContext(presenter);
        if (context == null) {
            return;
        }
        if (TextUtils.isEmpty(savePath)) {
            Toast.makeText(context, "拍照保存路径不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        File file = new File(savePath);
        File photoDir = file.getParentFile();
        if (!photoDir.exists() && !photoDir.mkdirs()) {
            Toast.makeText(context, "拍照保存目录创建失败", Toast.LENGTH_LONG).show();
            return;
        }
        Uri photoUri = getIntentUri(context, file);
        if (photoUri != null) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            launchForResult(presenter, intent, requestCode);
        }
    }

    /**
     * 调用系统录音机录制音频
     *
     * @param presenter   当前界面activity或者fragment
     * @param requestCode 请求码
     * @param maxBytes    录音文件最大字节限制
     */
    public static void recordAudio(Object presenter, int requestCode, long maxBytes) {
        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        intent.putExtra(MediaStore.Audio.Media.EXTRA_MAX_BYTES, maxBytes);
        launchForResult(presenter, intent, requestCode);
    }

    /**
     * 调用系统相机录制视频，注意：路径必须配置{@link FileProvider}支持，否则在>=7.0的某些手机上将出现
     * {@link android.os.FileUriExposedException}异常
     *
     * @param presenter   当前界面
     * @param requestCode 请求码
     * @param sizeLimit   视频大小限制
     * @param savePath    视频保存目录
     */
    public static void recordVideo(Object presenter, int requestCode, long sizeLimit, String savePath) {
        Context context = getContext(presenter);
        if (context == null) {
            return;
        }
        if (TextUtils.isEmpty(savePath)) {
            Toast.makeText(context, "录像保存路径不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        File file = new File(savePath);
        File videoDir = file.getParentFile();
        if (!videoDir.exists() && !videoDir.mkdirs()) {
            Toast.makeText(context, "录像保存目录创建失败", Toast.LENGTH_LONG).show();
            return;
        }
        Uri videoUri = getIntentUri(context, file);
        if (videoUri != null) {
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
            intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, sizeLimit);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
            intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            launchForResult(presenter, intent, requestCode);
        }
    }

    /**
     * 根据媒体Uri路径查询实际文件地址(只用于图片、音频和视频)
     *
     * @param context 上下文
     * @param uri     媒体文件Uri路径
     * @return 文件实际路径
     */
    public static String getMediaPath(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        String path = uri.getPath();
        if (!TextUtils.isEmpty(path) && new File(path).exists()) {
            return path;
        }

        String authority = uri.getAuthority();
        if ("com.android.providers.media.documents".equals(authority)) {
            String wholeId = DocumentsContract.getDocumentId(uri);
            String[] typeAndId = wholeId.split(":");
            String type = typeAndId[0];
            if ("primary".equalsIgnoreCase(type)) {
                path = Environment.getExternalStorageDirectory() + "/" + typeAndId[1];
                return path;
            } else {
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String selection = "_id=?";
                String[] selectionArgs = new String[]{typeAndId[1]};
                if (contentUri != null) {
                    String[] pathColumn = {MediaStore.MediaColumns.DATA};
                    Cursor cursor = context.getContentResolver().query(contentUri, pathColumn, selection, selectionArgs, null);
                    if (cursor == null) {
                        return null;
                    }
                    try {
                        int index = cursor.getColumnIndexOrThrow(pathColumn[0]);
                        cursor.moveToNext();
                        path = cursor.getString(index);
                        return path;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        cursor.close();
                    }
                }
            }
        } else {
            String[] pathColumn = {MediaStore.MediaColumns.DATA};
            Cursor cursor = context.getContentResolver().query(uri, pathColumn, null, null, null);
            if (cursor == null) {
                return null;
            }
            try {
                int index = cursor.getColumnIndexOrThrow(pathColumn[0]);
                cursor.moveToNext();
                path = cursor.getString(index);
                return path;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }

        return null;
    }

    /**
     * 调用系统播放器播放视频，注意：路径必须配置{@link FileProvider}支持，否则在>=7.0的某些手机上将出现
     * {@link android.os.FileUriExposedException}异常
     *
     * @param context   上下文
     * @param videoPath 视频路径
     */
    public static void playVideo(Context context, String videoPath) {
        if (TextUtils.isEmpty(videoPath)) {
            Toast.makeText(context, "视频资源路径不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            Toast.makeText(context, "视频资源不存在", Toast.LENGTH_LONG).show();
            return;
        }
        Uri videoUri = getIntentUri(context, videoFile);
        if (videoUri != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, false);
            intent.setDataAndType(videoUri, VIDEO_MEDIA);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        }
    }

    /**
     * 裁剪图片，注意：路径必须配置{@link FileProvider}支持，否则在>=7.0的某些手机上将出现
     * {@link android.os.FileUriExposedException}异常
     *
     * @param presenter   当前界面
     * @param imagePath   待裁剪图片地址
     * @param w           目标宽
     * @param h           目标高
     * @param requestCode 请求码
     * @param savePath    保存地址
     */
    public static void cropImage(Object presenter, String imagePath, int w, int h, int requestCode, String savePath) {
        File file = new File(imagePath);
        Context context = getContext(presenter);
        if (context == null) {
            return;
        }
        if (!file.exists()) {
            Toast.makeText(context, "图片文件不存在", Toast.LENGTH_LONG).show();
            return;
        }
        Uri imageUri = getIntentUri(context, file);
        if (imageUri != null) {
            cropImage(presenter, imageUri, w, h, requestCode, savePath);
        }
    }

    /**
     * 裁剪图片，注意：路径必须配置{@link FileProvider}支持，否则在>=7.0的某些手机上将出现
     * {@link android.os.FileUriExposedException}异常
     *
     * @param presenter   当前界面
     * @param imageUri    待裁剪图片地址
     * @param w           目标宽
     * @param h           目标高
     * @param requestCode 请求码
     * @param savePath    保存地址
     */
    public static void cropImage(Object presenter, Uri imageUri, int w, int h, int requestCode, String savePath) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(imageUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", w);
        intent.putExtra("outputY", h);
        intent.putExtra("return-data", false);
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);

        File file = new File(savePath);
        File dir = file.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(getContext(presenter), "裁剪保存目录创建失败", Toast.LENGTH_LONG).show();
            return;
        }

        // vivo >= 7.0的手机使用FileProvider生成输出路径保存不成功
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(savePath)));
        intent.putExtra("outputFormat", "JPEG");
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        launchForResult(presenter, intent, requestCode);
    }

    /**
     * 文件路径转为Uri路径
     *
     * @param context 上下文
     * @param file    文件
     * @return 文件Uri地址
     */
    public static Uri getIntentUri(Context context, File file) {
        if (file == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    private static void launchForResult(Object presenter, Intent intent, int requestCode) {
        if (presenter instanceof Activity) {
            ((Activity) presenter).startActivityForResult(intent, requestCode);
        } else if (presenter instanceof Fragment) {
            ((Fragment) presenter).startActivityForResult(intent, requestCode);
        } else if (presenter instanceof androidx.fragment.app.Fragment) {
            ((androidx.fragment.app.Fragment) presenter).startActivityForResult(intent, requestCode);
        }
    }

    private static Context getContext(Object presenter) {
        if (presenter instanceof Activity) {
            return (Activity) presenter;
        } else if (presenter instanceof Fragment) {
            return ((Fragment) presenter).getActivity();
        } else if (presenter instanceof androidx.fragment.app.Fragment) {
            return ((androidx.fragment.app.Fragment) presenter).getActivity();
        }
        return null;
    }
}
