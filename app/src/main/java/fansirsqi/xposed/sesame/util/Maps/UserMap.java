package fansirsqi.xposed.sesame.util.maps;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XposedHelpers;
import fansirsqi.xposed.sesame.entity.UserEntity;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import lombok.Getter;
/**
 * 用于管理和操作用户数据的映射关系，
 * 通常在应用程序中用于处理用户信息，
 * 如用户的 ID、昵称、账号、好友列表等。
 * 通过该类可以高效地加载、存储和操作用户信息，
 * 同时提供线程安全的访问机制。
 */
public class UserMap {
    private static final String TAG = UserMap.class.getSimpleName();
    // 存储用户信息的线程安全映射
    private static final Map<String, UserEntity> userMap = new ConcurrentHashMap<>();
    // 只读的用户信息映射
    private static final Map<String, UserEntity> readOnlyUserMap = Collections.unmodifiableMap(userMap);
    /**
     * 当前用户ID
     */
    @Getter
    public static String currentUid = null;
    /**
     * 获取只读的用户信息映射
     *
     * @return 只读的用户映射
     */
    public static Map<String, UserEntity> getUserMap() {
        return readOnlyUserMap;
    }
    /**
     * 获取所有用户ID的集合
     *
     * @return 用户ID集合
     */
    public static Set<String> getUserIdSet() {
        return userMap.keySet();
    }
    /**
     * 获取所有用户实体的集合
     *
     * @return 用户实体集合
     */
    public static Collection<UserEntity> getUserEntityCollection() {
        return userMap.values();
    }
    /**
     * 初始化用户数据
     *
     * @param currentUserId 当前用户ID
     */
    public static synchronized void initUser(String currentUserId) {
        Log.runtime(TAG ,"初始化用户数据: " + currentUserId);
        // 设置当前用户ID
        setCurrentUserId(currentUserId);
        // 在主线程中执行初始化逻辑
        ApplicationHook.getMainHandler().post(() -> {
            ClassLoader loader;
            try {
                // 获取类加载器
                loader = ApplicationHook.getClassLoader();
            } catch (Exception e) {
                Log.runtime(TAG,"Error getting classloader");
                return;
            }
            try {
                // 卸载现有数据
                UserMap.unload();
                String selfId = ApplicationHook.getUserId();
                // 反射加载类
                Class<?> clsUserIndependentCache = loader.loadClass("com.alipay.mobile.socialcommonsdk.bizdata.UserIndependentCache");
                Class<?> clsAliAccountDaoOp = loader.loadClass("com.alipay.mobile.socialcommonsdk.bizdata.contact.data.AliAccountDaoOp");
                Object aliAccountDaoOp = XposedHelpers.callStaticMethod(clsUserIndependentCache, "getCacheObj", clsAliAccountDaoOp);
                // 获取好友列表
                List<?> allFriends = (List<?>) XposedHelpers.callMethod(aliAccountDaoOp, "getAllFriends", new Object[0]);
                if (!allFriends.isEmpty()) {
                    Class<?> friendClass = allFriends.get(0).getClass();
                    // 通过反射获取字段
                    Field userIdField = XposedHelpers.findField(friendClass, "userId");
                    Field accountField = XposedHelpers.findField(friendClass, "account");
                    Field nameField = XposedHelpers.findField(friendClass, "name");
                    Field nickNameField = XposedHelpers.findField(friendClass, "nickName");
                    Field remarkNameField = XposedHelpers.findField(friendClass, "remarkName");
                    Field friendStatusField = XposedHelpers.findField(friendClass, "friendStatus");
                    UserEntity selfEntity = null;
                    // 遍历所有好友数据并添加到映射中
                    for (Object userObject : allFriends) {
                        try {
                            String userId = (String) userIdField.get(userObject);
                            String account = (String) accountField.get(userObject);
                            String name = (String) nameField.get(userObject);
                            String nickName = (String) nickNameField.get(userObject);
                            String remarkName = (String) remarkNameField.get(userObject);
                            Integer friendStatus = (Integer) friendStatusField.get(userObject);
                            // 创建用户实体
                            UserEntity userEntity = new UserEntity(userId, account, friendStatus, name, nickName, remarkName);
                            if (Objects.equals(selfId, userId)) {
                                selfEntity = userEntity;
                            }
                            UserMap.add(userEntity);
                        } catch (Throwable t) {
                            Log.runtime(TAG,"addUserObject err:");
                            Log.printStackTrace(t);
                        }
                    }
                    // 保存当前用户信息
                    UserMap.saveSelf(selfEntity);
                }
                UserMap.save(selfId);
            } catch (Throwable t) {
                Log.runtime(TAG,"checkUnknownId.run err:");
                Log.printStackTrace(t);
            }
        });
    }
    /**
     * 设置当前用户ID
     *
     * @param userId 用户ID
     */
    public static synchronized void setCurrentUserId(String userId) {
        currentUid = (userId == null || userId.isEmpty()) ? null : userId;
    }
    /**
     * 获取当前用户的掩码名称
     *
     * @return 掩码名称
     */
    public static String getCurrentMaskName() {
        return getMaskName(currentUid);
    }
    /**
     * 获取指定用户的掩码名称
     *
     * @param userId 用户ID
     * @return 掩码名称
     */
    public static String getMaskName(String userId) {
        UserEntity userEntity = userMap.get(userId);
        return userEntity == null ? null : userEntity.getMaskName();
    }
    /**
     * 获取指定用户的完整名称
     *
     * @param userId 用户ID
     * @return 完整名称
     */
    public static String getFullName(String userId) {
        UserEntity userEntity = userMap.get(userId);
        return userEntity == null ? null : userEntity.getFullName();
    }
    /**
     * 获取指定用户实体
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    public static UserEntity get(String userId) {
        return userMap.get(userId);
    }
    /**
     * 添加用户到映射
     *
     * @param userEntity 用户实体
     */
    public static synchronized void add(UserEntity userEntity) {
        if (userEntity.getUserId() != null && !userEntity.getUserId().isEmpty()) {
            userMap.put(userEntity.getUserId(), userEntity);
        }
    }
    /**
     * 从映射中移除指定用户
     *
     * @param userId 用户ID
     */
    public static synchronized void remove(String userId) {
        userMap.remove(userId);
    }
    /**
     * 加载用户数据
     *
     * @param userId 用户ID
     */
    public static synchronized void load(String userId) {
        userMap.clear();
        if (userId == null || userId.isEmpty()) {
            Log.runtime(TAG, "Skip loading user map for empty userId");
            return;
        }
        try {
            File friendIdMapFile = Files.getFriendIdMapFile(userId);
            if (friendIdMapFile == null) {
                Log.runtime(TAG, "Friend ID map file is null for userId: " + userId);
                return;
            }
            String body = Files.readFromFile(friendIdMapFile);
            if (!body.isEmpty()) {
                Map<String, UserEntity.UserDto> dtoMap = JsonUtil.parseObject(body, new TypeReference<>() {
                });
                for (UserEntity.UserDto dto : dtoMap.values()) {
                    userMap.put(dto.getUserId(), dto.toEntity());
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }
    /**
     * 卸载用户数据
     */
    public static synchronized void unload() {
        userMap.clear();
    }
    /**
     * 保存用户数据到文件
     *
     * @param userId 用户ID
     * @return 保存结果
     */
    public static synchronized boolean save(String userId) {
        return Files.write2File(JsonUtil.formatJson(userMap), Files.getFriendIdMapFile(userId));
    }
    /**
     * 加载当前用户的数据
     *
     * @param userId 用户ID
     */
    public static synchronized void loadSelf(String userId) {
        userMap.clear();
        try {
            String body = Files.readFromFile(Files.getSelfIdFile(userId));
            if (!body.isEmpty()) {
                UserEntity.UserDto dto = JsonUtil.parseObject(body, new TypeReference<>() {
                });
                userMap.put(dto.getUserId(), dto.toEntity());
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }
    /**
     * 保存当前用户数据到文件
     *
     * @param userEntity 用户实体
     */
    public static synchronized void saveSelf(UserEntity userEntity) {
        String body = JsonUtil.formatJson(userEntity);
        Files.write2File(body, Files.getSelfIdFile(userEntity.getUserId()));
    }
}
