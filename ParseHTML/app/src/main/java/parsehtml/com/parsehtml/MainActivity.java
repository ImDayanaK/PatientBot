package parsehtml.com.parsehtml;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Document htmlDocument;
    String apiResponse;
    String url = "https://patient.info/search.asp?searchTerm=";
    String collectionBind = "&collections=All";
    List<SymptomsSearchModel> symptomsSearchList;
    private RecyclerView searchList;
    private EditText searchInput;
    private Button searchBtn;
    private String searchString;
    private TextView searchResultLabel;
    ProgressDialog progress;
    String serviceUrl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // RequestQueue queue = Volley.newRequestQueue(this);
        hideKeyBoard(this);
        searchList = (RecyclerView) findViewById(R.id.searchlistview);
        searchInput = (EditText) findViewById(R.id.inputSearch);
        searchBtn = (Button) findViewById(R.id.search);
        searchResultLabel = (TextView) findViewById(R.id.search_results);

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyBoard(MainActivity.this);
                serviceUrl = url + searchInput.getText().toString() + collectionBind;
                new StartSync().execute();
            }
        });


    }

    private void populateValues() {
        if (symptomsSearchList != null && symptomsSearchList.size() > 0) {
            searchResultLabel.setVisibility(View.VISIBLE);
            searchList.setAdapter(new SearchResultAdapter(symptomsSearchList, this));
        } else {
            searchResultLabel.setVisibility(View.GONE);
            searchList.setVisibility(View.GONE);
        }


    }

    private class StartSync extends AsyncTask<Void, Void, Document> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = new ProgressDialog(MainActivity.this);
            progress.setMessage("Searching symptoms");
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
          //  Elements sections = document.select("section.search-results");
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
                Log.d("jsoup", "size: " + li.size());
            }

            populateValues();
        }
    }

    public static void hideKeyBoard(MainActivity view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) view.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindow().getDecorView().getRootView().getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }
    }


}
