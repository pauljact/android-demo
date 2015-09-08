package com.jact.jactapp;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

public class YouTubeFragment extends Fragment {
    private FragmentActivity my_context_;
    private YouTubePlayer youtube_player_;
    public static final String DEVELOPER_KEY = "AIzaSyBoJypQxKePzlnt_4kVbRbAez64hbULcqI";
    private static final int RECOVERY_DIALOG_REQUEST = 1;

    @Override
    public void onAttach(Activity activity) {

        if (activity instanceof FragmentActivity) {
            my_context_ = (FragmentActivity) activity;
        }

        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.youtube_layout, container, false);

        YouTubePlayerSupportFragment youTubePlayerFragment = YouTubePlayerSupportFragment.newInstance();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.youtube_fragment, youTubePlayerFragment).commit();

        youTubePlayerFragment.initialize(DEVELOPER_KEY, new OnInitializedListener() {
            @Override
            public void onInitializationSuccess(
                	YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
                if (!wasRestored) {
                  youtube_player_ = player;
                  youtube_player_.setFullscreen(true);
                  youtube_player_.loadVideo("2zNSgSzhBfM");
                  youtube_player_.play();
                }
              }
            @Override
            public void onInitializationFailure(Provider arg0, YouTubeInitializationResult arg1) {
                // TODO Auto-generated method stub
            }
        });
        return rootView;
    }
}