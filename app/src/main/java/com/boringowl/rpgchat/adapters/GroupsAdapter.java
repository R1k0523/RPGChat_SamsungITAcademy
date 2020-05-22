package com.boringowl.rpgchat.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.GroupsViewHolder> {
    private List<String> grouplist;
    private Context mContext;
    private String currentUserID;

    public GroupsAdapter(Context mContext, List<String> grouplist, String currentUserID) {
        this.mContext = mContext;
        this.grouplist = grouplist;
        this.currentUserID = currentUserID;
    }

    @NonNull
    @Override
    public GroupsAdapter.GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_group, viewGroup, false);
        return new GroupsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final GroupsAdapter.GroupsViewHolder holder, final int position) {
        final DatabaseReference GroupRef = FirebaseDatabase.getInstance().getReference().child("Groups").child(grouplist.get(position));

        GroupRef.child("info").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                holder.groupName.setText(Objects.requireNonNull(dataSnapshot.child("groupname").getValue()).toString());
                if (dataSnapshot.hasChild("image")) {
                    Picasso.get().load(Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString()).placeholder(R.drawable.profile_image).into(holder.groupImage);
                }
                if (dataSnapshot.hasChild("description")) {
                    holder.groupDesc.setText(dataSnapshot.child("description").getValue().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                GroupRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final String password = Objects.requireNonNull(dataSnapshot.child("info").child("password").getValue()).toString();
                        if (!password.equals("") && !dataSnapshot.child("Members").hasChild(currentUserID)) {

                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.AlertDialog);
                            builder.setTitle("Enter password");

                            final EditText groupPasswordField = new EditText(mContext);
                            groupPasswordField.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            groupPasswordField.setHint("password");

                            builder.setView(groupPasswordField);

                            builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String enteredPassword = groupPasswordField.getText().toString();

                                    if (TextUtils.isEmpty(enteredPassword) || !enteredPassword.equals(password)) {
                                        Toast.makeText(mContext, "Wrong password...", Toast.LENGTH_SHORT).show();
                                        groupPasswordField.setText("");
                                    } else {
                                        AddUserToGroup(GroupRef);
                                        SendUserToGroupChatActivity(position);
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

                        } else {
                            AddUserToGroup(GroupRef);
                            SendUserToGroupChatActivity(position);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });


            }
        });
    }

    @Override
    public int getItemCount() {
        return grouplist.size();
    }

    private void SendUserToGroupChatActivity(int position) {
        Intent groupChatIntent = new Intent(mContext, GroupChatActivity.class);
        groupChatIntent.putExtra("groupID", grouplist.get(position));
        mContext.startActivity(groupChatIntent);
    }

    private void AddUserToGroup(DatabaseReference GroupRef) {
        GroupRef.child("Members").child(currentUserID).setValue("");
    }

    class GroupsViewHolder extends RecyclerView.ViewHolder {
        TextView groupName, groupDesc;
        CircleImageView groupImage;

        GroupsViewHolder(@NonNull View itemView) {
            super(itemView);

            groupName = itemView.findViewById(R.id.group_name);
            groupDesc = itemView.findViewById(R.id.group_desc);
            groupImage = itemView.findViewById(R.id.group_image);
        }
    }
}
