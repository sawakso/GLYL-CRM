package cn.cordys.common.utils;

import org.apache.commons.lang3.Strings;

/**
 * 字段脱敏工具类
 * <p>
 * 提供按字段类型对值进行打码的通用算法，供搜索脱敏(BaseSearchService)和
 * 角色字段级脱敏(FieldMaskService)共用，避免逻辑重复。
 * </p>
 * <ul>
 *   <li>PHONE / SERIAL_NUMBER：保留前 N-6 位，后 6 位替换为 ******</li>
 *   <li>INPUT / DATA_SOURCE：保留首字符，其余替换为 *</li>
 * </ul>
 */
public final class FieldMaskUtils {

    private FieldMaskUtils() {
    }

    /**
     * 按字段类型对值进行脱敏
     *
     * @param fieldType 字段类型(PHONE/INPUT/DATA_SOURCE/SERIAL_NUMBER 等)
     * @param value     原始值
     * @return 脱敏后的值；若类型不在处理范围内则原样返回
     */
    public static Object maskValue(String fieldType, Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String stringValue) || stringValue.isEmpty()) {
            return value;
        }
        if (Strings.CI.equals(fieldType, "PHONE") || Strings.CI.equals(fieldType, "SERIAL_NUMBER")) {
            return maskPhone(stringValue);
        }
        if (Strings.CI.equals(fieldType, "INPUT") || Strings.CI.equals(fieldType, "DATA_SOURCE")) {
            return maskInput(stringValue);
        }
        // 其他类型默认按 INPUT 方式脱敏
        return maskInput(stringValue);
    }

    /**
     * 手机号/序列号脱敏：后 6 位替换为 *
     */
    public static String maskPhone(String value) {
        int length = value.length();
        if (length > 6) {
            return value.substring(0, length - 6) + "******";
        }
        return "******";
    }

    /**
     * 普通输入脱敏：保留首字符，其余替换为 *
     */
    public static String maskInput(String value) {
        int length = value.length();
        if (length > 1) {
            return value.charAt(0) + "*".repeat(length - 1);
        }
        return "*";
    }
}
