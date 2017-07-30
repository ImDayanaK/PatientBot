package com.patientbot;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jayanthi Venkat on 7/30/2017.
 */

public class SearchTermAsync extends AsyncTask<Void, Void, Document> {
    private ChatBoxActivity chatBoxActivity;
    private String serviceUrl;
    private Document htmlDocument;
    private List<SymptomsSearchModel> symptomsSearchList;
    public SearchTermAsync(ChatBoxActivity chatBoxActivity, String serviceUrl) {
        this.chatBoxActivity = chatBoxActivity;
        this.serviceUrl = serviceUrl;
        Log.d("TAG", "---service url" + serviceUrl);
    }

    @Override
    protected Document doInBackground(Void... params) {
        try {
            htmlDocument = Jsoup.connect(serviceUrl).get();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return htmlDocument;
    }

    @Override
    protected void onPostExecute(Document document) {
        Elements ol = document.select("section.search-results > ol");
        Elements li = ol.select("li"); // select all li from ul
        if (li != null) {
            symptomsSearchList = new ArrayList<>();
            for (int i = 0; i < li.size(); i++) {
                SymptomsSearchModel symptomsSearchModel = new SymptomsSearchModel();
                Log.d("jsoup", " " + li.get(i).select("href > href").text());
                String output = li.get(i).getElementsByAttribute("href")
                        .attr("href");
                String title = li.get(i).select("h3").text();
                String content = li.get(i).select("p").text();
                symptomsSearchModel.setRefLink(output);
                symptomsSearchModel.setTitle(title);
                symptomsSearchModel.setContent(content);
                if (title != null && title.length() > 0)
                    symptomsSearchList.add(symptomsSearchModel);
            }
            Log.d("jsoup", "----size: " + li.size());
        }
        chatBoxActivity.updateViewOnResult(symptomsSearchList.get(0));
    }
}


