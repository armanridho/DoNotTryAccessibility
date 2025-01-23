@file:Suppress("SetTextI18n")

package io.github.nitsuya.donottryaccessibility.ui.activity

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.highcapable.yukihookapi.YukiHookAPI
import io.github.nitsuya.donottryaccessibility.BuildConfig
import io.github.nitsuya.donottryaccessibility.R
import io.github.nitsuya.donottryaccessibility.databinding.ActivityMainBinding
import io.github.nitsuya.donottryaccessibility.ui.activity.base.BaseActivity
import io.github.nitsuya.donottryaccessibility.utils.factory.hideOrShowLauncherIcon
import io.github.nitsuya.donottryaccessibility.utils.factory.isLauncherIconShowing
import io.github.nitsuya.donottryaccessibility.utils.factory.locale
import io.github.nitsuya.donottryaccessibility.utils.factory.navigate
import io.github.nitsuya.donottryaccessibility.utils.factory.openBrowser
import io.github.nitsuya.donottryaccessibility.utils.factory.toast
import io.github.nitsuya.donottryaccessibility.utils.tool.FrameworkTool
import io.github.nitsuya.donottryaccessibility.data.*

class MainActivity : BaseActivity<ActivityMainBinding>() {

    companion object {
        private val systemVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) ${Build.DISPLAY}"
        var isModuleValid = false
    }

    private val createFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val success = ExportConfig.exportConfig(this, uri)
                runOnUiThread {
                    if (success) {
                        toast(getString(R.string.export_success))
                        /**
                         * 不清楚能不能行，主要是在某些情况下不刷新ui就再次导出会失败，不清楚是哪里卡住了
                         * 改成使用路径选择之后解决了这个问题，但是有时候会出现几秒钟的卡顿，疑似线程刷新导致的bug
                         */
                        recreate()
                    } else {
                        toast(getString(R.string.export_failed))
                    }
                }
            }
        }
    }

    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val success = ImportConfig.importConfig(this, uri)
                runOnUiThread {
                    if (success) {
                        toast(getString(R.string.import_success))
                        /**
                         * 不清楚能不能行，主要是在某些情况下不刷新ui就再次导出会失败，不清楚是哪里卡住了
                         * 改成使用文件选择之后解决了这个问题，但是有时候会出现几秒钟的卡顿，疑似线程刷新导致的bug
                         */
                        recreate()
                    } else {
                        toast(getString(R.string.import_failed))
                    }
                }
            }
        }
    }

    override fun onCreate() {
        binding.mainTextVersion.text = locale.moduleVersion(BuildConfig.VERSION_NAME)
        binding.mainTextSystemVersion.text = locale.systemVersion(systemVersion)
        binding.mgrAppsConfigsButton.setOnClickListener {
            whenActivated {
                navigate<AppsConfigActivity>()
            }
        }
        binding.ExportConfigsButton.setOnClickListener {
            ExportConfig.createFile(createFileLauncher = createFileLauncher)
        }
        binding.ImportConfigsButton.setOnClickListener {
            ImportConfig.openFile(openFileLauncher)
        }


        binding.hideIconInLauncherSwitch.isChecked = isLauncherIconShowing.not()
        binding.hideIconInLauncherSwitch.setOnCheckedChangeListener { button, isChecked ->
            if (button.isPressed) hideOrShowLauncherIcon(isChecked)
        }

        binding.titleGithubIcon.setOnClickListener { openBrowser(url = "https://github.com/Nitsuya/DoNotTryAccessibility") }
    }

    private fun refreshModuleStatus() {
        binding.mainLinStatus.setBackgroundResource(
            when {
                YukiHookAPI.Status.isXposedModuleActive && isModuleValid.not() -> R.drawable.bg_yellow_round
                YukiHookAPI.Status.isXposedModuleActive -> R.drawable.bg_green_round
                else -> R.drawable.bg_dark_round
            }
        )
        binding.mainImgStatus.setImageResource(
            when {
                YukiHookAPI.Status.isXposedModuleActive -> R.mipmap.ic_success
                else -> R.mipmap.ic_warn
            }
        )
        binding.mainTextStatus.text = when {
            YukiHookAPI.Status.isXposedModuleActive && isModuleValid.not() -> locale.moduleNotFullyActivated
            YukiHookAPI.Status.isXposedModuleActive -> locale.moduleIsActivated
            else -> locale.moduleNotActivated
        }
        binding.mainTextApiWay.isVisible = YukiHookAPI.Status.isXposedModuleActive
        binding.mainTextApiWay.text = "Activated by ${YukiHookAPI.Status.Executor.name} API ${YukiHookAPI.Status.Executor.apiLevel}"

    }

    private inline fun whenActivated(callback: () -> Unit) {
        if (YukiHookAPI.Status.isXposedModuleActive.not())  toast(locale.moduleNotActivated)
        else if(isModuleValid.not()) toast(locale.moduleNotFullyActivated)
        else callback()
    }

    override fun onResume() {
        super.onResume()
        /** 刷新模块状态 */
        refreshModuleStatus()
        /** 检查模块激活状态 */
        FrameworkTool.checkingActivated(context = this) { isValid ->
            isModuleValid = isValid
            refreshModuleStatus()
        }

        binding.serviceCheckResult.text = getSystemService(AccessibilityManager::class.java).let { am ->
            listOf("isEnabled: " + am.isEnabled) + am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).map { it.id }.sorted()
        }.joinToString("\r\n")

        binding.settingsSecureCheckResult.text = (
            listOf("isEnabled: " + try { (Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) != 0) } catch (e : Exception) { e.message })
            + try { Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).split(':').sorted() } catch (e : Exception) { listOf(e.message ?: "ERROR") }
        ).joinToString("\r\n")
    }
}

