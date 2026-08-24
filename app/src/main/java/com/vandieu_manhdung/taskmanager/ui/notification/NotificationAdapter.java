package com.vandieu_manhdung.taskmanager.ui.notification;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.AppNotification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.Holder> {
    interface Listener { void onClick(AppNotification item); }
    private final Listener listener;
    private final List<AppNotification> items = new ArrayList<>();

    NotificationAdapter(Listener listener) { this.listener = listener; }
    void submit(List<AppNotification> values) {
        items.clear();
        if (values != null) items.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }
    @Override public int getItemCount() { return items.size(); }

    class Holder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView message;
        private final TextView time;
        Holder(View view) {
            super(view);
            title = view.findViewById(R.id.textNotificationTitle);
            message = view.findViewById(R.id.textNotificationMessage);
            time = view.findViewById(R.id.textNotificationTime);
        }
        void bind(AppNotification item) {
            title.setText(item.getTitle());
            title.setTypeface(null, item.isRead() ? Typeface.NORMAL : Typeface.BOLD);
            message.setText(item.getMessage());
            time.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date(item.getCreatedAt())));
            itemView.setAlpha(item.isRead() ? 0.72f : 1f);
            itemView.setOnClickListener(v -> listener.onClick(item));
        }
    }
}
