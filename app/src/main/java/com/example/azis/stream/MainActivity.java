package com.example.azis.stream;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static FloatingActionButton playPauseButton;
    static TabLayout tabLayout;
    PlayerService mBoundService;
    boolean mServiceBound = false;
    List<Song> songs = new ArrayList<>();
    List<Song> sortedSongs;
    ListView songsListView;
    SongListAdapter adapter;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            PlayerService.MyBinder myBinder = (PlayerService.MyBinder) service;
            mBoundService = myBinder.getService();
            mServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mServiceBound = false;
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isPlaying = intent.getBooleanExtra("isPlaying", false);
            flipPlayPauseButton(isPlaying);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        playPauseButton = (FloatingActionButton) findViewById(R.id.fab);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mServiceBound) {
                    mBoundService.togglePlayer();
                }
            }
        });

        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                Log.i("POSITION", Integer.toString(pos));
                if (pos == 1) {
                    adapter.update(sortedSongs);
                    adapter.notifyDataSetChanged();
                    songsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Song song = sortedSongs.get(position);
                            String songAddress = "https://streamas.herokuapp.com/stream/" + song.getId();
                            startStreamingService(songAddress);
                            markSongPlayed(song.getId());
                            askForLikes(song);
                        }
                    });
                    Log.i("TOP5", Integer.toString(pos));
                }
                else if (pos == 0) {
                    adapter.update(songs);
                    adapter.notifyDataSetChanged();
                    songsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Song song = songs.get(position);
                            String songAddress = "https://streamas.herokuapp.com/stream/" + song.getId();
                            startStreamingService(songAddress);
                            markSongPlayed(song.getId());
                            askForLikes(song);
                        }
                    });
                    Log.i("SONGS", Integer.toString(pos));
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

//        String url = "https://www.android-examples.com/wp-content/uploads/2016/04/Thunder-rumble.mp3";
//        String url = "https://cb3a5688.ngrok.io/stream/4";
//        startStreamingService(url);
        songsListView = (ListView) findViewById(R.id.SongsListView);
        fetchSongsFromWeb();
    }

    private void startStreamingService (String url) {
        Intent i = new Intent(this, PlayerService.class);
        i.putExtra("url", url);
        i.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        startService(i);
        bindService(i, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mServiceBound) {
            unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("changePlayButton"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    public static void flipPlayPauseButton (boolean isPlaying) {
        if (isPlaying) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void fetchSongsFromWeb() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection urlConnection = null;
                InputStream inputStream = null;

                try {
                    URL url = new URL("https://streamas.herokuapp.com/songs");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");

                    int statusCode = urlConnection.getResponseCode();
                    if (statusCode == 200) {
                        inputStream = new BufferedInputStream((urlConnection.getInputStream()));
                        String response = convertInputStreamToStream (inputStream);
                        Log.i("GOT SONGS!", response);
                        parseIntoSongs(response);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
        });
        thread.start();
    }

    private String convertInputStreamToStream (InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String line = "";
        String result = "";

        while ((line = bufferedReader.readLine()) != null) {
            result += line;
        }

        if (inputStream != null) {
            inputStream.close();
        }

        return result;
    };

    private void parseIntoSongs (String data) {
        String[] dataArray = data.split("\\*");
        Log.i("DATA ARRAY", Arrays.toString(dataArray));

        int i = 0;

        for (i = 0; i < dataArray.length; i++) {
            String[] songArray = dataArray[i].split(",");
            Song song = new Song(songArray[0], songArray[1], songArray[2], songArray[3], songArray[4]);
            songs.add(song);
            Log.i("SONG ARRAY", Arrays.toString(songArray));
        }

        for (i = 0; i < songs.size(); i++) {
            Log.i("GOT SONG", songs.get(i).getTitle());
        }

        sortedSongs = new ArrayList<>(songs);

        Collections.sort(sortedSongs, new SongsLikesComparator());

        populateSongsListView();
    }

    private void populateSongsListView () {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter = new SongListAdapter(MainActivity.this, songs);
                songsListView.setAdapter(adapter);
                songsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Song song = songs.get(position);
                        String songAddress = "https://streamas.herokuapp.com/stream/" + song.getId();
                        startStreamingService(songAddress);
                        markSongPlayed(song.getId());
                        askForLikes(song);
                    }
                });
            }
        });
    }

    private void markSongPlayed (final int chosenId) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                HttpURLConnection urlConnection = null;

                try {
                    URL url = new URL("https://streamas.herokuapp.com/playadd?id=" + Integer.toString(chosenId));
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");

                    int statusCode = urlConnection.getResponseCode();
                    if (statusCode == 200) {
                        inputStream = new BufferedInputStream(urlConnection.getInputStream());
                        String response = convertInputStreamToStream(inputStream);
                        Log.i("PLAYED SONG ID", response);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
        });
        thread.start();
    }

    private void askForLikes(final Song song) {
        new AlertDialog.Builder(this)
                .setTitle(song.getTitle())
                .setMessage("Do you like this song?")
                .setPositiveButton("YES!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick (DialogInterface dialog, int which) {
                        likeSong(song.getId());
                    }
                })
                .setNegativeButton("Not really", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dislikeSong(song.getId());
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void likeSong (final int chosenId) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                HttpURLConnection urlConnection = null;

                try {
                    URL url = new URL("https://streamas.herokuapp.com/likeadd?id=" + Integer.toString(chosenId));
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");

                    int statusCode = urlConnection.getResponseCode();
                    if (statusCode == 200) {
                        inputStream = new BufferedInputStream(urlConnection.getInputStream());
                        String response = convertInputStreamToStream(inputStream);
                        Log.i("LIKED SONG ID", response);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
        });
        thread.start();
    }

    private void dislikeSong (final int chosenId) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                HttpURLConnection urlConnection = null;

                try {
                    URL url = new URL("https://streamas.herokuapp.com/dislikeadd?id=" + Integer.toString(chosenId));
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");

                    int statusCode = urlConnection.getResponseCode();
                    if (statusCode == 200) {
                        inputStream = new BufferedInputStream(urlConnection.getInputStream());
                        String response = convertInputStreamToStream(inputStream);
                        Log.i("DISLIKED SONG ID", response);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
        });
        thread.start();
    }
}

