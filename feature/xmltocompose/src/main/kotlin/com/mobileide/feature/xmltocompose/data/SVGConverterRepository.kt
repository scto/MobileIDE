package com.mobileide.xmltocompose.data

import android.util.Xml
import com.mobileide.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import javax.inject.Inject

interface XmlConverterRepository {
    suspend fun convertXmlToCompose(xmlInput: String): Resource<String>
}

class XmlConverterRepositoryImpl @Inject constructor() : XmlConverterRepository {

    override suspend fun convertXmlToCompose(xmlInput: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(StringReader(xmlInput.trim()))
            }

            val composeCode = StringBuilder()
            composeCode.append("@Composable\nfun ConvertedLayout() {\n")

            // Zum ersten Tag springen
            parser.nextTag()
            parseNode(parser, composeCode, 1)

            composeCode.append("}")

            Resource.Success(composeCode.toString())
        } catch (e: Exception) {
            Resource.Error("XML parsing failed: ${e.message}")
        }
    }

    private fun parseNode(parser: XmlPullParser, builder: StringBuilder, indentLevel: Int) {
        val indent = "    ".repeat(indentLevel)
        val tagName = parser.name
        val attributes = getAttributes(parser)

        val composableName = mapXmlTagToComposable(tagName)
        val modifier = buildModifier(attributes)
        val text = attributes["android:text"] ?: ""

        builder.append("$indent$composableName(")

        val params = mutableListOf<String>()
        if (modifier.isNotBlank()) params.add("modifier = $modifier")

        when (composableName) {
            "Text" -> if (text.isNotBlank()) params.add("text = \"$text\"")
            "Button" -> params.add("onClick = {}")
            // Weitere Parameter für andere Composables
        }
        builder.append(params.joinToString(", "))

        if (parser.isEmptyElementTag) {
            builder.append(")\n")
            return
        }

        if (isContainer(tagName)) {
            builder.append(") {\n")
            if (composableName == "Button" && text.isNotBlank()) {
                 builder.append("${indent}    Text(text = \"$text\")\n")
            }
            // Rekursiv durch Kind-Elemente iterieren
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    parseNode(parser, builder, indentLevel + 1)
                }
            }
            builder.append("$indent}\n")
        } else {
             builder.append(")\n")
             // Überspringe bis zum End-Tag
             while (parser.next() != XmlPullParser.END_TAG);
        }
    }

    private fun getAttributes(parser: XmlPullParser): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in 0 until parser.attributeCount) {
            map[parser.getAttributeName(i)] = parser.getAttributeValue(i)
        }
        return map
    }

    private fun mapXmlTagToComposable(tag: String): String = when (tag) {
        "TextView" -> "Text"
        "Button" -> "Button"
        "EditText" -> "OutlinedTextField"
        "ImageView" -> "Image"
        "LinearLayout" -> "Column" // Vereinfachung, müsste `orientation` prüfen
        "RelativeLayout", "FrameLayout", "androidx.constraintlayout.widget.ConstraintLayout" -> "Box"
        else -> "Box" // Fallback
    }
    
     private fun isContainer(tag: String): Boolean = when(tag) {
        "LinearLayout", "RelativeLayout", "FrameLayout", "androidx.constraintlayout.widget.ConstraintLayout", "Button" -> true
        else -> false
    }

    private fun buildModifier(attrs: Map<String, String>): String {
        val modifierChain = mutableListOf<String>()
        val width = attrs["android:layout_width"]
        val height = attrs["android:layout_height"]
        val padding = attrs["android:padding"]

        if (width == "match_parent") modifierChain.add("fillMaxWidth()")
        if (height == "match_parent") modifierChain.add("fillMaxHeight()")
        if (width == "wrap_content") modifierChain.add("wrapContentWidth()")
        if (height == "wrap_content") modifierChain.add("wrapContentHeight()")
        
        padding?.let {
            val paddingValue = it.removeSuffix("dp").toIntOrNull()
            if(paddingValue != null) modifierChain.add("padding(${paddingValue}.dp)")
        }

        return if (modifierChain.isNotEmpty()) "Modifier.${modifierChain.joinToString(".")}" else ""
    }
}
