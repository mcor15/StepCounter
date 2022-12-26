package com.example.step_counter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/*
Helper class to return the current or next day dates in the format of (dd/MM/yyyy).
 */
public class DateHelper {

    /*
    Helper to get the current date in the from of Day/Month/Year
     */
    public static String getCurrentDate(){
        java.util.Date today = Calendar.getInstance().getTime();
        String dateFormatPattern = "dd/MM/yyyy";
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormatPattern, Locale.UK);
        return formatter.format(today);
    }

    /*
    Helper to get the date yesterday in the from of Day/Month/Year
     */
    public static String getYesterday(){
        java.util.Date today = Calendar.getInstance().getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.DATE,-1);
        java.util.Date yesterday = calendar.getTime();
        String dateFormatPattern = "dd/MM/yyyy";
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormatPattern, Locale.UK);
        return formatter.format(yesterday);
    }


}
