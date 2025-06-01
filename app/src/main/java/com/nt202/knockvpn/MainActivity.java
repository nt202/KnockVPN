package com.nt202.knockvpn;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends FragmentActivity {
    private static final String PREFS_NAME = "PortKnockerPrefs";

    private static final int NUM_PAGES = 2;
    private ViewPager2 viewPager;
    private FragmentStateAdapter pagerAdapter;


    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new KnockingPortsFragment();
            }
            return new VpnFragment();
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewPager = findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        findViewById(R.id.ports_knocking_page).setOnClickListener(v -> {
            viewPager.setCurrentItem(0);
        });

        findViewById(R.id.vpn_page).setOnClickListener(v -> {
            viewPager.setCurrentItem(1);
        });

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                viewPager.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        viewPager.setCurrentItem(1);
//                    }
//                });
//            }
//        }).start();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        if (false) {
//            Intent intent = new Intent(this, VpnFragment.class);
//            startActivity(intent);
//            return;
//        }

//        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
//        listView = (ListView) findViewById(R.id.listView);
//        Button btnAdd = (Button) findViewById(R.id.btnAdd);
//
//        loadSavedSequences();
//
//        btnAdd.setOnClickListener(v -> showAddSequenceDialog());
//
//        String host = "";
//
//        final ArrayList<PortWithProtocol> portsWithProtocols = new ArrayList<>();
//
//        PortKnocker.knock(host, portsWithProtocols);
    }
}