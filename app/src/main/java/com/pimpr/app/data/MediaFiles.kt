package com.pimpr.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

data class VideoImport(val dir: String, val frames: Int, val fps: Int)

/** Copie les photos et vidéos choisies dans le stockage privé de l'app, à une taille raisonnable. */
object MediaFiles {
    private fun mediaDir(context: Context) = File(context.filesDir, "media").apply { mkdirs() }

    fun importImage(context: Context, uri: Uri, maxSide: Int = 1600): String? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        var bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scale = min(1f, maxSide.toFloat() / max(bitmap.width, bitmap.height))
        if (rotation != 0f || scale < 1f) {
            val m = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        }
        val file = File(mediaDir(context), "img_${UUID.randomUUID()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        file.path
    }.getOrNull()

    /**
     * Extrait une courte boucle d'images d'une vidéo. Un widget Android ne peut pas lire de
     * vidéo : on l'anime en faisant défiler ces images.
     */
    fun importVideo(context: Context, uri: Uri, frames: Int = 24, fps: Int = 8, maxSide: Int = 480): VideoImport? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (durationMs <= 0L) return null
            val stepMs = 1000L / fps
            val count = min(frames.toLong(), max(1L, durationMs / stepMs)).toInt()
            val dir = File(mediaDir(context), "vid_${UUID.randomUUID()}").apply { mkdirs() }
            var written = 0
            for (i in 0 until count) {
                val timeUs = i * stepMs * 1000L
                val frame = if (Build.VERSION.SDK_INT >= 27) {
                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: maxSide
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: maxSide
                    val s = min(1f, maxSide.toFloat() / max(w, h))
                    retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, max(1, (w * s).toInt()), max(1, (h * s).toInt()))
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                } ?: continue
                File(dir, "f_%03d.jpg".format(written)).outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                written++
            }
            if (written == 0) { dir.deleteRecursively(); null } else VideoImport(dir.path, written, fps)
        } finally {
            retriever.release()
        }
    }.getOrNull()
}
