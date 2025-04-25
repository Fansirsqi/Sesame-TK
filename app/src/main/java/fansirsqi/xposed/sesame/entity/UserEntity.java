package fansirsqi.xposed.sesame.entity;

import fansirsqi.xposed.sesame.util.StringUtil;
import lombok.Data;
import lombok.Getter;

/**
 * 表示一个用户实体，包含用户的基本信息。
 */
@Getter
public class UserEntity {
    /**
     * 用户 ID
     */
    private final String userId;
    /**
     * 用户的账号
     */
    private final String account;
    /**
     * 用户的好友状态（例如：是否是好友等）
     */
    private final Integer friendStatus;
    /**
     * 用户的真实姓名
     */
    private final String realName;
    /**
     * 用户的昵称
     */
    private final String nickName;
    /**
     * 用户的备注名
     */
    private final String remarkName;
    /**
     * 用于显示的名字，优先使用备注名，若无则使用昵称
     */
    private final String showName;
    /**
     * 用于显示的遮掩名字，真实姓名首字母被遮掩
     */
    private final String maskName;
    /**
     * 用户的全名，格式为：显示名字 | 真实姓名 (账号)
     */
    private final String fullName;

    /**
     * 构造方法，初始化用户基本信息。
     *
     * @param userId       用户 ID
     * @param account      用户账号
     * @param friendStatus 用户好友状态
     * @param realName     用户真实姓名
     * @param nickName     用户昵称
     * @param remarkName   用户备注名
     */
    public UserEntity(String userId, String account, Integer friendStatus, String realName, String nickName, String remarkName) {
        this.userId = userId;
        this.account = account;
        this.friendStatus = friendStatus;
        this.realName = realName;
        this.nickName = nickName;
        this.remarkName = remarkName;
        // 计算显示名称
        String showNameTmp = StringUtil.isEmpty(remarkName) ? nickName : remarkName;
        // 处理遮掩名称，真实姓名的第一个字母会被替换为 *
        String maskNameTmp = (realName != null && realName.length() > 1)
                ? "*" + realName.substring(1)
                : realName;
        // 设置 showName 和 maskName
        this.showName = showNameTmp;
        this.maskName = showNameTmp + "|" + maskNameTmp;
        // 设置 fullName，格式为：显示名称 | 真实姓名 (账号)
        this.fullName = showNameTmp + "|" + realName + "(" + account + ")";
//        Log.runtime("UserEntity " + "created: " + this.fullName);
    }

    /**
     * 用户 DTO 类，用于传输数据的简化版本。
     */
    @Data
    public static class UserDto {
        private String userId;
        private String account;
        private Integer friendStatus;
        private String realName;
        private String nickName;
        private String remarkName;

        /**
         * 将 UserDto 转换为 UserEntity 实体。
         *
         * @return 转换后的 UserEntity 实体
         */
        public UserEntity toEntity() {
            return new UserEntity(userId, account, friendStatus, realName, nickName, remarkName);
        }
    }
}
