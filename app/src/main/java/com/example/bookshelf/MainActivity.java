package com.example.bookshelf;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import edu.temple.audiobookplayer.AudiobookService;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.example.bookshelf.MainActivity.isDownloadComplete;

public class MainActivity extends AppCompatActivity implements BookListFragment.BookSelectedInterface, ControlFragment.ControlInterface {

    private FragmentManager fm;

    private boolean twoPane;
    private  BookDetailsFragment bookDetailsFragment;
    private  ControlFragment controlFragment;
    public Book selectedBook, playingBook;

    private final String TAG_BOOKLIST = "booklist", TAG_BOOKDETAILS = "bookdetails";
    private final String KEY_SELECTED_BOOK = "selectedBook", KEY_PLAYING_BOOK = "playingBook";
    private final String KEY_BOOKLIST = "searchedook";
    private final int BOOK_SEARCH_REQUEST_CODE = 123;
    private final String DIR = "audioBook";
    //hi there
    private long Aid;
    public DownloadManager downloadManager;
    public String Tempfile;

    public static boolean isDownloadComplete = false;
    private AudiobookService.MediaControlBinder mediaControl;
    private boolean serviceConnected;
    boolean autoSave;

    SharedPreferences preferences;
    Intent serviceIntent;

    public BookList bookList; //booklists and stuff

    String internalFilename = "myfile";
    File file;

    Handler progressHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            // Don't update contols if we don't know what bok the service is playing
            if (message.obj != null && playingBook != null) {
                controlFragment.updateProgress((int) (((float) ((AudiobookService.BookProgress) message.obj).getProgress() / playingBook.getDuration()) * 100));
                controlFragment.setNowPlaying(getString(R.string.now_playing, playingBook.getTitle()));
            }

            return true;
        }
    });

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mediaControl = (AudiobookService.MediaControlBinder) iBinder;
            mediaControl.setProgressHandler(progressHandler);
            serviceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceConnected = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //catch the download being completed from download manager which uses bradcast message
        file = new File(getFilesDir(), internalFilename);

        preferences = getPreferences(MODE_PRIVATE);

        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        serviceIntent = new Intent (this, AudiobookService.class);

        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        fm = getSupportFragmentManager();

        findViewById(R.id.searchDialogButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(MainActivity.this, BookSearchActivity.class), BOOK_SEARCH_REQUEST_CODE);
            }
        });

        if (savedInstanceState != null) {
            // Fetch selected book if there was one
            selectedBook = savedInstanceState.getParcelable(KEY_SELECTED_BOOK);

            // Fetch playing book if there was one
            playingBook = savedInstanceState.getParcelable(KEY_PLAYING_BOOK);

            // Fetch previously searched books if one was previously retrieved
            bookList = savedInstanceState.getParcelable(KEY_BOOKLIST);
        }else {
            // Create empty booklist if
            bookList = new BookList();
        }
        autoSave = preferences.getBoolean("autoSave", false); // Read last saved value from preferences, or false if no value saved
        twoPane = findViewById(R.id.container2) != null;

        Fragment fragment1;
        fragment1 = fm.findFragmentById(R.id.container_1);

        // I will only ever have a single ControlFragment - if I created one before, reuse it
        if ((controlFragment = (ControlFragment) fm.findFragmentById(R.id.control_container)) == null) {
            controlFragment = new ControlFragment();
            fm.beginTransaction()
                    .add(R.id.control_container, controlFragment)
                    .commit();
        }


        // At this point, I only want to have BookListFragment be displayed in container_1
        if (fragment1 instanceof BookDetailsFragment) {
            fm.popBackStack();
        } else if (!(fragment1 instanceof BookListFragment))
            fm.beginTransaction()
                    .add(R.id.container_1, BookListFragment.newInstance(bookList), TAG_BOOKLIST)
            .commit();

        /*
        If we have two containers available, load a single instance
        of BookDetailsFragment to display all selected books
         */
        bookDetailsFragment = (selectedBook == null) ? new BookDetailsFragment() : BookDetailsFragment.newInstance(selectedBook);
        if (twoPane) {
            fm.beginTransaction()
                    .replace(R.id.container2, bookDetailsFragment, TAG_BOOKDETAILS)
                    .commit();
        } else if (selectedBook != null) {
            /*
            If a book was selected, and we now have a single container, replace
            BookListFragment with BookDetailsFragment, making the transaction reversible
             */
            fm.beginTransaction()
                    .replace(R.id.container_1, bookDetailsFragment, TAG_BOOKDETAILS)
                    .addToBackStack(null)
                    .commit();
        }

    }

    @Override
    public void bookSelected(int index) {
        // Store the selected book to use later if activity restarts
        selectedBook = bookList.get(index);

        if (twoPane)
            /*
            Display selected book using previously attached fragment
             */
            bookDetailsFragment.displayBook(selectedBook);
        else {
            /*
            Display book using new fragment
             */
            fm.beginTransaction()
                    .replace(R.id.container_1, BookDetailsFragment.newInstance(selectedBook), TAG_BOOKDETAILS)
                    // Transaction is reversible
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * Display new books when retrieved from a search
     */
    private void showNewBooks() {
        if ((fm.findFragmentByTag(TAG_BOOKDETAILS) instanceof BookDetailsFragment)) {
            fm.popBackStack();
        }
        ((BookListFragment) fm.findFragmentByTag(TAG_BOOKLIST)).showNewBooks();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SELECTED_BOOK, selectedBook);
        outState.putParcelable(KEY_PLAYING_BOOK, playingBook);
        outState.putParcelable(KEY_BOOKLIST, bookList);
    }

    @Override
    public void onBackPressed() {
        // If the user hits the back button, clear the selected book
        selectedBook = null;
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BOOK_SEARCH_REQUEST_CODE && resultCode == RESULT_OK) {
            bookList.clear();
            bookList.addAll((BookList) data.getParcelableExtra(BookSearchActivity.BOOKLIST_KEY));
            if (bookList.size() == 0) {
                Toast.makeText(this, getString(R.string.error_no_results), Toast.LENGTH_SHORT).show();
            }
            showNewBooks();
        }
    }

    public String download(Book book){
        // when getting the directory and path, make sure to use get absolute path. Important to get to access later
        File file = new File(MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), String.valueOf(book.getId()));
        Uri file_uri = Uri.parse("https://kamorris.com/lab/audlib/download.php?id="+book.getId());
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(file_uri);

        //Setting title of request
        request.setTitle("Audio Download : "+book.getTitle());

        //Setting description of request
        request.setDescription("Title: "+book.getTitle()+ " Author: "+book.getAuthor()+ " Duration: "+book.getDuration());

        // set the file URi as destination
        request.setDestinationUri(Uri.fromFile(file));
        //request.setDestinationInExternalFilesDir(MainActivity.this,Environment.DIRECTORY_DOWNLOADS,"");

        //Enqueue download
        Aid = downloadManager.enqueue(request);
        Log.d( "FILE", "download Status id: "+Aid);

        String path = file.getPath().toString();

        return path;
    }
    BroadcastReceiver onComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if(Aid == id){
                isDownloadComplete = true;
                bookList.getByTitle(playingBook).setFile(Tempfile);
                playingBook.setFile(Tempfile);
                selectedBook.setFile(Tempfile);
                Toast.makeText(MainActivity.this, "Download Complete", Toast.LENGTH_SHORT).show();
                Log.d("FILE", "File path for: "+bookList.getByTitle(playingBook).getFile());

            }
        }
    };
    private boolean validDownload(long downloadId) {

        Log.d("FILE","Checking download status for id: " + downloadId);

        //Verify if download is a success
        Cursor c= downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));

        if(c.moveToFirst()){
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));

            if(status == DownloadManager.STATUS_SUCCESSFUL){
                return true; //Download is valid, celebrate
            }else{
                int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                Log.d("FILE", "Download not correct, status [" + status + "] reason [" + reason + "]");
                return false;
            }
        }
        return false;
    }
    @Nullable
    File getAppSpecificAudioStorageDir(Context context, String bookTitle) {
        // Get the directory that's inside the app-specific directory on
        // external storage.
        File file = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_DOWNLOADS), bookTitle); //creates a new file instance from a pathname
        Log.d( "FILE", "File is made "+ file.getAbsolutePath());
        if (file == null || !file.exists()) {
            Log.d( "FILE", "File is NULL or Directory is not created");
        }
        return file;
    }


    @Override
    public void play() {
        if (selectedBook != null) {
            playingBook = selectedBook;
            controlFragment.setNowPlaying(getString(R.string.now_playing, selectedBook.getTitle())); //setting the title
//          File file = getAppSpecificAudioStorageDir(this, selectedBook.getTitle());
            Log.d( "FILE", bookList.getByTitle(selectedBook).getFile()+": path of selected book");
            if(bookList.getByTitle(selectedBook).getFile() == null){ //if file is not downloaded
                //Log.d( "FILE", bookList.getByTitle(selectedBook).getFile()+": does not exists");
                if (serviceConnected) {
                    mediaControl.play(selectedBook.getId());
                    Tempfile = download(playingBook);
                    Log.d( "FILE", "Playing from online check");

                }
                // for some reason only great expectations does not work, but other files works? when downloading
            }else{// if the File exists, play the downloaded file
                Log.d( "FILE", bookList.getByTitle(selectedBook).getFile()+": exists");
                File tempfile = new File(MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), String.valueOf(selectedBook.getId()));
                mediaControl.play(tempfile);
                Log.d( "FILE", "Playing from download check: ");
            }

            // Make sure that the service doesn't stop
            // if the activity is destroyed while the book is playing
            startService(serviceIntent);
        }
    }

    @Override
    public void pause() {
        if (serviceConnected) {
            mediaControl.pause();
        }
    }

    @Override
    public void stop() {
        if (serviceConnected)
            mediaControl.stop();

        // If no book is playing, then it's fine to let
        // the service stop once the activity is destroyed
        stopService(serviceIntent);
    }

    @Override
    public void changePosition(int progress) {
        if (serviceConnected)
            mediaControl.seekTo((int) ((progress / 100f) * playingBook.getDuration()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        unregisterReceiver(onComplete);
    }

}

