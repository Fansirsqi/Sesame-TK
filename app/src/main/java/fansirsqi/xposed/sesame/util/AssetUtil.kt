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
    private var destDir: String = Files.MAIN_DIR.absolutePath + File.separator + "lib"
    private var destFile: File = File(destDir, "libchecker.so")

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
     * @param soName  so库名称
     * @return 复制是否成功
     */
    fun copySoFileToStorage(context: Context, soName: String): Boolean {
        try {
            Files.ensureDir(File(destDir))
            val appInfo = context.applicationInfo
            val sourceDir = appInfo.nativeLibraryDir + File.separator + soName
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
                    Log.runtime(TAG, "Copied " + soName + " from " + sourceDir + " to " + destFile.absolutePath)
                    setExecutablePermissions(destFile)
                    return true
                }
            }
        } catch (e: Exception) {
            Log.error(TAG, "Failed to copy SO file: " + e.message)
            return false
        }
    }

    //拷贝上面释放的文件到context 私有lib目录
    fun copyDtorageSoFileToPrivateDir(context: Context, soName: String): Boolean {
        try {
            if (!destFile.exists()) {
                Log.error(TAG, "SO file not exists: " + destFile.absolutePath)
                return false
            }
            val targetDir = context.applicationInfo.dataDir + File.separator + "lib"
            Files.ensureDir(File(targetDir))
            val targetFile = File(targetDir, soName)
            if (targetFile.exists() && compareMD5(destFile.absolutePath, targetFile.absolutePath)) {
                Log.runtime(TAG, "SO file already exists: " + targetFile.absolutePath)
                return true
            }
            FileInputStream(destFile).use { fis ->
                FileOutputStream(targetFile).use { fos ->
                    val buffer = ByteArray(4096)
                    var length: Int
                    while ((fis.read(buffer).also { length = it }) > 0) {
                        fos.write(buffer, 0, length)
                    }
                    fos.flush()
                    Log.runtime(TAG, "Copied " + soName + " from " + destFile.absolutePath + " to " + targetFile.absolutePath)
                    setExecutablePermissions(targetFile)
                    return true
                }
            }
        } catch (e: Exception) {
            Log.error(TAG, "Failed to copy SO file of storage: " + e.message)
            return false
        }

    }


    /**
     * 设置so库文件的执行权限
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