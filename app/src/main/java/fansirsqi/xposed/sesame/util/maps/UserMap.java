package fansirsqi.xposed.sesame.util.maps;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    
    // 优化：使用读写锁提升并发性能
    private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
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
     * 设置当前用户ID
     *
     * @param userId 用户ID
     */
    public static void setCurrentUserId(String userId) {
        rwLock.writeLock().lock();
        try {
            currentUid = (userId == null || userId.isEmpty()) ? null : userId;
        } finally {
            rwLock.writeLock().unlock();
        }
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
    public static void add(UserEntity userEntity) {
        rwLock.writeLock().lock();
        try {
            if (userEntity.getUserId() != null && !userEntity.getUserId().isEmpty()) {
                userMap.put(userEntity.getUserId(), userEntity);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 从映射中移除指定用户
     *
     * @param userId 用户ID
     */
    public static void remove(String userId) {
        rwLock.writeLock().lock();
        try {
            userMap.remove(userId);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 加载用户数据
     * 优化：使用写锁保护I/O操作
     *
     * @param userId 用户ID
     */
    public static void load(String userId) {
        rwLock.writeLock().lock();
        try {
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
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 卸载用户数据
     */
    public static void unload() {
        rwLock.writeLock().lock();
        try {
            userMap.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 保存用户数据到文件
     * 优化：使用读锁，允许并发读取
     *
     * @param userId 用户ID
     * @return 保存结果
     */
    public static boolean save(String userId) {
        rwLock.readLock().lock();
        try {
            return Files.write2File(JsonUtil.formatJson(userMap), Files.getFriendIdMapFile(userId));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 加载当前用户的数据
     * 优化：使用写锁保护I/O操作
     *
     * @param userId 用户ID
     */
    public static void loadSelf(String userId) {
        rwLock.writeLock().lock();
        try {
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
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 保存当前用户数据到文件
     * 优化：使用读锁，允许并发读取
     *
     * @param userEntity 用户实体
     */
    public static void saveSelf(UserEntity userEntity) {
        rwLock.readLock().lock();
        try {
            String body = JsonUtil.formatJson(userEntity);
            Files.write2File(body, Files.getSelfIdFile(userEntity.getUserId()));
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
