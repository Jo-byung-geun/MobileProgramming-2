package gachon.mpclass.pearth;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Shareboard_write extends AppCompatActivity {
    private static final String TAG = "SubActivity";
    private Button sendbt;
    private EditText title;
    private EditText content;
    private ImageButton imagebt;
    private ImageButton uploadbt;
    public String tit;
    public String con;
    public String img;
    public String tag;
    public String uid;
    public String loc;
    String good="0";
    public String userid;
    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference databaseReference = firebaseDatabase.getReference();
    private FirebaseDatabase mDatabase;
    private DatabaseReference mReference;
    private ChildEventListener mChild;
    private DatabaseReference mRef;
    DatabaseReference mDB2=FirebaseDatabase.getInstance().getReference().child("Users");
    private ListView listView;
    List<Object> Array = new ArrayList<Object>();
    private ImageView ivPreview;
    private Uri filePath;
    private boolean isImg = true;
    int share=0;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private Spinner spinner;
    private String[] listSpinner;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final int[] cnt = {0};
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shareboard_write);
        getSupportActionBar().setTitle("");
        Spinner spinner=(Spinner)findViewById(R.id.spinner);
        listSpinner = getResources().getStringArray(R.array.spinner_shareboard);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,listSpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView adapterView, View view, int position, long id){
                String item = (String)adapterView.getItemAtPosition(position);
                loc = item;
            }
            @Override
            public void onNothingSelected(AdapterView adapterView){
                loc="";
            }
        });

        title = (EditText) findViewById(R.id.title);
        content = (EditText)findViewById(R.id.content);
        sendbt = (Button) findViewById(R.id.upload);
        imagebt = (ImageButton) findViewById(R.id.imageUploadButton);
        ivPreview = (ImageView) findViewById(R.id.iv_preview);
        uid = user.getUid();

        initDatabase();
        mRef=mDatabase.getReference("Users").child(user.getUid());
//        mRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                int count=0;
//                if(snapshot.exists())
//                {
//                    for(DataSnapshot messageData : snapshot.getChildren()) {
//                        String msg2=messageData.getValue().toString();
//                        if(count!=3){
//                            count=count+1;
//                        }
//                        else if(count==3){
//                            share=Integer.parseInt(msg2);
//                        }
//
//
//                    }
//                }
//                else
//                {
//                    Toast.makeText(getApplicationContext(),"??????",Toast.LENGTH_LONG).show();
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
        imagebt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "???????????? ???????????????."), 0);

            }
        });

        sendbt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                tit = title.getText().toString();
                if(tit==null) {
                    tit = "title";
                }
                con = content.getText().toString();
                if(con==null){
                    con="content";
                }
                img = G.imgUrl;
                if(img==null){
                    img="imgUrl";
                }
                uid = user.getUid();
                if(uid==null){
                    uid="";
                }
                if(loc==null)
                {
                    loc = "";
                }
                //????????????, ??????, ?????? ????????? ?????????
                uploadFile();
                if(isImg==true) {
                    finish();//?????? ???????????? ?????????????????? ?????????
                }

            }
        });

        mReference = mDatabase.getReference("share"); // ???????????? ????????? child ??????






    }
    //?????? ??????
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //request????????? 0?????? OK??? ???????????? data??? ????????? ?????? ?????????
        if(requestCode == 0 && resultCode == RESULT_OK){
            filePath = data.getData();
            Log.d(TAG, "uri:" + String.valueOf(filePath));
            try {
                //Uri ????????? Bitmap?????? ???????????? ImageView??? ?????? ?????????.
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                ivPreview.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //upload the file
    private void uploadFile() {
        //???????????? ????????? ????????? ??????
        if (filePath != null&&loc !="") {
            //????????? ?????? Dialog ?????????
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("????????????...");
            progressDialog.show();
            isImg = true;
            //storage
            FirebaseStorage storage = FirebaseStorage.getInstance();

            //Unique??? ???????????? ?????????.
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMHH_mmss");
            Date now = new Date();
            String filename = formatter.format(now) + ".png";
            //storage ????????? ?????? ???????????? ????????? ??????.
            StorageReference storageRef = storage.getReferenceFromUrl("gs://pearth-7ec20.appspot.com").child("images_share/" + filename);
            G.fileName = filename;
            storageRef.putFile(filePath)
                    //?????????
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //????????? ???????????? ?????????????????????...
                            //????????? firebase storage??? ????????? ?????? ???????????? URL??? ????????????
                            storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    //??????????????? firebase??? ???????????? ???????????? ??????
                                    //???????????? ?????? ???????????? ??????(URL)??? ???????????? ????????????
                                    G.imgUrl = uri.toString();

                                    Toast.makeText(Shareboard_write.this, "????????? ??????", Toast.LENGTH_SHORT).show();

                                    //firebase DB????????? ?????? ??????
                                    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

                                    DatabaseReference profileRef = firebaseDatabase.getReference("share");

                                    //???????????? key ???????????? ?????? ????????? ???????????? ????????? ????????? ??????
                                    //profileRef.child("board").setValue(G.imgUrl);
                                    //????????? ?????????
                                    ListViewItem_shareboard list=new ListViewItem_shareboard(tit,con,G.imgUrl,loc,G.fileName,uid);
                                    databaseReference.child("share").push().setValue(list);

                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "????????? ??????!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        else {
            isImg = false;
            if(filePath==null&&loc==""){Toast.makeText(getApplicationContext(), "???????????? ???????????? ???????????????.", Toast.LENGTH_SHORT).show();}
            else if(filePath==null){Toast.makeText(getApplicationContext(), "???????????? ???????????????.", Toast.LENGTH_SHORT).show();}
            else if(loc==""){Toast.makeText(getApplicationContext(), "???????????? ???????????????.", Toast.LENGTH_SHORT).show();}}
        //saveData() ..
    }

    private void initDatabase() {

        mDatabase = FirebaseDatabase.getInstance();

        mReference = mDatabase.getReference("log");
        mReference.child("log").setValue("check");

        mChild = new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

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
        };
        mReference.addChildEventListener(mChild);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mReference.removeEventListener(mChild);
    }

    //???????????? ?????? ???????????? ?????? ??????
    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        getMenuInflater().inflate(R.menu.writemenu, menu);
        return true;
    }

    //????????? ?????????
    private void hideActionBar () {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (id == R.id.action_back) {
            finish();
        }




        return super.onOptionsItemSelected(item);
    }
    //????????? ??????????????? ?????????
    @Override
    protected  void onResume(){
        super.onResume();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }


}