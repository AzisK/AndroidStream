package com.example.azis.stream;

/**
 * Created by Azis on 2017-12-18.
 */

public class Constants {
    public interface ACTION {
        public static String MAIN_ACTION = "com.example.azis.stream.action.main";
        public static String PREVIOUS_ACTION = "com.example.azis.stream.action.previous";
        public static String NEXT_ACTION = "com.example.azis.stream.action.next";
        public static String PLAY_ACTION = "com.example.azis.stream.action.play";
        public static String STARTFOREGROUND_ACTION = "com.example.azis.stream.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "com.example.azis.stream.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }
}
