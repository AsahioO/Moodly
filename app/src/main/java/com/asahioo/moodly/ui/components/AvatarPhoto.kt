package com.asahioo.moodly.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val SIZE = 512

/** Foto de perfil guardada en filesDir; null si no hay o no se pudo leer. */
@Composable
fun Avatar(file: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(null, file) {
        value = file?.let {
            withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(File(context.filesDir, it).path)?.asImageBitmap()
            }
        }
    }
    val p = photo
    if (p == null) Avatar(modifier)
    else Box(modifier.clip(CircleShape)) {
        Image(p, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

/**
 * Copia [uri] a filesDir como JPEG cuadrado de 512 px (recorte central, orientación EXIF aplicada),
 * borra la foto anterior y devuelve el nombre nuevo; null si la imagen no se pudo leer.
 */
suspend fun saveAvatar(context: Context, uri: Uri, previous: String?): String? = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@withContext null
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f // ponytail: ignora orientaciones con espejo (raras); añadir si aparece
            }
        } ?: 0f
        val side = minOf(decoded.width, decoded.height)
        val square = Bitmap.createBitmap(
            decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side,
            Matrix().apply { postRotate(rotation); if (side > SIZE) postScale(SIZE / side.toFloat(), SIZE / side.toFloat()) },
            true,
        )
        val name = "avatar_${System.currentTimeMillis()}.jpg"
        File(context.filesDir, name).outputStream().use { square.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        previous?.let { File(context.filesDir, it).delete() }
        name
    } catch (e: Exception) {
        null
    }
}
