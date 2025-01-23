package io.github.nitsuya.donottryaccessibility.data

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

object ExportConfig {
    /**
     * 创建文件选择器以导出配置
     * @param fileName 文件名（默认为 DoNotTryAccessibility.xml）
     * @param createFileLauncher ActivityResultLauncher 用于处理文件创建结果
     */
    fun createFile(
        fileName: String = "DoNotTryAccessibility.xml",
        createFileLauncher: ActivityResultLauncher<Intent>
    ) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/xml"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        /** 启动系统文件选择器*/
        createFileLauncher.launch(intent)
    }

    /**
     * 导出配置到用户选择的URI
     * @param activity 当前Activity实例
     * @param uri 用户选择的保存位置URI
     */
    fun exportConfig(
        activity: Activity,
        uri: Uri
    ): Boolean {
        return try {
            // 确保ConfigData已经初始化
            ConfigData.init(activity)

            // 使用 ConfigData 来获取配置
            val blockApps = ConfigData.blockApps.data

            // 创建XML格式的配置内容
            val xmlContent = StringBuilder().apply {
                appendLine("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>")
                appendLine("<map>")
                appendLine("    <set name=\"_block_apps\">")
                blockApps.forEach { app ->
                    appendLine("        <string>$app</string>")
                }
                appendLine("    </set>")
                appendLine("</map>")
            }.toString()

            // 使用ContentResolver写入文件
            activity.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(xmlContent.toByteArray())
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}