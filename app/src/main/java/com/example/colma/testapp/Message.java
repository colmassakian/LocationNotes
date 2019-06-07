package com.example.colma.testapp;

import android.os.Parcel;
import android.os.Parcelable;

public class Message implements Parcelable {
    String id;
    String messageText;
    Loc location;


    public Message()
    {

    }

    public Message(String id, String messageText, Loc loc) {
        this.id = id;
        this.messageText = messageText;
        this.location = loc;
    }

    public Message(Parcel source) {
        id = source.readString();
        messageText = source.readString();
        location = source.readParcelable(getClass().getClassLoader());
    }

    public String getId() {
        return id;
    }

    public String getMessageText() {
        return messageText;
    }

    public Loc getLocation() {
        return location;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(messageText);
        dest.writeParcelable(location, flags);
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }

        @Override
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }
    };
}
