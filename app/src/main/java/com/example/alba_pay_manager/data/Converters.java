package com.example.alba_pay_manager.data;

import androidx.room.TypeConverter;
import android.util.Base64;
import android.util.Log;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * Room 데이터베이스의 타입 변환기
 */
public class Converters {
    private static final String TAG = "Converters";

    @TypeConverter
    public static LocalDateTime fromTimestamp(String value) {
        return value == null ? null : LocalDateTime.parse(value);
    }

    @TypeConverter
    public static String dateTimeToTimestamp(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }

    @TypeConverter
    public static LocalDate fromDateString(String value) {
        return value == null ? null : LocalDate.parse(value);
    }

    @TypeConverter
    public static String dateToString(LocalDate date) {
        return date == null ? null : date.toString();
    }

    @TypeConverter
    public static LocalTime fromTimeString(String value) {
        return value == null ? null : LocalTime.parse(value);
    }

    @TypeConverter
    public static String timeToString(LocalTime time) {
        return time == null ? null : time.toString();
    }

    @TypeConverter
    public static LocalDateTime fromTimestamp(Long value) {
        return value == null ? null : 
            LocalDateTime.ofEpochSecond(value, 0, ZoneOffset.UTC);
    }

    @TypeConverter
    public static Long dateToTimestamp(LocalDateTime date) {
        return date == null ? null : date.toEpochSecond(ZoneOffset.UTC);
    }

    @TypeConverter
    public static byte[] fromBase64(String value) {
        if (value == null) return null;
        try {
            // 문자열이 Base64 형식인지 확인
            if (value.matches("^[A-Za-z0-9+/=]+$")) {
                return Base64.decode(value, Base64.NO_WRAP);
            }
            // 일반 문자열인 경우 UTF-8 바이트로 변환
            return value.getBytes("UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "salt 변환 중 오류 발생", e);
            return value.getBytes();
        }
    }

    @TypeConverter
    public static String toBase64(byte[] value) {
        if (value == null) return null;
        try {
            return Base64.encodeToString(value, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "salt 변환 중 오류 발생", e);
            return new String(value);
        }
    }
} 