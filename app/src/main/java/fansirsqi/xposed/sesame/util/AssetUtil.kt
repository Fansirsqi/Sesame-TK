package fansirsqi.xposed.sesame.util

import android.annotation.SuppressLint
import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.MessageDigest

/**
 * 用于处理Assets资源文件的工具类
 */
object AssetUtil {
    private val TAG: String = AssetUtil::class.java.simpleName
    const val CHEKCE_SO = "libchecker.so"
    const val DEXKIT_OS = "libdexkit.so"
    private var destDir: String = Files.MAIN_DIR.absolutePath + File.separator + "lib"
    var checkerDestFile: File = File(destDir, CHEKCE_SO)
    var dexkitDestFile: File = File(destDir, DEXKIT_OS)

    private fun compareMD5(file1: String, file2: String): Boolean {
        try {
            val md51 = getMD5(file1)
            val md52 = getMD5(file2)
            if (md51 == null || md52 == null || md51.isEmpty() || md52.isEmpty()) {
                return false
            }
            return md51 == md52
        } catch (e: Exception) {
            Log.error(TAG, "Failed to compare MD5: " + e.message)
            return false
        }
    }


    private fun getMD5(filePath: String): String? {
        try {
            val file = File(filePath)
            if (!file.isFile) {
                return null // 文件无效时返回null
            }
            val digest = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { `in` ->  // 使用try-with-resources
                val buffer = ByteArray(1024)
                var len: Int
                while ((`in`.read(buffer).also { len = it }) != -1) {
                    digest.update(buffer, 0, len)
                }
            }
            val digestBytes = digest.digest()
            return BigInteger(1, digestBytes).toString(16)
        } catch (e: Exception) {
            Log.error(TAG, "Failed to get MD5: " + e.message)
            return null // 异常时返回null
        }
    }


    /**
     * 从应用安装目录复制so库到模块私有目录
     *
     * @param context 上下文
     * @param destFile  目标so库文件
     * @return 复制是否成功
     */
    fun copySoFileToStorage(context: Context, destFile: File): Boolean {
        try {
            Files.ensureDir(File(destDir))
            val appInfo = context.applicationInfo
            val sourceDir = appInfo.nativeLibraryDir + File.separator + destFile.name
//            Log.error(TAG, "Copying SO file from $sourceDir to ${destFile.absolutePath}")
            if (destFile.exists() && compareMD5(sourceDir, destFile.absolutePath)) {
                Log.runtime(TAG, "SO file already exists: " + destFile.absolutePath)
                return true
            }
            FileInputStream(sourceDir).use { fis ->
                FileOutputStream(destFile).use { fos ->
                    val buffer = ByteArray(4096)
                    var length: Int
                    while ((fis.read(buffer).also { length = it }) > 0) {
                        fos.write(buffer, 0, length)
                    }
                    fos.flush()
                    Log.runtime(
                        TAG,
                        "Copied ${destFile.name} from $sourceDir ${checkerDestFile.absolutePath}"
                    )
                    setExecutablePermissions(destFile)
                    return true
                }
            }
        } catch (e: Exception) {
            Log.error(TAG, "Failed to copy SO file: " + e.message)
            return false
        }
    }

    /**
     * 从模块私有目录复制so库到应用安装目录
     * @param context 上下文
     * @param sourceFile  源so库文件
     * @return 复制是否成功
     */
    fun copyStorageSoFileToPrivateDir(context: Context, sourceFile: File): File? {
        try {
            if (!sourceFile.exists()) {
                Log.error(TAG, "SO file not exists: " + sourceFile.absolutePath)
                return null
            }
            val targetDir = context.getDir("sesame_libs", Context.MODE_PRIVATE)
            val targetFile = File(targetDir, sourceFile.name)
            if (targetFile.exists() && compareMD5(
                    sourceFile.absolutePath,
                    targetFile.absolutePath
                )
            ) {
                Log.runtime(TAG, "SO file already exists: " + targetFile.absolutePath)
                return targetFile
            }
            FileInputStream(sourceFile).use { fis ->
                FileOutputStream(targetFile).use { fos ->
                    val buffer = ByteArray(4096)
                    var length: Int
                    while ((fis.read(buffer).also { length = it }) > 0) {
                        fos.write(buffer, 0, length)
                    }
                    fos.flush()
                    Log.runtime(
                        TAG,
                        "Copied ${sourceFile.name} from ${sourceFile.absolutePath} to ${targetFile.absolutePath}"
                    )
                    setExecutablePermissions(targetFile)
                    return targetFile
                }
            }
        } catch (e: Exception) {
            Log.error(TAG, "Failed to copy ${sourceFile.name} of storage: ${e.message}")
            return null
        }
    }


    /**
     * 设置目标文件的执行权限
     *
     * @param file so库文件
     */
    private fun setExecutablePermissions(file: File) {
        try {
            if (file.exists()) {
                val execSuccess = file.setExecutable(true, false)
                @SuppressLint("SetWorldReadable") val readSuccess = file.setReadable(true, false)
                if (!execSuccess) {
                    Log.error(TAG, "Failed to set executable permission for " + file.absolutePath)
                }
                if (!readSuccess) {
                    Log.error(TAG, "Failed to set readable permission for " + file.absolutePath)
                }
            }
        } catch (e: Exception) {
            Log.error(TAG, "Failed to set file permissions: " + e.message)
        }
    }
}
