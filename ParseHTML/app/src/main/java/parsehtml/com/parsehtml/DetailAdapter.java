package parsehtml.com.parsehtml;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class DetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<SymptomsSearchModel> symptomsSearchList;
    private SymptomDetailActivity mainActivity;


    public DetailAdapter(List<SymptomsSearchModel> symptomsList, SymptomDetailActivity mainActivity) {
        this.symptomsSearchList = symptomsList;
        this.mainActivity = mainActivity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_detail_item, parent, false);
        return new ContentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {

        SymptomsSearchModel symptomsSearchModel = symptomsSearchList.get(position);
        ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
        contentViewHolder.mItem = symptomsSearchModel;

        contentViewHolder.tvContent.setText(symptomsSearchModel.getContent());

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

        SymptomsSearchModel mItem;

        public ContentViewHolder(View view) {
            super(view);
            mView = view;
            tvContent = (TextView) view.findViewById(R.id.detail);

        }

        @Override
        public void onClick(View v) {
            int id = v.getId();

        }
    }

    private void loadSymptom(String url) {
        Intent intent = new Intent(mainActivity, SymptomDetailActivity.class);

        intent.putExtra("url", url);
        mainActivity.startActivity(intent);

    }

}
