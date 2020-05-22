package com.boringowl.rpgchat.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.boringowl.rpgchat.adapters.MyGroupsAdapter;
import com.boringowl.rpgchat.R;
import com.boringowl.rpgchat.tools.TimeHandler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class MyGroupsFragment extends Fragment {
    private RecyclerView recycler_view;
    private MyGroupsAdapter adapter;
    private EditText searchText;

    private List<String> list_of_groups;
    private DatabaseReference GroupRef;
    private String currentUserID = "";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View groupFragmentView = inflater.inflate(R.layout.fragment_groups, container, false);

        GroupRef = FirebaseDatabase.getInstance().getReference().child("Groups");

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        searchText = groupFragmentView.findViewById(R.id.search_chats);

        FloatingActionButton addGroupButton = groupFragmentView.findViewById(R.id.add_group_btn);
        recycler_view = groupFragmentView.findViewById(R.id.groups_list);

        recycler_view.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        recycler_view.setLayoutManager(linearLayoutManager);

        list_of_groups = new ArrayList<>();

        RetrieveAndDisplayGroups();

        addGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RequestNewGroup();
            }
        });
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

    private void CreateNewGroup(final String groupName, final String groupPassword) {
        final DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference().child("Groups").push();

        groupRef.child("info").child("password").setValue(groupPassword);
        groupRef.child("info").child("creator").setValue(currentUserID);
        groupRef.child("info").child("groupname").setValue(groupName);
        groupRef.child("info").child("searchname").setValue(groupName.toLowerCase());
        groupRef.child("info").child("insearch").setValue("closed");
        groupRef.child("info").child("lastmessage").setValue(TimeHandler.getLastTime());
        groupRef.child("Members").child(currentUserID).setValue("");
        RetrieveAndDisplayGroups();
    }


    private void RequestNewGroup() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialog);
        builder.setTitle("Create Group");
        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_group, null);
        final EditText groupNameText = dialogView.findViewById(R.id.group_name_text);
        final EditText groupPasswordText = dialogView.findViewById(R.id.password_text);
        final CheckBox hasPasswordBox = dialogView.findViewById(R.id.has_password);

        builder.setView(dialogView);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String groupName = groupNameText.getText().toString();
                String groupPassword = "";

                if (hasPasswordBox.isChecked())
                    groupPassword = groupPasswordText.getText().toString();

                if (TextUtils.isEmpty(groupName) || (hasPasswordBox.isActivated() && groupPassword.length() == 0)) {
                    Toast.makeText(getActivity(), "Please check your input data...", Toast.LENGTH_SHORT).show();
                } else {
                    CreateNewGroup(groupName, groupPassword);
                    dialogInterface.dismiss();
                }

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }

    private void searchChats(String s) {
        Query query = FirebaseDatabase.getInstance().getReference("Groups").orderByChild("info/searchname")
                .startAt(s)
                .endAt(s + "\uf8ff");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                list_of_groups.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.child("Members").hasChild(currentUserID))
                        list_of_groups.add(snapshot.getKey());
                }
                adapter = new MyGroupsAdapter(getContext(), list_of_groups);
                recycler_view.setAdapter(adapter);
                recycler_view.scrollToPosition(recycler_view.getAdapter().getItemCount() - 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void RetrieveAndDisplayGroups() {
        GroupRef.orderByChild("info/lastmessage").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                list_of_groups.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.child("Members").hasChild(currentUserID))
                        list_of_groups.add(snapshot.getKey());
                }
                adapter = new MyGroupsAdapter(getContext(), list_of_groups);
                recycler_view.setAdapter(adapter);
                recycler_view.scrollToPosition(recycler_view.getAdapter().getItemCount() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
