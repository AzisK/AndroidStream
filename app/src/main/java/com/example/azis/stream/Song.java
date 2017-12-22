package com.example.azis.stream;

import java.util.Comparator;

/**
 * Created by Azis on 2017-12-19.
 */

public class Song {
    int Id;
    String title;
    String author;
    int numplays;
    int numlikes;

    public Song (String Id, String title, String author, String numplays, String numlikes) {
        try {
            this.Id = Integer.parseInt(Id);
        } catch (Exception e) {
            this.Id = 0;
        }
        this.title = title;
        this.author = author;
        this.numplays = Integer.parseInt(numplays);
        this.numlikes = Integer.parseInt(numlikes);
    }

    public int getId() {
        return Id;
    }

    public String getTitle() {
        return title;
    }

    public int getNumplays() {
        return numplays;
    }

    public int getNumlikes() {
        return numlikes;
    }


}
