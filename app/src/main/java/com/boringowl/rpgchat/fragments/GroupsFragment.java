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

import com.boringowl.rpgchat.adapters.GroupsAdapter;
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


public class GroupsFragment extends Fragment {
    private RecyclerView recyclerView;
    private GroupsAdapter adapter;
    private EditText searchText;

    private List<String> groupsList;
    private DatabaseReference groupRef;
    private String currentUserID = "";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View groupFragmentView = inflater.inflate(R.layout.fragment_groups, container, false);

        groupRef = FirebaseDatabase.getInstance().getReference().child("Groups");

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUserID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        groupFragmentView.findViewById(R.id.add_group_btn).setVisibility(View.GONE);
        recyclerView = groupFragmentView.findViewById(R.id.groups_list);
        searchText = groupFragmentView.findViewById(R.id.search_chats);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        groupsList = new ArrayList<>();

        retrieveAndDisplayGroups();

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

        return groupFragmentView;
    }

    private void searchChats(String s) {
        Query query = FirebaseDatabase.getInstance().getReference("Groups").orderByChild("info/searchname")
                .startAt(s)
                .endAt(s + "\uf8ff");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    groupsList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (snapshot.child("info").hasChild("insearch"))
                            if (Objects.requireNonNull(snapshot.child("info").child("insearch").getValue()).toString().equals("opened"))
                                groupsList.add(snapshot.getKey());
                    }
                    adapter = new GroupsAdapter(getContext(), groupsList, currentUserID);
                    recyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void retrieveAndDisplayGroups() {
        groupRef.orderByChild("info/searchname").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (searchText.getText().toString().equals("") && dataSnapshot.exists()) {
                    groupsList.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (snapshot.child("info").hasChild("insearch"))
                            if (Objects.requireNonNull(snapshot.child("info").child("insearch").getValue()).toString().equals("opened"))
                                groupsList.add(snapshot.getKey());
                    }
                    adapter = new GroupsAdapter(getContext(), groupsList, currentUserID);
                    recyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
