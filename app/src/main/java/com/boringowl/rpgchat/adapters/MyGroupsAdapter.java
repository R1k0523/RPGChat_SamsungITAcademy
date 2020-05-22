package com.boringowl.rpgchat.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.boringowl.rpgchat.GroupChatActivity;
import com.boringowl.rpgchat.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MyGroupsAdapter extends RecyclerView.Adapter<MyGroupsAdapter.GroupsViewHolder> {
    private List<String> groupList;
    private Context mContext;

    public MyGroupsAdapter(Context mContext, List<String> groupList) {
        this.mContext = mContext;
        this.groupList = groupList;
    }

    @NonNull
    @Override
    public MyGroupsAdapter.GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_group, viewGroup, false);
        return new GroupsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyGroupsAdapter.GroupsViewHolder holder, final int position) {
        final DatabaseReference GroupRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(groupList.get(position));


        GroupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                holder.groupName.setText(Objects.requireNonNull(dataSnapshot.child("info").child("groupname").getValue()).toString());
                if (dataSnapshot.child("info").hasChild("image")) {
                    Picasso.get().load(Objects.requireNonNull(dataSnapshot.child("info").child("image").getValue()).toString()).placeholder(R.drawable.profile_image).into(holder.groupImage);
                }
                String message = "No messages";
                if (dataSnapshot.hasChild("Messages")) {
                    for (DataSnapshot snap : dataSnapshot.child("Messages").getChildren()) {
                        if (Objects.requireNonNull(snap.child("type").getValue()).toString().equals("text")) {
                            message = Objects.requireNonNull(snap.child("name").getValue()).toString() + ": " + Objects.requireNonNull(snap.child("message").getValue()).toString();
                        } else {
                            message = Objects.requireNonNull(snap.child("name").getValue()).toString() + ": Picture";
                        }
                    }
                }
                holder.groupLast.setText(message);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }

        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToGroupChatActivity(position);
            }
        });
    }


    @Override
    public int getItemCount() {
        return groupList.size();
    }

    private void SendUserToGroupChatActivity(int position) {
        Intent groupChatIntent = new Intent(mContext, GroupChatActivity.class);
        groupChatIntent.putExtra("groupID", groupList.get(position));
        mContext.startActivity(groupChatIntent);
    }

    class GroupsViewHolder extends RecyclerView.ViewHolder {
        TextView groupName, groupLast;
        CircleImageView groupImage;

        GroupsViewHolder(@NonNull View itemView) {
            super(itemView);

            groupName = itemView.findViewById(R.id.group_name);
            groupLast = itemView.findViewById(R.id.group_last_message);
            groupImage = itemView.findViewById(R.id.group_image);

            itemView.findViewById(R.id.group_last_message).setVisibility(View.GONE);
            groupLast.setVisibility(View.VISIBLE);
        }
    }
}
