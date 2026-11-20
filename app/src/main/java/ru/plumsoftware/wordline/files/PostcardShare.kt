package ru.plumsoftware.wordline.files

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import ru.plumsoftware.wordline.BuildConfig
import java.io.File

fun postcardBytes(context: Context,drawable: Int,title: String): ByteArray {
    val original=BitmapFactory.decodeResource(context.resources,drawable) ?: error("Открытка недоступна")
    val output=Bitmap.createBitmap(original.width,original.height+120,Bitmap.Config.ARGB_8888)
    Canvas(output).apply {
        drawColor(Color.rgb(255,248,232));drawBitmap(original,0f,0f,null)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.rgb(16,34,63);textSize=30f;typeface=Typeface.create(Typeface.SANS_SERIF,Typeface.BOLD)}
        drawText(title,24f,original.height+44f,paint)
        paint.textSize=22f;drawText("Соедини слова — кроссворды",24f,original.height+86f,paint)
    }
    val bytes=java.io.ByteArrayOutputStream().use {require(output.compress(Bitmap.CompressFormat.PNG,100,it));it.toByteArray()}
    original.recycle();output.recycle()
    return bytes
}
fun sharePostcard(context: Context,drawable: Int,title: String) {
    val dir=File(context.cacheDir,"postcards").apply {mkdirs()};val file=File(dir,"connected-words-postcard.png")
    file.writeBytes(postcardBytes(context,drawable,title))
    val uri=FileProvider.getUriForFile(context,BuildConfig.APPLICATION_ID+".files",file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Поделиться открыткой").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
