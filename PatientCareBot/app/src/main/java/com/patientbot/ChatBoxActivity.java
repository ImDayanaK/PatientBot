/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.patientbot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatBoxActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener, AIListener, View.OnClickListener {

    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private static final String MESSAGE_URL = "http://friendlychat.firebase.google.com/message/";
    public String userAccName = "user1";
    int count = 1;
    private AIService aiService;
    private AIDataService aiDataService;
    private AIConfiguration config;
    private String mUsername = "anonymous";
    private String mPhotoUrl;
    private SharedPreferences mSharedPreferences;
    private GoogleApiClient mGoogleApiClient;
    private Button mSendButton;
    private Button mSendSymptom;
    private Button mNoneOfThese;
    private Button btnYes;
    private Button btnNo;
    private LinearLayout searchInfoParent;
    private TextView tvSearchTerm;
    private TextView searchMoreInfo;
    private TextView searchTermContent;
    private RecyclerView mMessageRecyclerView;
    private FlexboxLayout mFlexibleLayout;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ImageView mMike;
    private LinearLayout userInteractionLinearLayout;
    private LinearLayout symptomSendParent;
    private LinearLayout optionsParent;
    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;
    private ArrayList<Button> buttonList = new ArrayList<>();
    private FirebaseRecyclerAdapter<BotMessage, MessageViewHolder> mFirebaseAdapter;
   // private String symptoms = "";
    private List<String> responseList = new ArrayList<>();
    private String url = "https://patient.info/search.asp?searchTerm=";
    private String collectionBind = "&collections=All";
    private String serviceUrl;
    private String searchTerm;
    private SymptomsSearchModel symptomsSearchModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_box);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (getIntent() != null && getIntent().getStringExtra("userId") != null) {
            userAccName = getIntent().getStringExtra("userId");
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();
        initializeViews();
        launchSignInActivity();
        updateToFirebase("Welcome to PatientBot");// Welcome Message
    }

    private void initializeViews() {
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        userInteractionLinearLayout = (LinearLayout) findViewById(R.id.userInteractionLinearLayout);
        symptomSendParent = (LinearLayout) findViewById(R.id.symptom_send_parent);
        optionsParent = (LinearLayout) findViewById(R.id.option_parent);
        searchInfoParent = (LinearLayout) findViewById(R.id.search_term_parent);
        mMike = (ImageView) findViewById(R.id.imageMike);
        mMike.setOnClickListener(this);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mFlexibleLayout = (FlexboxLayout) findViewById(R.id.flexibleLayout);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        config = new AIConfiguration("09744aa8a2a04880bd0120c8da49e3d5",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        // Use with text search
        aiDataService = new AIDataService(this, config);
        // Use with Voice input
        aiService = AIService.getService(this, config);
        aiService.setListener(this);
        // New child entries
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        setupFirebaseAdapter();
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
       /* mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});*/
        mMessageEditText.addTextChangedListener(new CustomTextWatcher());
        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(this);
        mSendSymptom = (Button) findViewById(R.id.symptom_send);
        mSendSymptom.setEnabled(false);
        mSendSymptom.setOnClickListener(this);
        mNoneOfThese = (Button) findViewById(R.id.symptom_none);
        mNoneOfThese.setOnClickListener(this);
        btnYes = (Button) findViewById(R.id.option_yes);
        btnYes.setOnClickListener(this);
        btnNo = (Button) findViewById(R.id.option_no);
        btnNo.setOnClickListener(this);
        tvSearchTerm = (TextView) findViewById(R.id.searchTerm);
        searchMoreInfo = (TextView) findViewById(R.id.moreInfo);
        searchMoreInfo.setOnClickListener(this);
        searchTermContent = (TextView) findViewById(R.id.searchTermContent);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
    }

    private void setupFirebaseAdapter() {
        mFirebaseAdapter = new FirebaseRecyclerAdapter<BotMessage,
                MessageViewHolder>(
                BotMessage.class,
                R.layout.item_message,
                MessageViewHolder.class,
                mFirebaseDatabaseReference.child(userAccName)) {


            @Override
            protected void populateViewHolder(final MessageViewHolder viewHolder,
                                              BotMessage botMessage, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (botMessage.getText() != null) {
                    viewHolder.bindData(botMessage);
                }
                if (botMessage.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setVisibility(View.GONE);
                    viewHolder.mBotImageView.setVisibility(View.VISIBLE);
                    viewHolder.mBotImageView.setImageDrawable(ContextCompat.getDrawable(ChatBoxActivity.this,
                            R.drawable.bot));
                } else {
                    if(botMessage.getName().equals("bot")) {
                        viewHolder.mBotImageView.setVisibility(View.VISIBLE);
                        viewHolder.messengerImageView.setVisibility(View.GONE);
                        Glide.with(ChatBoxActivity.this)
                                .load(botMessage.getPhotoUrl())
                                .into(viewHolder.mBotImageView);
                    }else{
                        viewHolder.mBotImageView.setVisibility(View.GONE);
                        viewHolder.messengerImageView.setVisibility(View.VISIBLE);
                        Glide.with(ChatBoxActivity.this)
                                .load(botMessage.getPhotoUrl())
                                .into(viewHolder.messengerImageView);
                    }
                }
            }
        };
        mFirebaseAdapter.registerAdapterDataObserver(new CustomAdapterDataObserver());
        mFirebaseAdapter.notifyDataSetChanged();
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
    }

    private void updateRecyclerItemContainerGravity(LinearLayout parentLayout, int gravity) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT);
        params.weight = 1.0f;
        params.gravity = gravity;
        parentLayout.setLayoutParams(params);
    }

    private void launchSignInActivity() {
        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }
    }

    private void updateApiAgent(BotMessage botMessage) {
        try {
            AIRequest aiRequest = new AIRequest();
            aiRequest.setQuery(botMessage.getText());
            new RequestTask().execute(aiRequest);
        } catch (IllegalArgumentException illegalArgumentException) {
            illegalArgumentException.printStackTrace();
        }
    }

    public static void hideKeyBoard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.sendButton) {
            sendMessageToFireBaseDatabaseAndUpdateApi(mMessageEditText.getText().toString());
            hideKeyBoard(mMessageEditText);
        }
        else if(id == R.id.symptom_send) {
            sendSelectedSymptomsInOrder();
        }
        else if (id == R.id.symptom_none) {
            updateViewOnSelectingNone(view);
        }
        else if (id == R.id.imageMike) {
            promptSpeechInput();
        }
        else if (id == R.id.option_yes || id == R.id.option_no) {
            sendOptionToApi(view);
        }
        else if(id == R.id.moreInfo) {
            loadPatientInfo();
        }
        else if (view instanceof Button) {
            updateViewState(view);
        }
    }

    private void sendSelectedSymptomsInOrder(){
        String symptoms = "";
        Collections.sort(responseList);
        if (responseList.size() > 0) {
            for (int i = 0; i < responseList.size(); i++) {
                if (i == 0) {
                    symptoms = responseList.get(i);
                } else {
                    symptoms = symptoms + "," + responseList.get(i);
                }
            }
            sendMessageToFireBaseDatabaseAndUpdateApi(symptoms);
            mFlexibleLayout.setVisibility(View.GONE);
            mSendSymptom.setEnabled(false);
            responseList.clear();
        }
    }

   private void updateViewOnSelectingNone(View view){
        for (int i = 0; i < buttonList.size(); i++) {
            buttonList.get(i).setEnabled(false);
            buttonList.get(i).setBackgroundColor(Color.LTGRAY);
        }
        view.setEnabled(true);
        view.setBackgroundResource(R.drawable.rounded_border_blue_bg_text_view);
        responseList.clear();
        responseList.add("None of these");
        mSendSymptom.setEnabled(true);
    }
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening...");
        startActivityForResult(intent, 1);
    }

    private void loadPatientInfo(){
        if(symptomsSearchModel!=null) {
            Intent launchPatientDetailPage = new Intent(this, PatientInfoDetailActivity.class);
            launchPatientDetailPage.putExtra("search_item_url", symptomsSearchModel.getRefLink());
            startActivity(launchPatientDetailPage);
        }
    }
    private void updateViewState(View view){
        String tag = view.getTag().toString();
        if(responseList.contains(tag)){
            view.setBackgroundColor(ContextCompat.getColor(this, R.color.blue));
            responseList.remove(tag);
        }else{
            responseList.add(tag);
            view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
        }
        mMessageEditText.setEnabled(false);
        mMike.setEnabled(false);
        mSendSymptom.setEnabled(true);
    }

    private void sendOptionToApi(View view){
        sendMessageToFireBaseDatabaseAndUpdateApi(view.getTag().toString());
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Populate the wordsList with the String values the recognition engine thought it heard
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            mMessageEditText.setText(matches.get(0));
            sendMessageToFireBaseDatabaseAndUpdateApi(mMessageEditText.getText().toString());

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendMessageToFireBaseDatabaseAndUpdateApi(String message) {
        BotMessage botMessage = new
                BotMessage(message,
                mUsername,
                mPhotoUrl,
                null /* no image */);
        mFirebaseDatabaseReference.child(userAccName)
                .push().setValue(botMessage);
        updateApiAgent(botMessage);
        mMessageEditText.setText("");
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    ;

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResult(AIResponse response) {
        Result result = response.getResult();
        Gson gson = new Gson();
        Log.d("TAG","---response-"+result.getParameters().toString());
        if(result.getParameters().size() > 0 ) {
            if (result.getParameters().get("SymptomList") != null) {
                mMessageEditText.setText("");
                mFlexibleLayout.removeAllViews();
                String data = result.getParameters().get("SymptomList").getAsString();
                List<String> symptomList = Arrays.asList(data.split("\\s*,\\s*"));
                for (int j = 0; j < symptomList.size(); j++) {
                    Button button = new Button(this);
                    FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.MATCH_PARENT, FlexboxLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(10, 10, 10, 10);
                    // params.order = -1;
                    params.flexGrow = 1;
                    params.flexBasisPercent = 0.4f;
                    button.setPadding(5, 0, 5, 0);
                    button.setLayoutParams(params);
                    button.setTag(symptomList.get(j).toLowerCase());
                    button.setText(symptomList.get(j).toLowerCase());
                    button.setOnClickListener(this);
                    button.setTextColor(ContextCompat.getColor(this, R.color.colorTitle));
                    //  button.setBackgroundResource(R.drawable.button_selector);//rounded_border_blue_bg_text_view
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.blue));
                    mFlexibleLayout.addView(button);
                    buttonList.add(button);
                }
                RelativeLayout.LayoutParams msgParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                msgParam.addRule(RelativeLayout.ABOVE, R.id.flexibleLayout);
                mMessageRecyclerView.setLayoutParams(msgParam);
                RelativeLayout.LayoutParams relativeParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                relativeParam.addRule(RelativeLayout.ABOVE, R.id.symptom_option_parent);
                mFlexibleLayout.setLayoutParams(relativeParam);
                mFlexibleLayout.setVisibility(View.VISIBLE);
                userInteractionLinearLayout.setVisibility(View.GONE);
                symptomSendParent.setVisibility(View.VISIBLE);
                optionsParent.setVisibility(View.GONE);
                searchInfoParent.setVisibility(View.GONE);
            } else if (result.getParameters().get("SingleOptionList") != null) {
                RelativeLayout.LayoutParams msgParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                msgParam.addRule(RelativeLayout.ABOVE, R.id.symptom_option_parent);
                mMessageRecyclerView.setLayoutParams(msgParam);
                userInteractionLinearLayout.setVisibility(View.GONE);
                symptomSendParent.setVisibility(View.GONE);
                optionsParent.setVisibility(View.VISIBLE);
                searchInfoParent.setVisibility(View.GONE);
            } else if (result.getParameters().get("SearchTerm") != null) {
                searchTerm = result.getParameters().get("SearchTerm").getAsString();
                serviceUrl =  url + searchTerm.replaceAll(" ", "%20") + collectionBind;
                new SearchTermAsync(this,serviceUrl).execute();
            }
        }
        updateToFirebase(result.getFulfillment().getSpeech());
    }

    private void updateToFirebase(String speech) {
        BotMessage botMessage = new
                BotMessage(speech,
                "bot",
                Uri.parse("android.resource://com.patientbot/" + R.drawable.bot).toString(),
                null /* no image */);
        mFirebaseDatabaseReference.child(userAccName)
                .push().setValue(botMessage);
    }

    public void updateViewOnResult(SymptomsSearchModel symptomsSearchModel){
        this.symptomsSearchModel = symptomsSearchModel;
        RelativeLayout.LayoutParams msgParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        msgParam.addRule(RelativeLayout.ABOVE, R.id.symptom_option_parent);
        mMessageRecyclerView.setLayoutParams(msgParam);
        userInteractionLinearLayout.setVisibility(View.GONE);
        symptomSendParent.setVisibility(View.GONE);
        optionsParent.setVisibility(View.GONE);
        searchInfoParent.setVisibility(View.VISIBLE);
        this.tvSearchTerm.setText(searchTerm);
        this.searchTermContent.setText(symptomsSearchModel.getContent());

    }
    @Override
    public void onError(AIError error) {
    }

    @Override
    public void onAudioLevel(float level) {
    }

    @Override
    public void onListeningStarted() {
    }

    @Override
    public void onListeningCanceled() {
    }

    @Override
    public void onListeningFinished() {
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        CircleImageView messengerImageView;
        CircleImageView mBotImageView;
        LinearLayout parentLayout;

        public MessageViewHolder(View view) {
            super(view);
            messageTextView = (TextView) view.findViewById(R.id.messageTextView);
            messengerImageView = (CircleImageView) view.findViewById(R.id.messengerImageView);
            messengerImageView.setVisibility(View.GONE);
            mBotImageView = (CircleImageView) view.findViewById(R.id.messengerImageViewbot);
            parentLayout = (LinearLayout) view.findViewById(R.id.parentLayout);
        }

        public void bindData(BotMessage botMessage) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            params.weight = 1.0f;
            if (botMessage.getName().equalsIgnoreCase("bot")) {
                params.gravity = Gravity.START;
                parentLayout.setLayoutParams(params);
                messageTextView.setText(botMessage.getText());
            } else {
                params.gravity = Gravity.END;
                parentLayout.setLayoutParams(params);
                messageTextView.setText(botMessage.getText());
            }
        }
    }

    private class RequestTask extends AsyncTask<AIRequest, Integer, AIResponse> {
        private AIError aiError;

        @Override
        protected AIResponse doInBackground(final AIRequest... params) {
            final AIRequest request = params[0];
            try {
                final AIResponse response = aiDataService.request(request);
                // Return response
                return response;
            } catch (final AIServiceException e) {
                aiError = new AIError(e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(final AIResponse response) {
            if (response != null) {
                onResult(response);
            } else {
                onError(aiError);
            }
        }
    }

    private class CustomTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (charSequence.toString().trim().length() > 0) {
                mSendButton.setEnabled(true);
            } else {
                mSendButton.setEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }

    private class CustomAdapterDataObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            int friendlyMessageCount = mFirebaseAdapter.getItemCount();
            int lastVisiblePosition =
                    mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
            // If the recycler view is initially being loaded or the
            // user is at the bottom of the list, scroll to the bottom
            // of the list to show the newly added message.
            if (lastVisiblePosition == -1 ||
                    (positionStart >= (friendlyMessageCount - 1) &&
                            lastVisiblePosition == (positionStart - 1))) {
                mMessageRecyclerView.scrollToPosition(positionStart);
            }
        }
    }
}
