package com.mathcoach.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * 图片处理工具：URI → Bitmap → 旋转纠正 → 压缩 → MD5。
 */
object ImageCodec {

    /**
     * 从 URI 读取图片，处理 EXIF 旋转，压缩到指定最大尺寸，返回 JPEG 字节。
     */
    fun uriToJpegBytes(
        context: Context,
        uri: Uri,
        maxDim: Int = 1568,
        quality: Int = 88
    ): ByteArray? {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use(InputStream::readBytes)
            ?: return null
        val bitmap = decodeScaled(rawBytes, maxDim) ?: return null
        val rotated = applyExifRotation(context, uri, bitmap)
        return compressJpeg(rotated, quality)
    }

    fun bitmapToJpegBytes(bitmap: Bitmap, maxDim: Int = 1568, quality: Int = 88): ByteArray {
        val scaled = if (maxOf(bitmap.width, bitmap.height) > maxDim) {
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            val matrix = Matrix().apply { postScale(scale, scale) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else bitmap
        return compressJpeg(scaled, quality)
    }

    private fun decodeScaled(bytes: ByteArray, maxDim: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        val srcW = opts.outWidth
        val srcH = opts.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        var sampleSize = 1
        while (srcW / (sampleSize * 2) >= maxDim && srcH / (sampleSize * 2) >= maxDim) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val input = context.contentResolver.openInputStream(uri) ?: return bitmap
        val exif = input.use { ExifInterface(it) }
        val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun compressJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        return baos.toByteArray()
    }

    /**
     * 计算 MD5，用于缓存 key 和图片哈希。
     */
    fun md5(vararg parts: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        for (p in parts) md.update(p)
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun md5(text: String): String = md5(text.toByteArray(Charsets.UTF_8))
}
