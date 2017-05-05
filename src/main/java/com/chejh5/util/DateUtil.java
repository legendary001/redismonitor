package com.chejh5.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by chenjh5 on 2017/5/4.
 */
public class DateUtil {
    public static long transferStrToDateTime(String dateStr) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.parse(dateStr).getTime();
    }

    public static long getTodayDateTime() throws Exception {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return transferStrToDateTime(dateFormat.format(date));

    }

    public static String getTodayDateStr() {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(date);

    }
}
