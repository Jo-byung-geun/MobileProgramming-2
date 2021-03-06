package gachon.mpclass.pearth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

public class UserProfileActivity extends AppCompatActivity{
    private static final String TAG ="UserProfileActivity" ;
    ImageView imageView;
    TextView nickname;
    TextView withdrawl;
    TextView logout;
    TextView announce;
    TextView aboutApp;
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();;
    private Uri filePath;


    private Uri imageUri;
    private String pathUri;

    String uid;
    String nick;

    DatabaseReference mRootDatabaseReference= FirebaseDatabase.getInstance().getReference();
    DatabaseReference userDatabaseReference=mRootDatabaseReference.child("Users");
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference reference = database.getReference("Profile");
    FirebaseAuth user = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        getSupportActionBar().setTitle("????????? ??????");
        Intent intent=getIntent();
        uid=intent.getStringExtra("uid");

        //??????
        withdrawl=(TextView)findViewById(R.id.withdrawl);
        withdrawl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert_confirm = new AlertDialog.Builder(UserProfileActivity.this);
                alert_confirm.setMessage("?????? ????????? ?????? ??????????").setCancelable(false).setPositiveButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                user.delete()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Toast.makeText(UserProfileActivity.this, "????????? ?????? ???????????????.", Toast.LENGTH_LONG).show();
                                                finish();
                                                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                                            }
                                        });
                                deleteFile();
                                mRootDatabaseReference.child("Checklist").child(uid).setValue(null); //realtime database?????? user??? checklist ??????
                                userDatabaseReference.child(uid).setValue(null); //realtime database?????? user ?????? ??????

                            }
                        }
                );
                alert_confirm.setNegativeButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(UserProfileActivity.this, "??????", Toast.LENGTH_LONG).show();
                    }
                });
                alert_confirm.show();
            }
        });

        //????????????
        logout=(TextView)findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                finish();
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));

            }
        });


        //?????????
        aboutApp=(TextView)findViewById(R.id.aboutApp);
        aboutApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), AboutAppActivity.class));

            }
        });

        nickname=(TextView)findViewById(R.id.nickname);
        userDatabaseReference.child(uid).child("nickname").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                nick= (String) snapshot.getValue();
                Log.d("?????????"," " + nick);
                nickname.setText(nick);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        nickname.setText(nick);
        nickname.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                Intent intent1=new Intent(UserProfileActivity.this,ChangeNicknameActivity.class);
                Log.v("???????????? uid: ",uid);
                intent1.putExtra("uid",uid);
                startActivityForResult(intent1,1);
            }
        });

        imageView=(ImageView)findViewById(R.id.imageView);
        setProfile();
        imageView.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {


                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "???????????? ???????????????."), 0);




            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        //request????????? 0?????? OK??? ???????????? data??? ????????? ?????? ?????????
        if(requestCode == 0 && resultCode == RESULT_OK){
            filePath = data.getData();
            Log.d(TAG, "uri:" + String.valueOf(filePath));
            try {
                //Uri ????????? Bitmap?????? ???????????? ImageView??? ?????? ?????????.
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                deleteFile();
                uploadFile();
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(requestCode==1)
        {
            String new_nickname=data.getStringExtra("new nickname");
            nickname.setText(new_nickname);
        }
        if(resultCode==2)
        {
            nickname.setText(nick);
        }
    }
    private void setProfile(){
        FirebaseStorage storage=FirebaseStorage.getInstance();
        StorageReference storageReference=storage.getReferenceFromUrl("gs://pearth-7ec20.appspot.com/");
        StorageReference pathReference=storageReference.child("profile");

        if(pathReference==null)
        {
            Toast.makeText(UserProfileActivity.this,"???????????? ????????? ????????????.",Toast.LENGTH_SHORT).show();
        }
        else
        {
            StorageReference submitProfile=storageReference.child("profile/"+uid+".png");
            submitProfile.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    //Glide.with(UserProfileActivity.this).load(uri).into(imageView);
                    reference.child(user.getUid()).setValue(uri.toString());
                    Glide.with(UserProfileActivity.this).load(uri).circleCrop().into(imageView);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });
        }
    }
    private void deleteFile(){
        FirebaseStorage storage=FirebaseStorage.getInstance();
        StorageReference storageRef=storage.getReference();
        StorageReference desertRef=storageRef.child("profile/"+uid+".png");
        desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                reference.child(user.getUid()).setValue("https://firebasestorage.googleapis.com/v0/b/pearth-7ec20.appspot.com/o/profile%2Fplant.png?alt=media&token=021c6c31-684d-401e-b5ab-c2d8c415cbc8");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }
    //upload the file
    private void uploadFile() {
        //???????????? ????????? ????????? ??????
        if (filePath != null) {
            //????????? ?????? Dialog ?????????
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("????????????...");
            progressDialog.show();

            //storage

            FirebaseStorage storage = FirebaseStorage.getInstance();


            //Unique??? ???????????? ?????????.
            String filename = uid + ".png";

            //storage ????????? ?????? ???????????? ????????? ??????.
            StorageReference storageRef = storage.getReferenceFromUrl("gs://pearth-7ec20.appspot.com/").child("profile/" + filename);
            //???????????????...
            storageRef.putFile(filePath)
                    //?????????
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss(); //????????? ?????? Dialog ?????? ??????
                            Toast.makeText(getApplicationContext(), "????????? ??????!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    //?????????
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "????????? ??????!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    //?????????
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            @SuppressWarnings("VisibleForTests") //?????? ?????? ?????? ???????????? ????????? ????????????. ??? ??????????
                                    double progress = (100 * taskSnapshot.getBytesTransferred()) /  taskSnapshot.getTotalByteCount();
                            //dialog??? ???????????? ???????????? ????????? ??????
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "% ...");
                        }
                    });
        } else {
            Toast.makeText(getApplicationContext(), "????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show();
        }
    }



    //???????????? ?????? ???????????? ?????? ??????
    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        getMenuInflater().inflate(R.menu.menu, menu);
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

        if (id == R.id.action_record) {
            Intent homeIntent = new Intent(this, RecordActivity.class);
            startActivity(homeIntent);
        }
        if (id == R.id.action_analysis) {
            Intent settingIntent = new Intent(this, Analysis.class);
            startActivity(settingIntent);
        }
        if (id == R.id.action_share) {
            Intent shareIntent = new Intent(this, Shareboard.class);
            startActivity(shareIntent);
        }
        if (id == R.id.action_plant) {
            Intent plantIntent = new Intent(this, GrowingPlantActivity.class);
            startActivity(plantIntent);
        }
        if(id==R.id.action_checklist)
        {
            Intent intent=new Intent(UserProfileActivity.this,CheckListActivity.class);
            String uid = firebaseAuth.getCurrentUser().getUid();
            intent.putExtra("uid",uid);
            startActivity(intent);
        }
        if(id==R.id.action_UserProfile)
        {
            Intent intent=new Intent(UserProfileActivity.this,UserProfileActivity.class);
            String uid = firebaseAuth.getCurrentUser().getUid();
            intent.putExtra("uid",uid);
            startActivity(intent);
        }
        if(id==R.id.action_post){
            Intent PostIntent = new Intent(this, Userpost.class);
            startActivity(PostIntent);
        }
        if(id==R.id.action_store) {
            Intent intent = new Intent(this, SetLocationActivity.class);
            startActivity(intent);
        }
        if (id == R.id.action_back) {
            finish();
        }


        return super.onOptionsItemSelected(item);
    }
}