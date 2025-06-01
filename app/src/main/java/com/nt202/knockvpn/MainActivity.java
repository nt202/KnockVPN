package com.nt202.knockvpn;

import android.os.Bundle;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.nt202.knockvpn.vpn.VpnFragment;
import com.nt202.knockvpn.knocking.KnockingPortsFragment;

public class MainActivity extends FragmentActivity {

    private static final int NUM_PAGES = 2;
    private ViewPager2 viewPager;
    private FragmentStateAdapter pagerAdapter;
    private Button buttonPortsKnocking;
    private Button buttonVpnOverSsh;
    private final ViewPager2.OnPageChangeCallback onPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            if (position == 0) {
                selectedPortsKnocking();
                return;
            }
            if (position == 1) {
                selectedVpnOverSsh();
                return;
            }
            throw new RuntimeException(String.format("%s: %s", "XkuOuK", "Unknown position " + position));
        }
    };

    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new KnockingPortsFragment();
            }
            if (position == 1) {
                return new VpnFragment();
            }
            throw new RuntimeException("rBFxJJ");
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

        buttonPortsKnocking = findViewById(R.id.ports_knocking_page);
        buttonVpnOverSsh = findViewById(R.id.vpn_page);

        selectedPortsKnocking();

        buttonPortsKnocking.setOnClickListener(v -> {
            selectedPortsKnocking();
            viewPager.setCurrentItem(0);
        });

        buttonVpnOverSsh.setOnClickListener(v -> {
            selectedVpnOverSsh();
            viewPager.setCurrentItem(1);
        });

        viewPager.registerOnPageChangeCallback(onPageChangeCallback);
    }

    @Override
    protected void onDestroy() {
        if (viewPager != null) {
            viewPager.unregisterOnPageChangeCallback(onPageChangeCallback);
        }
        super.onDestroy();
    }

    private void selectedPortsKnocking() {
        buttonPortsKnocking.setEnabled(false);
        buttonVpnOverSsh.setEnabled(true);
    }

    private void selectedVpnOverSsh() {
        buttonPortsKnocking.setEnabled(true);
        buttonVpnOverSsh.setEnabled(false);
    }
}