package com.boringowl.rpgchat.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.DialogFragment;

import com.boringowl.rpgchat.R;


public class DiceDialog extends DialogFragment {

    public DiceDialog() {
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_dice, null);
    }

}
