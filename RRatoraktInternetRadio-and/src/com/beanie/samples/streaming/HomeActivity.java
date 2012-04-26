
package com.beanie.samples.streaming;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;

public class HomeActivity extends Activity implements OnClickListener {

    private final static String RADIO_STATION_URL = "http://shoutcast2.omroep.nl:8104/";

    private ProgressBar playSeekBar;

    private Button buttonPlay;

    private Button buttonStopPlay;

    private Button buttonRecord;

    private Button buttonStopRecord;

    private MediaPlayer player;

    private InputStream recordingStream;

    private RecorderThread recorderThread;

    private boolean isRecording = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initializeUIElements();

        initializeMediaPlayer();
    }

    private void initializeUIElements() {

        playSeekBar = (ProgressBar) findViewById(R.id.progressBar1);
        playSeekBar.setMax(100);
        playSeekBar.setVisibility(View.INVISIBLE);

        buttonPlay = (Button) findViewById(R.id.buttonPlay);
        buttonPlay.setOnClickListener(this);

        buttonStopPlay = (Button) findViewById(R.id.buttonStopPlay);
        buttonStopPlay.setEnabled(false);
        buttonStopPlay.setOnClickListener(this);

        buttonRecord = (Button) findViewById(R.id.buttonRecord);
        buttonRecord.setOnClickListener(this);

        buttonStopRecord = (Button) findViewById(R.id.buttonStopRecord);
        buttonStopRecord.setOnClickListener(this);
    }

    public void onClick(View v) {
        if (v == buttonPlay) {
            startPlaying();
        } else if (v == buttonStopPlay) {
            stopPlaying();
        } else if (v == buttonRecord) {
            recorderThread = new RecorderThread();
            recorderThread.start();

            buttonRecord.setEnabled(false);
            buttonStopRecord.setEnabled(true);
        } else if (v == buttonStopRecord) {
            stopRecording();
        }
    }

    private void startPlaying() {
        buttonStopPlay.setEnabled(true);
        buttonPlay.setEnabled(false);

        playSeekBar.setVisibility(View.VISIBLE);

        player.prepareAsync();

        player.setOnPreparedListener(new OnPreparedListener() {

            public void onPrepared(MediaPlayer mp) {
                player.start();
                buttonRecord.setEnabled(true);
            }
        });

    }

    private void stopPlaying() {
        if (player.isPlaying()) {
            player.stop();
            player.release();
            initializeMediaPlayer();
        }

        buttonPlay.setEnabled(true);
        buttonStopPlay.setEnabled(false);
        playSeekBar.setVisibility(View.INVISIBLE);
        buttonRecord.setEnabled(false);
        buttonStopRecord.setEnabled(false);
        stopRecording();
    }

    private void initializeMediaPlayer() {
        player = new MediaPlayer();
        try {
            player.setDataSource(RADIO_STATION_URL);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {

            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                playSeekBar.setSecondaryProgress(percent);
                Log.i("Buffering", "" + percent);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player.isPlaying()) {
            player.stop();
        }
    }

    private void startRecording() {

        BufferedOutputStream writer = null;
        try {
            URL url = new URL(RADIO_STATION_URL);
            URLConnection connection = url.openConnection();
            final String FOLDER_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator + "Songs";

            File folder = new File(FOLDER_PATH);
            if (!folder.exists()) {
                folder.mkdir();
            }

            writer = new BufferedOutputStream(new FileOutputStream(new File(FOLDER_PATH
                    + File.separator + "sample.mp3")));
            recordingStream = connection.getInputStream();

            final int BUFFER_SIZE = 100;

            byte[] buffer = new byte[BUFFER_SIZE];

            while (recordingStream.read(buffer, 0, BUFFER_SIZE) != -1 && isRecording) {
                writer.write(buffer, 0, BUFFER_SIZE);
                writer.flush();
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                recordingStream.close();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        buttonStopRecord.setEnabled(false);
        buttonRecord.setEnabled(true);
        try {
            isRecording = false;
            if (recordingStream != null) {
                recordingStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class RecorderThread extends Thread {
        @Override
        public void run() {
            isRecording = true;
            startRecording();
        }

    };
}
