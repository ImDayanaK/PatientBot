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

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
        implements GoogleApiClient.OnConnectionFailedListener , AIListener , View.OnClickListener{

    private AIService aiService;
    private AIDataService aiDataService;
    private AIConfiguration config;
    private static final String TAG = "MainActivity";
    public String userAccName = "user1";
    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private String mUsername = "anonymous";
    private String mPhotoUrl;
    private SharedPreferences mSharedPreferences;
    private GoogleApiClient mGoogleApiClient;
    private static final String MESSAGE_URL = "http://friendlychat.firebase.google.com/message/";
    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ImageView mAddMessageImageView;
    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<BotMessage, MessageViewHolder> mFirebaseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_box);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(getIntent()!=null && getIntent().getStringExtra("userId")!=null){
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
        mAddMessageImageView = (ImageView) findViewById(R.id.addMessageImageView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        // API.ai with client id in api.ai console
        //33ed587a20af4b5c9b96300aff4e9dff
        config = new AIConfiguration("dcd2779937d349c9ac9eeb7e8043ab64",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        // Use with text search
        aiDataService = new AIDataService(this, config);
        // Use with Voice input
        aiService = AIService.getService(this, config);
        aiService.setListener(this);
        // New child entries
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAdapter = generateFirebaseAdapterInstance();

        mFirebaseAdapter.registerAdapterDataObserver(new CustomAdapterDataObserver());

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(mSharedPreferences
                .getInt(CodelabPreferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        mMessageEditText.addTextChangedListener(new CustomTextWatcher());
        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(this);
        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
    }

    private FirebaseRecyclerAdapter<BotMessage,MessageViewHolder> generateFirebaseAdapterInstance() {
        FirebaseRecyclerAdapter  mFirebaseAdapter =  new FirebaseRecyclerAdapter<BotMessage,
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
                    Log.d("TAG","--populateviewholder if");
                    viewHolder.messageTextView.setText(botMessage.getText());
                    viewHolder.messageTextView.setGravity(Gravity.LEFT);
                    viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
                    viewHolder.messageImageView.setVisibility(ImageView.GONE);
                } else {
                    Log.d("TAG","--populateviewholder else");
                    String imageUrl = botMessage.getImageUrl();
                    if (imageUrl.startsWith("gs://")) {
                        Log.d("TAG","--populateviewholder else if");
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(imageUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(
                                new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            String downloadUrl = task.getResult().toString();
                                            Glide.with(viewHolder.messageImageView.getContext())
                                                    .load(downloadUrl)
                                                    .into(viewHolder.messageImageView);
                                        } else {
                                            Log.w(TAG, "Getting download url was not successful.",
                                                    task.getException());
                                        }
                                    }
                                });
                    } else {
                        Log.d("TAG","--populateviewholder else else");
                        Glide.with(viewHolder.messageImageView.getContext())
                                .load(botMessage.getImageUrl())
                                .into(viewHolder.messageImageView);
                    }
                    viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
                    viewHolder.messageTextView.setVisibility(TextView.GONE);
                }

                viewHolder.messengerTextViewContent.setText(botMessage.getName());
                if(botMessage.getName().equalsIgnoreCase("bot")){
                    viewHolder.parentLayout.setGravity(Gravity.LEFT);
                }else{
                    viewHolder.parentLayout.setGravity(Gravity.RIGHT);
                }
                if (botMessage.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(ChatBoxActivity.this,
                            R.drawable.bot));
                } else {
                    Glide.with(ChatBoxActivity.this)
                            .load(botMessage.getPhotoUrl())
                            .into(viewHolder.messengerImageView);
                }
            }
        };
        return mFirebaseAdapter;
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
        }catch (IllegalArgumentException illegalArgumentException){
            illegalArgumentException.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.sendButton){
            BotMessage botMessage = new
                    BotMessage(mMessageEditText.getText().toString(),
                    mUsername,
                    mPhotoUrl,
                    null /* no image */);
            mFirebaseDatabaseReference.child(userAccName)
                    .push().setValue(botMessage);
            mMessageEditText.setText("");
            // save to api.ai
            updateApiAgent(botMessage);
        }
    }
    private class RequestTask extends AsyncTask<AIRequest, Integer, AIResponse> {
        private AIError aiError;

        @Override
        protected AIResponse doInBackground(final AIRequest... params) {
            final AIRequest request = params[0];
            try {
                final AIResponse response =    aiDataService.request(request);
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
    };
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextViewContent;
        CircleImageView messengerImageView;
        LinearLayout parentLayout;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messageImageView = (ImageView) itemView.findViewById(R.id.messageImageView);
            messengerTextViewContent = (TextView) itemView.findViewById(R.id.messengerTextViewContent );
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
            parentLayout = (LinearLayout) itemView.findViewById(R.id.message_container);
        }
    }

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
       /* switch (item.getItemId()) {
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mUsername = ANONYMOUS;
                startActivity(new Intent(this, SignInActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }  */
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
/*        Gson gson = new Gson();
        Log.d("TAG","---parameterString"+gson.toJson(response));
        Log.d("TAG","---speech"+result.getFulfillment().getSpeech());*/
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
