package com.example.bookshelf;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class BookList implements Parcelable {
    private ArrayList<Book> books;

    public BookList() {
        books = new ArrayList<>();
    }

    protected BookList(Parcel in) {
        books = in.createTypedArrayList(Book.CREATOR);
    }

    public static final Creator<BookList> CREATOR = new Creator<BookList>() {
        @Override
        public BookList createFromParcel(Parcel in) {
            return new BookList(in);
        }

        @Override
        public BookList[] newArray(int size) {
            return new BookList[size];
        }
    };

    public void clear () {
        books.clear();
    }

    public void addAll (BookList books) {
        for (int i = 0; i < books.size(); i++) {
            this.books.add(books.get(i));
        }
    }

    public void add(Book book) {
        books.add(book);
    }

    public Book get(int position) {
        return books.get(position);
    }

    public int size() {
        return books.size();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(books);
    }
}
