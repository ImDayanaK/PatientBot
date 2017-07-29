package parsehtml.com.parsehtml;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SymptomDetailActivity extends AppCompatActivity {
    private WebView detailView;
    private Document htmlDocument;
    String apiResponse;
    String url = " https://patient.info/health/";
    String collectionBind = "&collections=All";
    List<SymptomsSearchModel> symptomsSearchList;
    private RecyclerView searchList;
    private TextView searchInput;
    private Button searchBtn;
    private String searchString;
    private TextView searchResultLabel;
    ProgressDialog progress;
    String serviceUrl;
    String summary;
    List<SymptomsSearchModel> symptoms = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_detail);
        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        searchList = (RecyclerView) findViewById(R.id.detaillist);
        searchInput = (TextView) findViewById(R.id.summary);
        serviceUrl = url;
        new StartSync().execute();

    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    /**
     * Opens the URL in a browser
     */
    private void openURL(String url) {
        detailView.loadUrl(url);
        detailView.requestFocus();
    }

    private class StartSync extends AsyncTask<Void, Void, Document> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = new ProgressDialog(SymptomDetailActivity.this);
            progress.setMessage("Loading detail");
            progress.show();
        }

        @Override
        protected Document doInBackground(Void... voids) {
            System.out.println("url " + serviceUrl);
            try {
                htmlDocument = Jsoup.connect(serviceUrl).get();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return htmlDocument;
        }

        @Override
        protected void onPostExecute(Document document) {
            super.onPostExecute(document);
            progress.dismiss();
            System.out.println("Html doc " + document);
            Elements content = document.select("section#content");
            System.out.println("content *** " + content.size());

            Elements sections1 = document.select("div.container_12");
            System.out.println("sections1 *** " + sections1.size());

            Elements container1 = document.select("div#container");
            System.out.println("container1 *** " + container1.size());

            Elements body = document.select("div.body bt-body document");
            System.out.println("body *** " + body.size());

            Elements read = document.select("div#readspeaker-content");
            System.out.println("body *** " + read.size());


            for (int i = 0; i < read.size(); i++) {
                Elements summaryele = read.get(i).getAllElements();
                System.out.println("***summaryele size  " + summaryele.size());
                summary = summaryele.select("div.summary > p").text();
                System.out.println("***summary: " + summaryele.select("div.summary > p").text());
                Elements ul = summaryele.select("ul");
                System.out.println("***ul size : " + ul.size());
                Elements li = ul.select("li"); // select all li from ul
                System.out.println("***li size : " + li.size());


                for (int l = 0; l < li.size(); l++) {
                    SymptomsSearchModel searchModel = new SymptomsSearchModel();
                    searchModel.setContent(li.get(l).select("li").text());
                    symptoms.add(searchModel);
                }

            }
            if (symptoms.size() > 5)
                symptoms = symptoms.subList(0, 5);
            populateValues();

        }


    }

    private void populateValues() {
        searchInput.setText(summary);
        if (symptoms != null && symptoms.size() > 0) {
            //searchResultLabel.setVisibility(View.VISIBLE);
            searchList.setAdapter(new DetailAdapter(symptoms, SymptomDetailActivity.this));
        } else {
//            searchResultLabel.setVisibility(View.GONE);
//            searchList.setVisibility(View.GONE);
        }
    }
}

