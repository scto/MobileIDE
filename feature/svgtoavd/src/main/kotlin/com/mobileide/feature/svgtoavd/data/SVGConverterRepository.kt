package com.mobileide.svgtoavd.data

import android.util.Xml
import com.mobileide.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.StringReader
import java.io.StringWriter
import javax.inject.Inject

interface SvgConverterRepository {
    suspend fun convertSvgToAvd(svgInput: String): Resource<String>
}

class SvgConverterRepositoryImpl @Inject constructor() : SvgConverterRepository {

    override suspend fun convertSvgToAvd(svgInput: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(svgInput))

            val serializer = Xml.newSerializer()
            val writer = StringWriter()
            serializer.setOutput(writer)

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "svg" -> {
                                serializer.startTag(null, "vector")
                                serializer.attribute(null, "xmlns:android", "http://schemas.android.com/apk/res/android")
                                copyAndMapAttributes(parser, serializer, svgToAvdAttributeMap)
                            }
                            "path" -> {
                                serializer.startTag(null, "path")
                                copyAndMapAttributes(parser, serializer, svgToAvdAttributeMap)
                            }
                            "g" -> {
                                serializer.startTag(null, "group")
                                // Gruppen-Attribute wie Transformationen könnten hier behandelt werden
                            }
                            // Hier könnten weitere SVG-Tags wie <rect>, <circle> behandelt werden
                            else -> {
                                // Unbekannte Tags vorerst ignorieren
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                         when (parser.name) {
                            "svg" -> serializer.endTag(null, "vector")
                            "path" -> serializer.endTag(null, "path")
                            "g" -> serializer.endTag(null, "group")
                        }
                    }
                }
                eventType = parser.next()
            }
            serializer.endDocument()
            Resource.Success(writer.toString())
        } catch (e: XmlPullParserException) {
            Resource.Error("Invalid SVG XML: ${e.message}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred during conversion: ${e.message}")
        }
    }

    private fun copyAndMapAttributes(parser: XmlPullParser, serializer: android.util.XmlSerializer, attributeMap: Map<String, String>) {
        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i)
            val attrValue = parser.getAttributeValue(i)
            val mappedName = attributeMap[attrName] ?: continue // Nur gemappte Attribute übernehmen
            serializer.attribute(null, mappedName, attrValue)
        }
    }

    private val svgToAvdAttributeMap = mapOf(
        // SVG -> AVD
        "width" to "android:width",
        "height" to "android:height",
        "viewBox" to "android:viewportWidth", // Vereinfachung, braucht Parsing
        // Behandelt viewBox="0 0 24 24" -> viewportWidth="24", viewportHeight="24"
        "d" to "android:pathData",
        "fill" to "android:fillColor",
        "stroke" to "android:strokeColor",
        "stroke-width" to "android:strokeWidth",
        "stroke-linecap" to "android:strokeLineCap",
        "stroke-linejoin" to "android:strokeLineJoin",
        "fill-rule" to "android:fillType"
    )
}
