package me.dats.com.datsme.Activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import me.dats.com.datsme.Adapters.ChatAdapter;
import me.dats.com.datsme.Models.ChatModelObject;
import me.dats.com.datsme.Models.DateObject;
import me.dats.com.datsme.Models.ListObject;
import me.dats.com.datsme.Models.Messages;
import me.dats.com.datsme.R;
import me.dats.com.datsme.Utils.DateParser;

public class ChatActivity extends AppCompatActivity {

    private static final int TOTAL_ITEM_LOAD = 10;
    private static final int GALLERY_PICK = 1;
    private final List<Messages> MessageList = new ArrayList<>();
    @BindView(R.id.chatAppBar)
    Toolbar mToolbar;
    TextView mUserName;
    TextView mUserSeen;
    CircleImageView mUserImage;
    ImageButton chat_back_button;
    @BindView(R.id.chat_sendbtn)
    ImageButton mSendBtn;
    @BindView(R.id.chat_addbtn)
    ImageView mAddbtn;
    @BindView(R.id.chat_msgview)
    EditText mMsgView;
    @BindView(R.id.messageslist)
    RecyclerView mMessagesList;
    @BindView(R.id.swipe_message_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    LinkedHashMap<String, Set<Messages>> groupedHashMap = new LinkedHashMap<>();
    Set<Messages> list = null;
    int i = 0;
    private String chatUser, userName, userImage, userOnline;
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private FirebaseUser currentUser;
    private String uid;
    private ChatAdapter chatAdapter;
    private LinearLayoutManager mLinearLayout;
    private int mCurrentPage = 1;
    //new solution
    private int itemPos = 0;
    private String mLastKey = "";
    private String mPrevKey = "";
    // Storage Firebase
    private StorageReference mImageStorage;
    private ProgressDialog loadingBar;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        chatUser = getIntent().getStringExtra("from_user_id");
        userName = getIntent().getStringExtra("userName");
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);


        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar = layoutInflater.inflate(R.layout.chat_bar, null);

        actionBar.setCustomView(action_bar);

        bindingViews();

        loadMessages();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCurrentPage++;
                itemPos = 0;
                loadmoreMessages();
            }

        });


    }

    private void bindingViews() {
        mAuth = FirebaseAuth.getInstance();

        currentUser = mAuth.getCurrentUser();

        uid = currentUser.getUid();

        mUserImage = findViewById(R.id.chatBarImageView);
        mUserName = findViewById(R.id.chatBarUserName);
        // mUserSeen = findViewById(R.id.chatBarUserOnline);
        chat_back_button = findViewById(R.id.chat_back_button);

        chat_back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mImageStorage = FirebaseStorage.getInstance().getReference();
        chatAdapter = new ChatAdapter(null, getApplicationContext());
        mMessagesList.setHasFixedSize(true);
        mLinearLayout = new LinearLayoutManager(this);
        mMessagesList.setLayoutManager(mLinearLayout);
        mMessagesList.setAdapter(chatAdapter);
        mMessagesList.setItemAnimator(new DefaultItemAnimator());
        loadingBar = new ProgressDialog(this);


    }

    private void loadmoreMessages() {

        DatabaseReference messageRef = mRootRef.child("messages").child(uid).child(chatUser);

        Query query1 = FirebaseDatabase.getInstance().getReference().child("messages").child(uid);
        query1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot:dataSnapshot.getChildren())
                {
                    snapshot.getRef().orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot d) {
                            Log.d("LetsTestthis", "onDataChange: "+d.getValue());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
//        query1.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                Log.i("TAG", "loadmoreMessages: "+dataSnapshot.toString());
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });
        Query messagequery = messageRef.orderByKey().endAt(mLastKey).limitToFirst(10);

        messagequery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);

                String messageKey = dataSnapshot.getKey();

                if (!mPrevKey.equals(messageKey)) {

                    MessageList.add(itemPos++, messages);

                } else {

                    mPrevKey = mLastKey;

                }

                if (itemPos == 1) {
                    mLastKey = messageKey;
                }

                chatAdapter.notifyDataSetChanged();

                mSwipeRefreshLayout.setRefreshing(false);

                mLinearLayout.scrollToPositionWithOffset(10, 0);


            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void loadMessages() {


        DatabaseReference messageRef = mRootRef.child("messages").child(uid).child(chatUser);

        Query messagequery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEM_LOAD);

        messagequery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);
                String hashMapKey = DateParser.convertDateToString(messages.getTime());
                Log.d("TAG", "start date: " + DateParser.convertDateToString(messages.getTime()));

                if (groupedHashMap.containsKey(hashMapKey)) {
                    // The key is already in the HashMap; add the pojo object
                    // against the existing key.
                    Log.d("TAG", "start date: " + hashMapKey + " message" + messages.getMessage());
                    groupedHashMap.get(hashMapKey).add(messages);
                } else {
                    // The key is not there in the HashMap; create a new key-value pair
                    list = new LinkedHashSet<>();
                    list.add(messages);
                    groupedHashMap.put(hashMapKey, list);
                    Log.d("TAG", "start date: " + hashMapKey + " message" + groupedHashMap.get(i++));
                }


                itemPos++;

                if (itemPos == 1) {
                    mLastKey = dataSnapshot.getKey();
                    mPrevKey = mLastKey;
                }

                MessageList.add(messages);
                mMessagesList.scrollToPosition(MessageList.size() - 1);

                mSwipeRefreshLayout.setRefreshing(false);
                generateListFromMap(groupedHashMap);


            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private List<ListObject> generateListFromMap(LinkedHashMap<String, Set<Messages>> groupedHashMap) {
        // We linearly add every item into the consolidatedList.
        List<ListObject> consolidatedList = new ArrayList<>();
        for (String date : groupedHashMap.keySet()) {
            DateObject dateItem = new DateObject();
            dateItem.setDate(date);
            consolidatedList.add(dateItem);
            for (Messages chatModel : groupedHashMap.get(date)) {
                ChatModelObject generalItem = new ChatModelObject();
                generalItem.setChatModel(chatModel);
                consolidatedList.add(generalItem);
                Log.d("TAG", "start date: " + generalItem.toString());
            }
        }

        chatAdapter.setDataChange(consolidatedList);
        return consolidatedList;
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

//        if (currentUser != null) {
//
//            mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("online").setValue("true");
//
//        }
        settingview();

        chatFuctions();


    }

    private void chatFuctions() {

        mRootRef.child("chat").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(chatUser)) {

                    Map<String, Object> chatAddMap = new HashMap<>();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("timestamp", ServerValue.TIMESTAMP);

                    Map<String, Object> chatUserMap = new HashMap<>();
                    chatUserMap.put("chat/" + uid + "/" + chatUser, chatAddMap);
                    chatUserMap.put("chat/" + chatUser + "/" + uid, chatAddMap);

                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if (databaseError != null) {
                                Log.i("TAG", "onComplete: " + databaseError.getMessage().toString());
                            }

                        }
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void settingview() {

        mUserName.setText(userName);
        Log.i("TAG", "settingview: " + userName);

        mRootRef.child("Users").child(chatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                userImage = dataSnapshot.child("image").getValue().toString();
//                userOnline = dataSnapshot.child("online").getValue().toString();
//
//
//                if (userOnline.equals("true")) {
//                    mUserSeen.setText("Online");
//                } else {
//
//                    LastSeen getTime = new LastSeen();
//                    long time = Long.parseLong(userOnline);
//
//                    String lastseen = getTime.getTimeAgo(time, getApplicationContext());
//
//
//                    mUserSeen.setText("last seen " + lastseen);
//                }
                if (!userImage.equals("default"))
                    Picasso.get()
                            .load(userImage)
                            .networkPolicy(NetworkPolicy.OFFLINE)
                            .placeholder(R.drawable.default_avatar)
                            .into(mUserImage, new Callback() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError(Exception e) {
                                    if (!userImage.equals("default"))
                                        Picasso.get()
                                                .load(userImage)
                                                .placeholder(R.drawable.default_avatar)
                                                .into(mUserImage);
                                }

                            });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        FirebaseUser currentUser = mAuth.getCurrentUser();

//        if (currentUser != null) {
//
//            mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("online").setValue("true");
//
//        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser currentUser = mAuth.getCurrentUser();

//        if (currentUser != null) {
//
//            mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("online").setValue(ServerValue.TIMESTAMP);
//
//        }

    }

    public void SendMessage(View view) {

        getMessage();
    }

    private void getMessage() {

        String message = mMsgView.getText().toString().trim();
        if (message != null) {

            if (!TextUtils.isEmpty(message)) {

                String current_user_ref = "messages/" + uid + "/" + chatUser;
                String chat_user_ref = "messages/" + chatUser + "/" + uid;

                DatabaseReference userMessagePush = mRootRef.child("messages")
                        .child(uid).child(chatUser).push();

                String push_id = userMessagePush.getKey();

                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("message", message);
                messageMap.put("seen", false);
                messageMap.put("type", "text");
                messageMap.put("time", ServerValue.TIMESTAMP);
                messageMap.put("from", uid);

                Map<String, Object> messageUserMap = new HashMap<>();
                messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
                messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

                mMsgView.setText("");

                mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            Log.i("TAG", "onComplete: " + databaseError.getMessage().toString());
                        }
                    }
                });

            }

        }
    }

    public void sendImage(View view) {
        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {

            loadingBar.setTitle("Sending Chat Image");
            loadingBar.setMessage("Please wait, while your image is sending ");
            loadingBar.show();

            Uri imageUri = data.getData();

            final String current_user_ref = "messages/" + uid + "/" + chatUser;
            final String chat_user_ref = "messages/" + chatUser + "/" + uid;

            DatabaseReference user_message_push = mRootRef.child("messages")
                    .child(uid).child(chatUser).push();

            final String push_id = user_message_push.getKey();


//            final StorageReference filepath = mImageStorage.child("message_images").child(push_id + ".jpg");
            final StorageReference ref = mImageStorage.child("message_images").child(push_id + ".jpg");
            UploadTask uploadTask = ref.putFile(imageUri);

            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Continue with the task to get the download URL
                    return ref.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {

                        String download_url = task.getResult().toString();

                        Map<String, Object> messageMap = new HashMap<>();
                        messageMap.put("message", download_url);
                        messageMap.put("seen", false);
                        messageMap.put("type", "image");
                        messageMap.put("time", ServerValue.TIMESTAMP);
                        messageMap.put("from", uid);

                        Map<String, Object> messageUserMap = new HashMap<>();
                        messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
                        messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);


                        mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                                if (databaseError != null) {

                                    Log.d("CHAT_LOG", databaseError.getMessage().toString());
                                    loadingBar.dismiss();

                                }
                                mMsgView.setText("");
                                loadingBar.dismiss();
                            }
                        });


                    }

                }
            });

        }
    }
}
