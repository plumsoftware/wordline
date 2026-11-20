package ru.plumsoftware.wordline.files

import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

const val MAX_SAVE_BYTES=10*1024*1024
private const val MAX_SAFE_INTEGER=9_007_199_254_740_991L
/** RFC 8785 canonical JSON for the save schema's restricted value domain: safe integers only. */
fun canonicalJson(value: Any?): String = when(value) {
    null,JSONObject.NULL -> "null"
    is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(",","{","}") {quote(it)+":"+canonicalJson(value.get(it))}
    is JSONArray -> (0 until value.length()).joinToString(",","[","]") {canonicalJson(value.get(it))}
    is String -> quote(value)
    is Boolean -> value.toString()
    is Byte,is Short,is Int,is Long -> (value as Number).toLong().also {require(it in -MAX_SAFE_INTEGER..MAX_SAFE_INTEGER)}.toString()
    else -> error("Unsupported JSON number or value")
}
private fun quote(value: String): String = buildString {
    append('"');var index=0
    while(index<value.length) {
        val c=value[index++]
        when(c) {
            '"' ->append("\\\"");'\\' ->append("\\\\");'\b' ->append("\\b");'\u000c' ->append("\\f")
            '\n' ->append("\\n");'\r' ->append("\\r");'\t' ->append("\\t")
            else -> {
                if(c<' ') append("\\u"+c.code.toString(16).padStart(4,'0'))
                else if(c.isHighSurrogate()) {require(index<value.length && value[index].isLowSurrogate());append(c);append(value[index++])}
                else {require(!c.isLowSurrogate());append(c)}
            }
        }
    };append('"')
}
fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {"%02x".format(it)}
/** Duplicate keys, excessive nesting, malformed UTF-8 and fractional numbers fail before application. */
fun strictJson(bytes: ByteArray): JSONObject {
    require(bytes.size<=MAX_SAVE_BYTES) {"Файл больше 10 МиБ"}
    val text=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
    return Parser(text).parse()
}
private class Parser(private val source: String) {
    private var position=0;private var entries=0
    fun parse(): JSONObject {val obj=value(0) as? JSONObject ?: error("Ожидается JSON-объект");space();require(position==source.length) {"Лишние данные"};return obj}
    private fun space() {while(position<source.length && source[position] in " \r\n\t") position++}
    private fun take(c: Char): Boolean {space();return if(position<source.length && source[position]==c) {position++;true} else false}
    private fun value(depth: Int): Any {
        require(depth<=20 && ++entries<=1_000_000) {"Слишком сложное сохранение"};space();require(position<source.length)
        return when(source[position]) {
            '{' -> {
                position++;val obj=JSONObject();val keys=mutableSetOf<String>()
                if(!take('}')) {
                    do {space();val key=string();require(keys.add(key)) {"Повтор ключа $key"};require(take(':'));obj.put(key,value(depth+1))} while(take(','))
                    require(take('}'))
                }
                obj
            }
            '[' -> {
                position++;val array=JSONArray()
                if(!take(']')) {do {array.put(value(depth+1))} while(take(','));require(take(']'))}
                array
            }
            '"' ->string()
            't' ->literal("true",true);'f' ->literal("false",false);'n' ->literal("null",JSONObject.NULL)
            else -> {
                val start=position
                if(source[position]=='-') position++
                require(position<source.length && source[position].isDigit())
                if(source[position]=='0') position++ else while(position<source.length && source[position] in '0'..'9') position++
                val token=source.substring(start,position)
                require(token.matches(Regex("-?(0|[1-9][0-9]*)")))
                token.toLong().also {require(it in -MAX_SAFE_INTEGER..MAX_SAFE_INTEGER)}
            }
        }
    }
    private fun literal(token: String, value: Any): Any {require(source.startsWith(token,position));position+=token.length;return value}
    private fun string(): String {
        require(position<source.length && source[position++]=='"')
        val text=StringBuilder()
        while(position<source.length) {
            val c=source[position++]
            if(c=='"') {val result=text.toString();quote(result);return result}
            require(c>=' ')
            if(c!='\\') text.append(c) else {
                require(position<source.length)
                text.append(when(val escaped=source[position++]) {
                    '"','\\','/' ->escaped;'b' ->'\b';'f' ->'\u000c';'n' ->'\n';'r' ->'\r';'t' ->'\t'
                    'u' -> {require(position+4<=source.length);source.substring(position,position+4).toInt(16).toChar().also {position+=4}}
                    else ->error("Неверное экранирование")
                })
            }
            require(text.length<=16_384) {"Слишком длинная строка"}
        };error("Незавершённая строка")
    }
}
