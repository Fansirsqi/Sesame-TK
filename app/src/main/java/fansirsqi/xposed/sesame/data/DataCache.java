package fansirsqi.xposed.sesame.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.StringUtil;
import lombok.Data;

/**
 * @author Byseven
 * @date 2025/2/21
 * @apiNote
 */
@Data
public class DataCache {
    private static final String TAG = DataCache.class.getSimpleName();
    public static final DataCache INSTANCE = new DataCache();

    @JsonIgnore
    private boolean init;
    @JsonIgnore
    private static final String fileName = "dataCache.json";

    //光盘行动图片缓存
    private final Set<Map<String, String>> photoGuangPanCacheSet = new HashSet<>();

    public static Boolean checkGuangPanPhoto(Map<String, String> guangPanPhoto) {
        if (guangPanPhoto == null) {
            return false;
        }
        String beforeImageId = guangPanPhoto.get("before");
        String afterImageId = guangPanPhoto.get("after");
        return !StringUtil.isEmpty(beforeImageId)
                && !StringUtil.isEmpty(afterImageId)
                && !Objects.equals(beforeImageId, afterImageId);
    }

    public static void saveGuangPanPhoto(Map<String, String> guangPanPhoto) {
        if (!checkGuangPanPhoto(guangPanPhoto)) {
            return;
        }
        DataCache cache = INSTANCE;
        if (!cache.photoGuangPanCacheSet.contains(guangPanPhoto)) {
            cache.photoGuangPanCacheSet.add(guangPanPhoto);
            save();
        }
    }


    public static int getGuangPanPhotoCount() {
        load(); // 同步最新数据
        return INSTANCE.photoGuangPanCacheSet.size();
    }

    public static Boolean clearGuangPanPhoto() {
        INSTANCE.photoGuangPanCacheSet.clear();
        return save();
    }


    public static Map<String, String> getRandomGuangPanPhoto() {
        load(); // 确保加载最新数据
        List<Map<String, String>> list = new ArrayList<>(INSTANCE.photoGuangPanCacheSet);
        if (list.isEmpty()) {
            return null;
        }
        int pos = RandomUtil.nextInt(0, list.size() - 1);
        Map<String, String> photo = list.get(pos);
        return checkGuangPanPhoto(photo) ? photo : null;
    }


    private static Boolean save() {
        Log.record(TAG, "save DataCache");
        return Files.write2File(JsonUtil.formatJson(INSTANCE), Files.getTargetFileofDir(Files.MAIN_DIR, fileName));
    }

    public static synchronized DataCache load() {
        File targetFile = Files.getTargetFileofDir(Files.MAIN_DIR, fileName);
        try {
            if (targetFile.exists()) {
                String json = Files.readFromFile(targetFile);
                JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue(json);
                String formatted = JsonUtil.formatJson(INSTANCE);
                if (formatted != null && !formatted.equals(json)) {
                    Log.runtime(TAG, "format" + TAG + "config");
                    Files.write2File(formatted, targetFile);
                }
            } else {
                Log.runtime(TAG, "init" + TAG + "config");
                JsonUtil.copyMapper().updateValue(INSTANCE, new DataCache());
                Files.write2File(JsonUtil.formatJson(INSTANCE), targetFile);
            }
        } catch (Exception e) {
            reset(targetFile);
        }
        INSTANCE.setInit(true);
        return INSTANCE;
    }

    public static synchronized void reset(File targetFile) {
        try {
            JsonUtil.copyMapper().updateValue(INSTANCE, new DataCache());
            Files.write2File(JsonUtil.formatJson(INSTANCE), targetFile);
            Log.runtime(TAG, "reset" + TAG + "config success");
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
            Log.error(TAG, "reset" + TAG + "sonfig failed");
        }
    }


}

