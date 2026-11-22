package ru.plumsoftware.wordline.ui.game

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

private val welcomeInk=AndroidColor.rgb(7,23,56)

private fun welcomeTypeface(weight: Int): Typeface = if(Build.VERSION.SDK_INT>=28)
    Typeface.create(Typeface.create("sans-serif",Typeface.NORMAL),weight,false)
else Typeface.create(if(weight>=900) "sans-serif-black" else "sans-serif",Typeface.BOLD)

/** Shared S01/S07 brand artwork, in the 273 × 139 coordinate space of S07. */
@Composable
internal fun JourneyWordmark(modifier: Modifier) {
    val heavy=remember {welcomeTypeface(900)}
    val bold=remember {welcomeTypeface(700)}
    Canvas(modifier.semantics {contentDescription="Слова. Путешествие"}) {
        val c=drawContext.canvas.nativeCanvas
        val artScale=size.width/273f
        c.save();c.translate(0f,(size.height-139f*artScale)/2f);c.scale(artScale,artScale)
        val p=Paint(Paint.ANTI_ALIAS_FLAG)
        val mountain=AndroidPath().apply {
            moveTo(79f,43f);lineTo(110f,15f);lineTo(120f,20f);lineTo(138f,1f)
            lineTo(166f,25f);lineTo(175f,23f);lineTo(199f,43f);close()
        }
        p.color=AndroidColor.rgb(0,75,75);p.setShadowLayer(2f,0f,1f,0x5500353B)
        c.drawPath(mountain,p);p.clearShadowLayer()
        p.color=AndroidColor.rgb(255,255,247)
        c.drawPath(AndroidPath().apply {
            moveTo(79f,43f);lineTo(110f,15f);lineTo(120f,20f);lineTo(138f,1f);lineTo(166f,25f)
            lineTo(175f,23f);lineTo(199f,43f);lineTo(174f,32f);lineTo(163f,35f)
            lineTo(145f,16f);lineTo(140f,28f);lineTo(134f,20f);lineTo(122f,34f)
            lineTo(113f,28f);lineTo(105f,34f);lineTo(109f,25f);close()
        },p)
        p.color=AndroidColor.rgb(142,205,212)
        c.drawPath(AndroidPath().apply {moveTo(138f,1f);lineTo(145f,16f);lineTo(140f,28f);lineTo(134f,20f);lineTo(138f,12f);lineTo(131f,17f);close()},p)

        val ribbon=AndroidPath().apply {
            moveTo(36f,91f);cubicTo(88f,80f,188f,80f,237f,91f)
            quadTo(247f,93f,244f,107f);lineTo(239f,126f);quadTo(237f,137f,224f,134f)
            cubicTo(171f,125f,99f,125f,47f,134f);quadTo(34f,137f,32f,125f)
            lineTo(29f,107f);quadTo(26f,95f,36f,91f);close()
        }
        p.shader=LinearGradient(0f,84f,0f,138f,intArrayOf(0xFF087C69.toInt(),0xFF005546.toInt(),0xFF004D40.toInt()),null,Shader.TileMode.CLAMP)
        p.setShadowLayer(2f,0f,2f,0x660B3532);c.drawPath(ribbon,p);p.clearShadowLayer();p.shader=null
        p.style=Paint.Style.STROKE;p.strokeWidth=.8f;p.color=0xFF126D59.toInt();c.drawPath(ribbon,p);p.style=Paint.Style.FILL

        drawWelcomeLeaf(c,32f,114f,-140f,33f,10f)
        drawWelcomeLeaf(c,32f,114f,-188f,30f,9f)
        drawWelcomeLeaf(c,241f,114f,-40f,33f,10f)
        drawWelcomeLeaf(c,241f,114f,8f,30f,9f)

        val baseline=AndroidPath().apply {moveTo(46f,123f);quadTo(136.5f,101f,227f,123f)}
        p.typeface=bold;p.textSize=25.5f;p.color=AndroidColor.WHITE
        p.setShadowLayer(1.2f,0f,1.1f,0xAA003A32.toInt())
        val word="путешествие"
        c.drawTextOnPath(word,baseline,(PathMeasure(baseline,false).length-p.measureText(word))/2f,0f,p)
        p.clearShadowLayer()

        val ys=floatArrayOf(43f,37f,34f,37f,43f)
        val angles=floatArrayOf(-10f,-5f,0f,5f,10f)
        "СЛОВА".forEachIndexed {index,letter ->
            val x=21f+index*46f;val y=ys[index]
            c.save();c.rotate(angles[index],x+24f,y+26f)
            val tile=RectF(x,y,x+48f,y+53f)
            p.shader=LinearGradient(0f,y,0f,y+53f,intArrayOf(0xFFFFFDF0.toInt(),0xFFFFF0CF.toInt()),null,Shader.TileMode.CLAMP)
            p.setShadowLayer(1.7f,0f,2f,0x664C4B35);c.drawRoundRect(tile,10f,10f,p)
            p.clearShadowLayer();p.shader=null;p.style=Paint.Style.STROKE;p.strokeWidth=.8f;p.color=0xFFFFFFE9.toInt()
            c.drawRoundRect(tile,10f,10f,p);p.style=Paint.Style.FILL
            p.typeface=heavy;p.textSize=37f;p.color=welcomeInk;p.textAlign=Paint.Align.CENTER
            val metrics=p.fontMetrics
            c.drawText(letter.toString(),x+24f,y+26f-(metrics.ascent+metrics.descent)/2f,p)
            p.textAlign=Paint.Align.LEFT;c.restore()
        }
        c.restore()
    }
}

private fun drawWelcomeLeaf(c: AndroidCanvas,x: Float,y: Float,angle: Float,length: Float,width: Float) {
    c.save();c.translate(x,y);c.rotate(angle)
    val p=Paint(Paint.ANTI_ALIAS_FLAG)
    val leaf=AndroidPath().apply {
        moveTo(0f,0f);cubicTo(length*.15f,-width,length*.65f,-width,length,0f)
        cubicTo(length*.65f,width,length*.15f,width,0f,0f);close()
    }
    p.shader=LinearGradient(0f,width,length,-width,intArrayOf(0xFF257B12.toInt(),0xFF70AF19.toInt(),0xFFCAEA4C.toInt()),null,Shader.TileMode.CLAMP)
    c.drawPath(leaf,p);p.shader=null;p.color=0xFFCEE45C.toInt();p.strokeWidth=.7f
    c.drawLine(0f,0f,length*.91f,0f,p)
    c.restore()
}

/** Location names share the curved green satin ribbon of the journey wordmark. */
@Composable
internal fun LocationRibbon(title: String,modifier: Modifier) {
    val face=remember {welcomeTypeface(900)}
    Canvas(modifier.semantics {contentDescription=title}) {
        val c=drawContext.canvas.nativeCanvas
        val scale=size.width/327f
        c.save();c.translate(0f,(size.height-82f*scale)/2f);c.scale(scale,scale)
        val p=Paint(Paint.ANTI_ALIAS_FLAG)
        val ribbon=AndroidPath().apply {
            moveTo(34f,18f);quadTo(163f,-1f,293f,18f);quadTo(305f,20f,301f,36f)
            lineTo(295f,65f);quadTo(293f,79f,275f,74f);quadTo(163f,59f,52f,74f)
            quadTo(34f,79f,32f,65f);lineTo(26f,36f);quadTo(22f,21f,34f,18f);close()
        }
        p.shader=LinearGradient(0f,4f,0f,78f,intArrayOf(0xFF087C76.toInt(),0xFF00635B.toInt(),0xFF004B42.toInt()),null,Shader.TileMode.CLAMP)
        p.setShadowLayer(2f,0f,2f,0x660A3937);c.drawPath(ribbon,p);p.clearShadowLayer();p.shader=null
        p.style=Paint.Style.STROKE;p.color=0xFF126F60.toInt();p.strokeWidth=1f;c.drawPath(ribbon,p);p.style=Paint.Style.FILL
        drawWelcomeLeaf(c,34f,44f,-147f,31f,10f);drawWelcomeLeaf(c,34f,44f,174f,29f,9f)
        drawWelcomeLeaf(c,293f,44f,-33f,31f,10f);drawWelcomeLeaf(c,293f,44f,6f,29f,9f)
        p.typeface=face;p.textSize=30f;p.color=AndroidColor.WHITE
        while(p.measureText(title)>228f && p.textSize>15f) p.textSize-=.5f
        p.setShadowLayer(1.3f,0f,1.3f,0x99002D28.toInt())
        val line=AndroidPath().apply {moveTo(46f,57f);quadTo(163.5f,36f,281f,57f)}
        c.drawTextOnPath(title,line,(PathMeasure(line,false).length-p.measureText(title))/2f,0f,p)
        c.restore()
    }
}

/** Keep line baselines, font weight and the white halo independent of Text auto-fit. */
@Composable
internal fun WelcomeSlogan(modifier: Modifier) {
    val face=remember {welcomeTypeface(900)}
    Canvas(modifier.semantics {contentDescription="Слова открывают мир"}) {
        val c=drawContext.canvas.nativeCanvas
        val artScale=size.width/219f
        c.save();c.translate(0f,(size.height-93f*artScale)/2f);c.scale(artScale,artScale)
        val glow=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader=RadialGradient(109.5f,46f,110f,intArrayOf(0xCFFFFFFF.toInt(),0x70FFFFFF,0x00FFFFFF),floatArrayOf(0f,.55f,1f),Shader.TileMode.CLAMP)
        }
        c.drawOval(-20f,-11f,239f,104f,glow)
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {typeface=face;textSize=31f;textAlign=Paint.Align.CENTER;isFakeBoldText=true}
        listOf("Слова","открывают","мир").forEachIndexed {index,line ->
            val baseline=27f+index*31f
            p.style=Paint.Style.STROKE;p.strokeWidth=3f;p.color=AndroidColor.WHITE;p.setShadowLayer(9f,0f,0f,AndroidColor.WHITE)
            c.drawText(line,109.5f,baseline,p)
            p.clearShadowLayer();p.style=Paint.Style.FILL;p.color=welcomeInk
            c.drawText(line,109.5f,baseline,p)
        }
        c.restore()
    }
}

@Composable
internal fun WelcomeLetter(letter: Char,modifier: Modifier) {
    val face=remember {welcomeTypeface(900)}
    Canvas(modifier.semantics {contentDescription=letter.toString()}) {
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface=face;textSize=size.minDimension*.75f;color=welcomeInk;textAlign=Paint.Align.CENTER;isFakeBoldText=true
        }
        val metrics=p.fontMetrics
        drawContext.canvas.nativeCanvas.drawText(letter.toString(),size.width/2f,size.height/2f-(metrics.ascent+metrics.descent)/2f,p)
    }
}
