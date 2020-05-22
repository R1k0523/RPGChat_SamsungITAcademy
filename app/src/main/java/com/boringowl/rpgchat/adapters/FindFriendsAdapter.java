package com.boringowl.rpgchat.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.boringowl.rpgchat.dialogs.ProfileDialog;
import com.boringowl.rpgchat.models.Contacts;
import com.boringowl.rpgchat.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FindFriendsAdapter extends RecyclerView.Adapter<FindFriendsAdapter.FindFriendViewHolder> {
    private List<Contacts> contactsList;
    private FragmentManager fragmentManager;

    public FindFriendsAdapter(List<Contacts> contactsList, FragmentManager fragmentManager) {
        this.contactsList = contactsList;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public FindFriendsAdapter.FindFriendViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_user, viewGroup, false);
        return new FindFriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final FindFriendsAdapter.FindFriendViewHolder holder, final int position) {

        holder.userName.setText(contactsList.get(position).getName());
        holder.userStatus.setText(contactsList.get(position).getStatus());
        Picasso.get().load(contactsList.get(position).getImage()).placeholder(R.drawable.profile_image).into(holder.profileImage);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProfileDialog dialog = new ProfileDialog(contactsList.get(position).getUid());
                dialog.show(fragmentManager, "a");
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactsList.size();
    }

    class FindFriendViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userStatus;
        CircleImageView profileImage;

        FindFriendViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.users_profile_image);
        }
    }
}
