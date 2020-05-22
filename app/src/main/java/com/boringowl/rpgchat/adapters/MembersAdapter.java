package com.boringowl.rpgchat.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.boringowl.rpgchat.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.GroupsViewHolder> {
    private List<String> membersList;
    private Context mContext;
    private String currentUserID, groupId, chatCreatorId;
    private DatabaseReference UsersRef, GroupRef;

    public MembersAdapter(Context mContext, List<String> membersList, String currentUserID, String groupId, String chatCreatorId) {
        this.mContext = mContext;
        this.membersList = membersList;
        this.currentUserID = currentUserID;
        this.groupId = groupId;
        this.chatCreatorId = chatCreatorId;
    }

    @NonNull
    @Override
    public MembersAdapter.GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_member, viewGroup, false);

        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        GroupRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(groupId);

        return new GroupsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MembersAdapter.GroupsViewHolder holder, final int position) {
        final String usersIDs = membersList.get(position);

        UsersRef.child(usersIDs).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("image")) {
                    Picasso.get().load(Objects.requireNonNull(dataSnapshot.child("image").getValue())
                            .toString()).placeholder(R.drawable.profile_image).into(holder.memberImage);
                }
                String username = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                if (currentUserID.equals(membersList.get(position))) {
                    username += " (You)";
                }
                holder.memberName.setText(username);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        if (currentUserID.equals(membersList.get(position)) || !currentUserID.equals(chatCreatorId)) {
            holder.deleteButton.setVisibility(View.GONE);
        } else {
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GroupRef.child("Members").child(usersIDs).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(mContext, "User deleted", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return membersList.size();
    }

    class GroupsViewHolder extends RecyclerView.ViewHolder {
        TextView memberName;
        CircleImageView memberImage;
        ImageButton deleteButton;

        GroupsViewHolder(@NonNull View itemView) {
            super(itemView);

            memberName = itemView.findViewById(R.id.member_name);
            memberImage = itemView.findViewById(R.id.member_image);
            deleteButton = itemView.findViewById(R.id.delete_btn);
        }
    }
}
