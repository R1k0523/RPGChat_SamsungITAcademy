package com.boringowl.rpgchat.adapters;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.boringowl.rpgchat.fragments.ChatsFragment;
import com.boringowl.rpgchat.fragments.GroupsFragment;
import com.boringowl.rpgchat.fragments.MyGroupsFragment;
import com.boringowl.rpgchat.fragments.RequestsFragment;


public class TabsAccessorAdapter extends FragmentPagerAdapter {

    public TabsAccessorAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int item) {
        switch (item) {
            case 0:
                return new MyGroupsFragment();
            case 1:
                return new ChatsFragment();
            case 2:
                return new GroupsFragment();
            case 3:
                return new RequestsFragment();

            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "My Groups";
            case 1:
                return "Chats";
            case 2:
                return "Groups";
            case 3:
                return "Requests";
            default:
                return null;

        }
    }
}
