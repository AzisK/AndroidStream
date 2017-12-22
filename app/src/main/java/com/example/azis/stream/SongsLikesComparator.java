package com.example.azis.stream;

import android.util.Log;

import java.util.Comparator;

/**
 * Created by Azis on 2017-12-22.
 */

public class SongsLikesComparator implements Comparator<Song> {
    public int compare(Song s1, Song s2) {
        int likes1 = s1.getNumlikes();
        int likes2 = s2.getNumlikes();
        Log.i("LIKES", Integer.toString(likes1) + Integer.toString(likes2));
        return likes2 - likes1;
    }
}
