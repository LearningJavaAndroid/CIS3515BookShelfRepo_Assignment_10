package com.example.bookshelf;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BookSearchActivity extends AppCompatActivity {

    private final String urlPrefix = "https://kamorris.com/lab/cis3515/search.php?term=";
    public static final String BOOKLIST_KEY = "booklist";

    // The JSON object fields for a book
    private final String id = "id", title = "title", author = "author", cover_url = "cover_url", duration ="duration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_search);

        final RequestQueue requestQueue = Volley.newRequestQueue(this);

        findViewById(R.id.searchButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String searchUrl = urlPrefix + ((EditText) findViewById(R.id.searchEditText)).getText().toString();
                requestQueue.add(new JsonArrayRequest(searchUrl, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Intent resultIntent = new Intent();

                        // Return retrieved books to calling activity
                        resultIntent.putExtra(BOOKLIST_KEY, getBookListFromJsonArray(response));
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), getString(R.string.search_error_message), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }));
            }
        });

        findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private BookList getBookListFromJsonArray(JSONArray booksArray) {
        BookList bookList = new BookList();
        JSONObject tmpBook;

        // Convert all books retrieved in the JSON array to books in a booklist object
        for (int i = 0; i < booksArray.length(); i++) {
            try {
                tmpBook = booksArray.getJSONObject(i);
                bookList.add(new Book(tmpBook.getInt(id), tmpBook.getString(title), tmpBook.getString(author), tmpBook.getString(cover_url), tmpBook.getInt("duration")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return bookList;
    }
}