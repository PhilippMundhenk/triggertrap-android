package com.triggertrap.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class WifiPagerAdapter extends FragmentPagerAdapter {

    private String[] mTitles;

    public WifiPagerAdapter(FragmentManager fm, String[] titles) {
        super(fm);
        mTitles = titles;

    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }

    @Override
    public Fragment getItem(int position) {
        return null;
    }

    @Override
    public int getCount() {
        return mTitles.length;
    }

}
