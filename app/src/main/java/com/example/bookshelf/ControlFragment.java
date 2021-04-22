package com.example.bookshelf;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

public class ControlFragment extends Fragment {

    private ControlInterface parentActivity;

    private TextView nowPlayingTextView;
    private SeekBar seekBar;

    public ControlFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof ControlInterface)
            parentActivity = (ControlInterface) context;
        else
            throw new RuntimeException("Please implement ControlFragment.ControlInterface");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View l = inflater.inflate(R.layout.fragment_control, container, false);

        nowPlayingTextView = l.findViewById(R.id.nowPlayingTextView);
        seekBar = l.findViewById(R.id.seekBar);

        l.findViewById(R.id.playButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parentActivity.play();
            }
        });
        l.findViewById(R.id.pauseButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parentActivity.pause();
            }
        });
        l.findViewById(R.id.stopButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parentActivity.stop();
            }
        });

        // If the user is dragging the seekbar, update the book position
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b)
                    parentActivity.changePosition(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        return l;
    }

    public void setNowPlaying(String title) {
        nowPlayingTextView.setText(title);
    }

    public void updateProgress(int progress) {
        seekBar.setProgress(progress);
    }

    interface ControlInterface {
        void play();
        void pause();
        void stop();
        void changePosition (int progress);
    }
}