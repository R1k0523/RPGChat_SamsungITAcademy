package com.boringowl.rpgchat.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.boringowl.rpgchat.ChatActivity;
import com.boringowl.rpgchat.R;
import com.boringowl.rpgchat.tools.TimeHandler;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatsViewHolder> {
    private List<String> usersList;
    private Context mContext;
    private DatabaseReference usersRef;

    public ChatsAdapter(Context mContext, List<String> usersList, DatabaseReference UsersRef) {
        this.usersList = usersList;
        this.mContext = mContext;
        this.usersRef = UsersRef;
    }

    @NonNull
    @Override
    public ChatsAdapter.ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_user, viewGroup, false);
        return new ChatsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ChatsAdapter.ChatsViewHolder holder, final int position) {
        final String usersIDs = usersList.get(position);
        final String[] retImage = {"default_image"};

        assert usersIDs != null;
        usersRef.child(usersIDs).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (dataSnapshot.hasChild("image")) {
                        retImage[0] = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();
                        Picasso.get().load(retImage[0]).placeholder(R.drawable.profile_image).into(holder.profileImage);
                    }

                    final String retName = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();

                    holder.userName.setText(retName);

                    if (dataSnapshot.child("userState").hasChild("state")) {
                        String state = Objects.requireNonNull(dataSnapshot.child("userState").child("state").getValue()).toString();
                        String timeDate = Objects.requireNonNull(dataSnapshot.child("userState").child("time").getValue()).toString();
                        timeDate = TimeHandler.convertTime(timeDate);
                        String date = timeDate.split(",")[0];
                        String time = timeDate.split(",")[1];

                        if (state.equals("online")) {
                            holder.userStatus.setText(mContext.getResources().getString(R.string.online));
                            holder.onlineStatus.setVisibility(View.VISIBLE);
                        } else if (state.equals(mContext.getResources().getString(R.string.offline))) {
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat nowDate = new SimpleDateFormat("dd MMM", Locale.ENGLISH);
                            String currentDate = nowDate.format(calendar.getTime());
                            String status = "Last Seen: " + date + ", " + time;
                            if (currentDate.equals(date))
                                status = "Last Seen: " + time;

                            holder.userStatus.setText(status);
                            holder.onlineStatus.setVisibility(View.GONE);

                        }
                    } else {
                        holder.userStatus.setText(mContext.getResources().getString(R.string.offline));
                    }

                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent chatIntent = new Intent(mContext, ChatActivity.class);
                            chatIntent.putExtra("visit_user_id", usersIDs);
                            chatIntent.putExtra("visit_user_name", retName);
                            chatIntent.putExtra("visit_image", retImage[0]);
                            mContext.startActivity(chatIntent);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    static class ChatsViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        ImageView onlineStatus;
        TextView userStatus, userName;

        ChatsViewHolder(@NonNull View itemView) {
            super(itemView);

            profileImage = itemView.findViewById(R.id.users_profile_image);
            userStatus = itemView.findViewById(R.id.user_status);
            onlineStatus = itemView.findViewById(R.id.user_online_status);
            userName = itemView.findViewById(R.id.user_profile_name);
        }
    }
}
