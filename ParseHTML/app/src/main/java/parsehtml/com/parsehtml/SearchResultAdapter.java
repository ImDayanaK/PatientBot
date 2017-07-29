package parsehtml.com.parsehtml;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<SymptomsSearchModel> symptomsSearchList;
    private MainActivity mainActivity;


    public SearchResultAdapter(List<SymptomsSearchModel> symptomsList, MainActivity mainActivity) {
        this.symptomsSearchList = symptomsList;
        this.mainActivity = mainActivity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_search_item, parent, false);
        return new ContentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {

        SymptomsSearchModel symptomsSearchModel = symptomsSearchList.get(position);
        ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
        contentViewHolder.mItem = symptomsSearchModel;

        contentViewHolder.tvContent.setText(symptomsSearchModel.getContent());
        contentViewHolder.tvTitle.setText(symptomsSearchModel.getTitle());
        contentViewHolder.tvLink.setText(Html.fromHtml(symptomsSearchModel.getRefLink()));
        contentViewHolder.tvLink.setLinkTextColor(Color.BLUE);
    }


    @Override
    public int getItemCount() {
        return symptomsSearchList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    private class ContentViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final View mView;
        final TextView tvContent;
        final TextView tvTitle;
        final TextView tvLink;
        SymptomsSearchModel mItem;

        public ContentViewHolder(View view) {
            super(view);
            mView = view;
            tvContent = (TextView) view.findViewById(R.id.content);
            tvTitle = (TextView) view.findViewById(R.id.title);
            tvLink = (TextView) view.findViewById(R.id.link);
            tvLink.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.link)
                loadSymptom(tvLink.getText().toString());
        }
    }

    private void loadSymptom(String url) {
        Intent intent = new Intent(mainActivity, SymptomDetailActivity.class);

        intent.putExtra("url", url);
        mainActivity.startActivity(intent);

    }

}
