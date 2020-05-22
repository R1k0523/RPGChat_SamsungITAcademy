package com.boringowl.rpgchat.fragments;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.boringowl.rpgchat.adapters.ChatsAdapter;
import com.boringowl.rpgchat.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class ChatsFragment extends Fragment {
    private RecyclerView chatsList;
    private DatabaseReference chatsRef, usersRef;
    private ChatsAdapter adapter;
    private List<String> usersList;
    private EditText searchText;

    public ChatsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View myView = inflater.inflate(R.layout.fragment_chats, container, false);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        chatsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        chatsList = myView.findViewById(R.id.chats_list);
        searchText = myView.findViewById(R.id.search_chats);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        chatsList.setLayoutManager(linearLayoutManager);
        usersList = new ArrayList<>();
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchChats(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        return myView;
    }

    @Override
    public void onStart() {
        super.onStart();
        chatsRef.orderByChild("LastMessage").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (searchText.getText().toString().equals("")) {
                    usersList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        usersList.add(snapshot.getKey());
                    }
                    adapter = new ChatsAdapter(getContext(), usersList, usersRef);
                    chatsList.setAdapter(adapter);
                    chatsList.scrollToPosition(Objects.requireNonNull(chatsList.getAdapter()).getItemCount() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void searchChats(String s) {
        Query query = chatsRef.orderByChild("Name")
                .startAt(s)
                .endAt(s + "\uf8ff");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    usersList.add(snapshot.getKey());
                }
                adapter = new ChatsAdapter(getContext(), usersList, usersRef);
                chatsList.setAdapter(adapter);
                chatsList.scrollToPosition(Objects.requireNonNull(chatsList.getAdapter()).getItemCount() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
