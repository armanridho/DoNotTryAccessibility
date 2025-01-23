package io.github.nitsuya.donottryaccessibility.data

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object ImportConfig {
    /**
     * 创建文件选择器以导入配置
     * @param openFileLauncher ActivityResultLauncher 用于处理文件选择结果
     */
    fun openFile(openFileLauncher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/xml", "text/xml"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // 启动系统文件选择器
        openFileLauncher.launch(intent)
    }

    /**
     * 从用户选择的URI导入配置
     * @param activity 当前Activity实例
     * @param uri 用户选择的文件URI
     * @return 导入是否成功
     */
    fun importConfig(
        activity: Activity,
        uri: Uri
    ): Boolean {
        return try {
            // 确保ConfigData已经初始化
            ConfigData.init(activity)
            
            // 先清空现有数据
            val currentApps = ConfigData.blockApps.data.toHashSet()
            currentApps.forEach { app ->
                ConfigData.blockApps.remove(app)
            }
            
            // 创建XML解析器
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            
            // 读取XML文件
            activity.contentResolver.openInputStream(uri)?.use { input ->
                parser.setInput(input, "UTF-8")
                var eventType = parser.eventType
                
                // 解析XML文件
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "string" -> {
                                    parser.next()
                                    if (parser.eventType == XmlPullParser.TEXT) {
                                        // 使用 ConfigData 的方式添加应用
                                        ConfigData.blockApps.add(parser.text)
                                    }
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}