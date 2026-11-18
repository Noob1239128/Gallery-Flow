package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.CollageLayout
import com.example.data.model.ColorCategory
import com.example.data.model.CropAspect
import com.example.data.model.FrameStyle
import com.example.data.model.PhotoAdjustments
import com.example.data.model.PhotoFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ExtractedPhotoMetadata(
    val filePath: String,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val takenAt: Long,
    val cameraModel: String,
    val lensSpec: String,
    val aperture: String,
    val shutterSpeed: String,
    val iso: Int,
    val paletteCsv: String,
    val colorCategory: ColorCategory
)

class ImageProcessingEngine(private val context: Context) {

    val photosDir: File
        get() = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    private val cameraCacheDir: File
        get() = File(context.cacheDir, "camera").apply { if (!exists()) mkdirs() }

    fun createCameraTempFile(): Pair<File, Uri> {
        val file = File(cameraCacheDir, "cam_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return file to uri
    }

    fun getShareUri(filePath: String): Uri? {
        val file = File(filePath)
        if (!file.exists()) return null
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun saveDrawableAsGalleryFile(drawableResId: Int, fileName: String): ExtractedPhotoMetadata =
        withContext(Dispatchers.IO) {
            val destFile = File(photosDir, fileName)
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableResId)
            if (!destFile.exists()) {
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
            }
            val (paletteCsv, category) = extractPalette(bitmap)
            val w = bitmap.width
            val h = bitmap.height
            bitmap.recycle()

            ExtractedPhotoMetadata(
                filePath = destFile.absolutePath,
                width = w,
                height = h,
                fileSizeBytes = destFile.length(),
                takenAt = System.currentTimeMillis(),
                cameraModel = "Lumina Studio",
                lensSpec = "35mm f/1.4",
                aperture = "f/2.0",
                shutterSpeed = "1/320s",
                iso = 100,
                paletteCsv = paletteCsv,
                colorCategory = category
            )
        }

    suspend fun importUriToGallery(uri: Uri): ExtractedPhotoMetadata? = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "import_${UUID.randomUUID()}.tmp")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            importLocalFileToGallery(tempFile, deleteSource = true)
        } catch (_: Exception) {
            tempFile.delete()
            null
        }
    }

    suspend fun importLocalFileToGallery(sourceFile: File, deleteSource: Boolean = false): ExtractedPhotoMetadata? =
        withContext(Dispatchers.IO) {
            if (!sourceFile.exists() || sourceFile.length() == 0L) return@withContext null
            try {
                val exif = runCatching { ExifInterface(sourceFile.absolutePath) }.getOrNull()
                val orientation = exif?.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                ) ?: ExifInterface.ORIENTATION_NORMAL

                val cameraMake = exif?.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
                val cameraModelRaw = exif?.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
                val cameraModel = listOf(cameraMake, cameraModelRaw)
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(" ")
                    .ifEmpty { "Smartphone Kamera" }

                val apertureRaw = exif?.getAttribute(ExifInterface.TAG_F_NUMBER)
                val aperture = if (!apertureRaw.isNullOrBlank()) "f/$apertureRaw" else "f/1.8"

                val exposureRaw = exif?.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDoubleOrNull()
                val shutterSpeed = when {
                    exposureRaw == null || exposureRaw <= 0.0 -> "1/120s"
                    exposureRaw < 1.0 -> "1/${(1.0 / exposureRaw).roundToInt().coerceAtLeast(1)}s"
                    else -> "${String.format(Locale.US, "%.1f", exposureRaw)}s"
                }

                val iso = exif?.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0)
                    ?.takeIf { it > 0 } ?: 160

                val focalLength = exif?.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                    ?.let { raw ->
                        if (raw.contains("/")) {
                            val parts = raw.split("/")
                            val num = parts.getOrNull(0)?.toDoubleOrNull() ?: 26.0
                            val den = parts.getOrNull(1)?.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
                            "${(num / den).roundToInt()}mm"
                        } else {
                            "${raw}mm"
                        }
                    } ?: "26mm"

                val dateRaw = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif?.getAttribute(ExifInterface.TAG_DATETIME)
                val takenAt = parseExifDate(dateRaw) ?: System.currentTimeMillis()

                val decoded = decodeSampledBitmap(sourceFile.absolutePath, maxDim = 2048)
                    ?: return@withContext null
                val oriented = applyExifOrientation(decoded, orientation)

                val destFile = File(photosDir, "lumina_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
                FileOutputStream(destFile).use { out ->
                    oriented.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }

                val (paletteCsv, category) = extractPalette(oriented)
                val w = oriented.width
                val h = oriented.height
                oriented.recycle()

                if (deleteSource) sourceFile.delete()

                ExtractedPhotoMetadata(
                    filePath = destFile.absolutePath,
                    width = w,
                    height = h,
                    fileSizeBytes = destFile.length(),
                    takenAt = takenAt,
                    cameraModel = cameraModel,
                    lensSpec = "$focalLength $aperture",
                    aperture = aperture,
                    shutterSpeed = shutterSpeed,
                    iso = iso,
                    paletteCsv = paletteCsv,
                    colorCategory = category
                )
            } catch (_: Exception) {
                null
            }
        }

    suspend fun renderAndSaveEditedPhoto(
        sourceFilePath: String,
        adjustments: PhotoAdjustments,
        takenAt: Long,
        overwriteTargetPath: String? = null
    ): ExtractedPhotoMetadata? = withContext(Dispatchers.IO) {
        val source = decodeSampledBitmap(sourceFilePath, maxDim = 2048) ?: return@withContext null

        // 1. Transform (rotate & flip)
        val matrix = Matrix().apply {
            if (adjustments.rotationDegrees != 0) {
                postRotate(adjustments.rotationDegrees.toFloat())
            }
            if (adjustments.flipHorizontal || adjustments.flipVertical) {
                postScale(
                    if (adjustments.flipHorizontal) -1f else 1f,
                    if (adjustments.flipVertical) -1f else 1f
                )
            }
        }
        val transformed = if (!matrix.isIdentity) {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                .also { if (it != source) source.recycle() }
        } else {
            source
        }

        // 2. Crop aspect
        val cropped = applyCropAspect(transformed, adjustments.cropAspect).also {
            if (it != transformed) transformed.recycle()
        }

        // 3. Apply ColorMatrix
        val filtered = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(filtered)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(buildAndroidColorMatrix(adjustments))
        }
        canvas.drawBitmap(cropped, 0f, 0f, paint)
        cropped.recycle()

        // 4. Vignette
        if (adjustments.vignette > 0.01f) {
            val cx = filtered.width / 2f
            val cy = filtered.height / 2f
            val radius = max(filtered.width, filtered.height) * 0.72f
            val alpha = (adjustments.vignette * 190).roundToInt().coerceIn(0, 210)
            val gradient = RadialGradient(
                cx, cy, radius,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(alpha, 8, 8, 12)),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient }
            canvas.drawRect(0f, 0f, filtered.width.toFloat(), filtered.height.toFloat(), vignettePaint)
        }

        // 5. Gallery / Polaroid Frame
        val finalBitmap = applyFrame(filtered, adjustments.frameStyle, takenAt).also {
            if (it != filtered) filtered.recycle()
        }

        val targetFile = if (overwriteTargetPath != null) {
            File(overwriteTargetPath)
        } else {
            File(photosDir, "lumina_edit_${System.currentTimeMillis()}.jpg")
        }

        FileOutputStream(targetFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 93, out)
        }

        val (paletteCsv, category) = extractPalette(finalBitmap)
        val w = finalBitmap.width
        val h = finalBitmap.height
        finalBitmap.recycle()

        ExtractedPhotoMetadata(
            filePath = targetFile.absolutePath,
            width = w,
            height = h,
            fileSizeBytes = targetFile.length(),
            takenAt = takenAt,
            cameraModel = "Lumina Studio",
            lensSpec = adjustments.filter.displayName,
            aperture = "f/2.0",
            shutterSpeed = "1/250s",
            iso = 100,
            paletteCsv = paletteCsv,
            colorCategory = category
        )
    }

    suspend fun renderAndSaveCollage(
        filePaths: List<String>,
        layout: CollageLayout,
        spacingPx: Float,
        cornerRadiusPx: Float,
        backgroundColor: Int
    ): ExtractedPhotoMetadata? = withContext(Dispatchers.IO) {
        if (filePaths.size < layout.requiredCount) return@withContext null

        val outSize = 1600
        val outHeight = when (layout) {
            CollageLayout.TWO_SPLIT -> 1200
            CollageLayout.TWO_STACK -> 1800
            CollageLayout.THREE_EDITORIAL -> 1600
            CollageLayout.FOUR_GRID -> 1600
        }

        val result = Bitmap.createBitmap(outSize, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(backgroundColor)

        val slots = computeCollageSlots(layout, outSize.toFloat(), outHeight.toFloat(), spacingPx)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        slots.forEachIndexed { index, rect ->
            val path = filePaths.getOrNull(index) ?: return@forEachIndexed
            val bmp = decodeSampledBitmap(path, maxDim = 1024) ?: return@forEachIndexed
            canvas.save()
            val clipPath = Path().apply {
                addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            drawBitmapCenterCrop(canvas, bmp, rect, paint)
            canvas.restore()
            bmp.recycle()
        }

        val destFile = File(photosDir, "lumina_collage_${System.currentTimeMillis()}.jpg")
        FileOutputStream(destFile).use { out ->
            result.compress(Bitmap.CompressFormat.JPEG, 93, out)
        }

        val (paletteCsv, category) = extractPalette(result)
        val w = result.width
        val h = result.height
        result.recycle()

        ExtractedPhotoMetadata(
            filePath = destFile.absolutePath,
            width = w,
            height = h,
            fileSizeBytes = destFile.length(),
            takenAt = System.currentTimeMillis(),
            cameraModel = "Lumina Kollage Studio",
            lensSpec = layout.label,
            aperture = "f/2.8",
            shutterSpeed = "1/125s",
            iso = 100,
            paletteCsv = paletteCsv,
            colorCategory = category
        )
    }

    private fun computeCollageSlots(
        layout: CollageLayout,
        w: Float,
        h: Float,
        s: Float
    ): List<RectF> {
        return when (layout) {
            CollageLayout.TWO_SPLIT -> {
                val cellW = (w - 3 * s) / 2f
                listOf(
                    RectF(s, s, s + cellW, h - s),
                    RectF(2 * s + cellW, s, w - s, h - s)
                )
            }
            CollageLayout.TWO_STACK -> {
                val cellH = (h - 3 * s) / 2f
                listOf(
                    RectF(s, s, w - s, s + cellH),
                    RectF(s, 2 * s + cellH, w - s, h - s)
                )
            }
            CollageLayout.THREE_EDITORIAL -> {
                val leftW = (w - 3 * s) * 0.54f
                val rightX = 2 * s + leftW
                val rightH = (h - 3 * s) / 2f
                listOf(
                    RectF(s, s, s + leftW, h - s),
                    RectF(rightX, s, w - s, s + rightH),
                    RectF(rightX, 2 * s + rightH, w - s, h - s)
                )
            }
            CollageLayout.FOUR_GRID -> {
                val cellW = (w - 3 * s) / 2f
                val cellH = (h - 3 * s) / 2f
                listOf(
                    RectF(s, s, s + cellW, s + cellH),
                    RectF(2 * s + cellW, s, w - s, s + cellH),
                    RectF(s, 2 * s + cellH, s + cellW, h - s),
                    RectF(2 * s + cellW, 2 * s + cellH, w - s, h - s)
                )
            }
        }
    }

    private fun drawBitmapCenterCrop(canvas: Canvas, bitmap: Bitmap, dest: RectF, paint: Paint) {
        val scale = max(dest.width() / bitmap.width, dest.height() / bitmap.height)
        val scaledW = bitmap.width * scale
        val scaledH = bitmap.height * scale
        val dx = dest.left + (dest.width() - scaledW) / 2f
        val dy = dest.top + (dest.height() - scaledH) / 2f
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        canvas.drawBitmap(bitmap, matrix, paint)
    }

    private fun applyCropAspect(bitmap: Bitmap, aspect: CropAspect): Bitmap {
        val targetRatio = aspect.ratio ?: return bitmap
        val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        if (kotlin.math.abs(currentRatio - targetRatio) < 0.01f) return bitmap

        val cropW: Int
        val cropH: Int
        if (currentRatio > targetRatio) {
            cropH = bitmap.height
            cropW = (cropH * targetRatio).roundToInt().coerceAtMost(bitmap.width)
        } else {
            cropW = bitmap.width
            cropH = (cropW / targetRatio).roundToInt().coerceAtMost(bitmap.height)
        }
        val x = ((bitmap.width - cropW) / 2).coerceAtLeast(0)
        val y = ((bitmap.height - cropH) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(bitmap, x, y, cropW, cropH)
    }

    private fun applyFrame(bitmap: Bitmap, style: FrameStyle, takenAt: Long): Bitmap {
        if (style == FrameStyle.NONE) return bitmap
        val baseDim = min(bitmap.width, bitmap.height)
        val border = (baseDim * 0.055f).roundToInt().coerceAtLeast(24)
        val bottomExtra = if (style == FrameStyle.POLAROID) (baseDim * 0.14f).roundToInt() else border

        val framedW = bitmap.width + border * 2
        val framedH = bitmap.height + border + bottomExtra
        val out = Bitmap.createBitmap(framedW, framedH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val bgColor = when (style) {
            FrameStyle.GALLERY_WHITE -> Color.parseColor("#FAF8F4")
            FrameStyle.POLAROID -> Color.parseColor("#F5F2EB")
            FrameStyle.DARK_MATTE -> Color.parseColor("#161820")
            FrameStyle.NONE -> Color.TRANSPARENT
        }
        canvas.drawColor(bgColor)
        canvas.drawBitmap(bitmap, border.toFloat(), border.toFloat(), null)

        if (style == FrameStyle.POLAROID) {
            val dateText = SimpleDateFormat("dd. MMM yyyy", Locale.GERMAN).format(Date(takenAt))
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#3B3A36")
                textSize = (bottomExtra * 0.32f).coerceAtLeast(22f)
                textAlign = Paint.Align.CENTER
            }
            val textY = bitmap.height + border + (bottomExtra * 0.62f)
            canvas.drawText(dateText, framedW / 2f, textY, textPaint)
        }
        return out
    }

    private fun extractPalette(bitmap: Bitmap): Pair<String, ColorCategory> {
        val sample = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
        val pixels = IntArray(24 * 24)
        sample.getPixels(pixels, 0, 24, 0, 0, 24, 24)
        if (sample != bitmap) sample.recycle()

        // Bucket colors roughly to find distinct dominant swatches
        val buckets = LinkedHashMap<Int, Int>()
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var satTotal = 0f

        val hsv = FloatArray(3)
        for (px in pixels) {
            val r = Color.red(px)
            val g = Color.green(px)
            val b = Color.blue(px)
            sumR += r
            sumG += g
            sumB += b
            Color.RGBToHSV(r, g, b, hsv)
            satTotal += hsv[1]

            val qr = (r / 42) * 42 + 21
            val qg = (g / 42) * 42 + 21
            val qb = (b / 42) * 42 + 21
            val key = Color.rgb(qr.coerceAtMost(255), qg.coerceAtMost(255), qb.coerceAtMost(255))
            buckets[key] = (buckets[key] ?: 0) + 1
        }

        val topSwatches = buckets.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { String.format("#%06X", 0xFFFFFF and it.key) }

        val avgSat = satTotal / pixels.size
        val avgR = (sumR / pixels.size).toInt()
        val avgG = (sumG / pixels.size).toInt()
        val avgB = (sumB / pixels.size).toInt()
        Color.RGBToHSV(avgR, avgG, avgB, hsv)
        val hue = hsv[0]

        val category = when {
            avgSat < 0.16f -> ColorCategory.MONOCHROME
            hue in 15f..55f || hue >= 335f -> ColorCategory.WARM_AMBER
            hue in 75f..155f -> ColorCategory.EMERALD
            hue in 156f..205f -> ColorCategory.NEON_CYAN
            hue in 206f..270f -> ColorCategory.COOL_BLUE
            else -> ColorCategory.WARM_AMBER
        }

        return topSwatches.joinToString(",") to category
    }

    private fun decodeSampledBitmap(path: String, maxDim: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        var inSample = 1
        while (opts.outWidth / inSample > maxDim || opts.outHeight / inSample > maxDim) {
            inSample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
        return BitmapFactory.decodeFile(path, decodeOpts)
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun parseExifDate(dateString: String?): Long? {
        if (dateString.isNullOrBlank()) return null
        return runCatching {
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(dateString)?.time
        }.getOrNull()
    }

    companion object {
        fun buildAndroidColorMatrix(adjustments: PhotoAdjustments): ColorMatrix {
            val result = ColorMatrix()

            // Preset filter matrix
            val filterMatrix = when (adjustments.filter) {
                PhotoFilter.ORIGINAL -> ColorMatrix()
                PhotoFilter.KODAK_GOLD -> ColorMatrix(
                    floatArrayOf(
                        1.12f, 0.04f, 0f, 0f, 8f,
                        0.02f, 1.05f, 0f, 0f, 4f,
                        0f, 0f, 0.86f, 0f, -6f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                PhotoFilter.POLAROID -> ColorMatrix(
                    floatArrayOf(
                        1.18f, 0f, 0f, 0f, -10f,
                        0f, 1.08f, 0.04f, 0f, 6f,
                        0f, 0.06f, 0.95f, 0f, 12f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                PhotoFilter.MONOCHROM -> ColorMatrix().apply { setSaturation(0f) }
                PhotoFilter.FILM_NOIR -> ColorMatrix().apply { setSaturation(0f) }.also { m ->
                    val c = 1.45f
                    val t = (-0.5f * c + 0.5f) * 255f
                    m.postConcat(
                        ColorMatrix(
                            floatArrayOf(
                                c, 0f, 0f, 0f, t,
                                0f, c, 0f, 0f, t,
                                0f, 0f, c, 0f, t,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                    )
                }
                PhotoFilter.BERNSTEIN -> ColorMatrix(
                    floatArrayOf(
                        1.22f, 0.08f, 0f, 0f, 10f,
                        0f, 1.04f, 0f, 0f, 2f,
                        0f, 0f, 0.78f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                PhotoFilter.NORDISCH -> ColorMatrix(
                    floatArrayOf(
                        0.88f, 0f, 0.05f, 0f, -4f,
                        0f, 1.02f, 0.06f, 0f, 4f,
                        0.04f, 0.06f, 1.18f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                PhotoFilter.NEON -> ColorMatrix(
                    floatArrayOf(
                        1.15f, -0.05f, 0.1f, 0f, 0f,
                        0f, 1.12f, 0.08f, 0f, 0f,
                        0.1f, 0.05f, 1.25f, 0f, 6f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                PhotoFilter.SEPIA -> ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                PhotoFilter.LEBENDIG -> ColorMatrix().apply { setSaturation(1.42f) }
            }
            result.postConcat(filterMatrix)

            // Saturation
            if (adjustments.saturation != 1f) {
                val satMatrix = ColorMatrix().apply { setSaturation(adjustments.saturation) }
                result.postConcat(satMatrix)
            }

            // Contrast & Brightness & Warmth
            val c = adjustments.contrast
            val bOffset = adjustments.brightness * 255f
            val t = (-0.5f * c + 0.5f) * 255f + bOffset
            val rWarm = adjustments.warmth * 36f
            val bWarm = -adjustments.warmth * 36f

            val adjMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t + rWarm,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t + bWarm,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            result.postConcat(adjMatrix)
            return result
        }
    }
}
