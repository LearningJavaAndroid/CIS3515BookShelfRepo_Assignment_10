package com.example.bookshelf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.example.bookshelf.MainActivity.isDownloadComplete;

public class BookDownload extends BroadcastReceiver {
    
    //notifies us if download is complete by isdownloadComplete boolean, true means download complete
    @Override
    public void onReceive(Context context, Intent intent) {
        isDownloadComplete = true;
        Log.i("Download completed?", String.valueOf(isDownloadComplete));
    }


}
