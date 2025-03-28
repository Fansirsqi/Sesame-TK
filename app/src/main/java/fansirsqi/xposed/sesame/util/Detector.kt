package fansirsqi.xposed.sesame.util

import android.content.Context
import android.content.pm.PackageManager
import fansirsqi.xposed.sesame.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

object Detector {
    @JvmStatic
    fun getLibPath(context: Context): String? {
    var libSesamePath: String? = null
    try {
        val appInfo = context.packageManager.getApplicationInfo(
            BuildConfig.APPLICATION_ID, 0
        )
        libSesamePath = appInfo.nativeLibraryDir + File.separator + System.mapLibraryName("checker")
        Log.runtime("load libSesame success")
    } catch (e: PackageManager.NameNotFoundException) {
        ToastUtil.showToast(context, "请不要对应用宝隐藏TK模块")
        Log.record("请不要对应用宝隐藏TK模块")
        Log.error("getLibPath", e.message)
    }
    return libSesamePath
}



    fun loadLibrary(libraryName: String): Boolean {
        try {
            System.loadLibrary(libraryName)
            return true
        } catch (e: UnsatisfiedLinkError) {
            Log.error("loadLibrary", e.message)
            return false
        }
    }

    private external fun init(context: Context)

    @JvmStatic
    external fun tips(context: Context?, message: String?)

    external fun foundTargetData(apkPath: String?): String?

    fun initDetector(context: Context) {
        try {
            init(context)
        } catch (e: Exception) {
            Log.error("initDetector", e.message)
        }
    }

    private fun getApkPath(context: Context, packageName: String): String? {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            Log.runtime("appInfo.sourceDir: " + appInfo.sourceDir)
            return appInfo.sourceDir
        } catch (e: PackageManager.NameNotFoundException) {
            Log.runtime("Package not found: $packageName")
            return null
        }
    }

    @JvmStatic
    fun checkForLspatch(context: Context, packageName: String): Boolean {
        try {
            val apkPath = getApkPath(context, packageName)
            if (apkPath == null) {
                Log.runtime("Package not found2: $packageName")
                return true
            }
            //            String jniFoundTargetData = foundTargetData(apkPath);
//            Log.runtime("jniFoundTargetData: " + jniFoundTargetData);
            return paserJson(jFoundTargetData(apkPath))
        } catch (e: Exception) {
            return true
        }
    }

    private fun jFoundTargetData(apkPath: String): String? {
        try {
            ZipFile(File(apkPath)).use { zipFile ->
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val entryName = entry.name
                    if (entryName.startsWith("assets/lspatch/")) {
                        if (entryName.endsWith(".json")) {
                            Log.runtime("found target data: $entryName")
                            val baos = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var len: Int
                            zipFile.getInputStream(entry).use { `is` ->
                                while ((`is`.read(buffer).also { len = it }) != -1) {
                                    baos.write(buffer, 0, len)
                                }
                            }
                            return baos.toString()
                        }
                    }
                }
                Log.runtime("not found target data")
                return null
            }
        } catch (e: IOException) {
            Log.error("checkForLspatch", e.message)
            return null
        }
    }

    private fun paserJson(jsonContent: String?): Boolean {
        try {
            if (jsonContent == null) {
                return false
            }
            Log.runtime("jsonContent: $jsonContent")
            val jsonParser = JsonUtil.getJsonParser(jsonContent)
            val useManager = JsonUtil.parseUseManager(jsonParser)
            Log.runtime("useManager: $useManager")
            return useManager // 找到后立即返回
        } catch (e: Exception) {
            Log.error("paserJson", e.message)
            return false
        }
    }
}