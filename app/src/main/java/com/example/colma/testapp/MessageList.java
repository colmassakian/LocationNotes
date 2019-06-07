package com.example.colma.testapp;

import android.app.Activity;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class MessageList extends ArrayAdapter<Message> {

    private Activity context;
    private List<Message> messageList;

    public MessageList(Activity context, List<Message> messageList)
    {
        super(context, R.layout.list_layout, messageList);
        this.context = context;
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();

        View listViewItem = inflater.inflate(R.layout.list_layout, null, true);

        TextView textViewMessage = (TextView) listViewItem.findViewById(R.id.textViewMessage);

        Message message = messageList.get(position);

        textViewMessage.setText(message.getMessageText());

        return listViewItem;
    }
}
