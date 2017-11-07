package MyMethod;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Created by user on 2016/10/16.
 */

public class ViewPagerAdapter extends FragmentPagerAdapter {

    private final String[] tabTitles = {"動態","聊天室"};
    List<Fragment> fragments;

    public ViewPagerAdapter(FragmentManager fm, List<Fragment> f) {
        super(fm);
        fragments = f;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position); //從上方List<Fragment> fragments取得
    }

}