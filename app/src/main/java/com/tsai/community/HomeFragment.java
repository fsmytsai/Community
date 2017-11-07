package com.tsai.community;


import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import java.util.List;

import MyMethod.ViewPagerAdapter;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    public ViewPager viewPager;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initView(view);
        return view;
    }

    private void initView(View v) {

        List<Fragment> fragments = new ArrayList<Fragment>();
        fragments.add(new PostListFragment());
        fragments.add(new ChatFragment());

        viewPager = (ViewPager) v.findViewById(R.id.viewpager);
        viewPager.setAdapter(new ViewPagerAdapter(getChildFragmentManager(), fragments));
        //使用getActivity()從MainActivity取值
        viewPager.setCurrentItem(getActivity().getIntent().getIntExtra("ViewItem", 0));

        TabLayout tabs = (TabLayout) v.findViewById(R.id.tabs);
        tabs.setTabMode(TabLayout.MODE_FIXED);
        tabs.setTabGravity(TabLayout.GRAVITY_FILL);

        tabs.setupWithViewPager(viewPager);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                final ListView listView = getDataListFragment().lv_post;
                listView.smoothScrollToPosition(0);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (listView.getFirstVisiblePosition() > 0) {
                            listView.smoothScrollToPosition(0);
                            handler.postDelayed(this, 100);
                        }
                    }
                }, 100);
            }
        });
    }

    public PostListFragment getDataListFragment() {
        return (PostListFragment) getChildFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":0");
    }

    public ChatFragment getChatFragment() {
        return (ChatFragment) getChildFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":1");
    }
}
