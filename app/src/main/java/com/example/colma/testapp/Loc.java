package com.example.colma.testapp;

import android.os.Parcel;
import android.os.Parcelable;

public class Loc implements Parcelable {
    double latitude;
    double longitude;

    public Loc() {
    }

    public Loc(double lat, double lon) {
        this.latitude = lat;
        this.longitude = lon;
    }

    public Loc(Parcel source) {
        latitude = source.readDouble();
        longitude = source.readDouble();
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }

    public static final Creator<Loc> CREATOR = new Creator<Loc>() {
        @Override
        public Loc[] newArray(int size) {
            return new Loc[size];
        }

        @Override
        public Loc createFromParcel(Parcel source) {
            return new Loc(source);
        }
    };
}
