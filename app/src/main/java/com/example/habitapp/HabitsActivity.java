package com.example.habitapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.github.siyamed.shapeimageview.CircularImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import org.xmlpull.v1.XmlPullParser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HabitsActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private String userID;
    static CircularImageView profileImage;
    final public List<Habit> habitsList = new ArrayList<>();
    public ArrayList<User> users = new ArrayList<>();
    GridView gridview;
    TextView habitsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habits);

        gridview = findViewById(R.id.gridview);
        habitsText = findViewById(R.id.habitsText);
        profileImage = findViewById(R.id.profileImage);
        profileImage.setVisibility(View.INVISIBLE);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        userID = firebaseAuth.getCurrentUser().getUid();

        // Get last time user logged in and restart habit values in new day (Ceren)
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int lastTimeStarted = settings.getInt("last_time_started", -1);
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_YEAR);
        if (today != lastTimeStarted) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getDataFromFireStore();

                }
            }, 1300);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("last_time_started", today);
            editor.commit();
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getDataFromFireStore();

                }
            }, 800);
        }



        getUsers();

        //When clicked on the habit, it switches to the DetailsActivity and transfers all the information of the habit there. -gizem-
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(HabitsActivity.this, DetailsActivity.class);
                intent.putExtra("SelectedHabit", habitsList.get(i));
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.habit_options_menu, menu);

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.add_habit) {

            Intent intentToHabit = new Intent(HabitsActivity.this, AddHabitActivity.class);
            startActivity(intentToHabit);

        } else if(item.getItemId() == R.id.global_ranking) {

            Intent intentToRanking = new Intent(HabitsActivity.this, RankingActivity.class);
            intentToRanking.putExtra("user_list", (Serializable) users);
            startActivity(intentToRanking);
            finish();

        } else if (item.getItemId() == R.id.sign_out) {

            firebaseAuth.signOut();

            Intent intentToLogin = new Intent(HabitsActivity.this, LoginActivity.class);
            startActivity(intentToLogin);

        }else if (item.getItemId() == R.id.settings){
            Intent intentToSettings = new Intent(HabitsActivity.this,SettingsActivity.class);
            startActivity(intentToSettings);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }


    // Get data of the user's profile picture. Get user's habits and create new habit object for each of them. Store them in an arraylist (Ceren)
    public void getDataFromFireStore() {

        DocumentReference documentReference = firebaseFirestore.collection("users").document(userID);
        documentReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(HabitsActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
                if (value != null) {
                    String downloadUrl = (String) value.get("profile_image");

                        if (downloadUrl != null) {
                            profileImage.setVisibility(View.VISIBLE);
                            Picasso.get().load(downloadUrl).into(profileImage);
                        }
                    }
            }
        });

        CollectionReference collectionReference = documentReference.collection("habits");

        Query query = collectionReference.orderBy("name", Query.Direction.ASCENDING);


        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(HabitsActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }

                if (value != null) {
                    for (DocumentSnapshot snapshot : value.getDocuments()) {

                        Map<String, Object> data = snapshot.getData();
                        String habitId = snapshot.getId();
                        String name = (String) data.get("name");
                        String description = (String) data.get("description");
                        String image_id = (String) data.get("image_id");
                        String done = (String) data.get("done");
                        String target = (String) data.get("target");
                        String habit_value = (String) data.get("value");
                        String type = (String) data.get("type");
                        String done_percent = (String) data.get("done_percent");

                        habitsList.add(new Habit(habitId, name, image_id, description, done, target, habit_value, type, done_percent));

                        //gridview connects with habitslist -gizem-
                        HabitsAdapter adapter = new HabitsAdapter(HabitsActivity.this, habitsList);
                        gridview.setAdapter(adapter);
                    }
                }
            }
        });
    }

    // Get every user's profile information from Firebase to show up on RankingActivity. Create user object for each of them and hold in an arraylist (Ceren)
    public void getUsers(){
        CollectionReference collectionReference = firebaseFirestore.collection("users");

        collectionReference.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(HabitsActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }

                if (value != null) {
                    for (DocumentSnapshot snapshot : value.getDocuments()) {
                        String name = (String) snapshot.get("name");
                        String surname = (String) snapshot.get("surname");
                        String pictureUrl = (String) snapshot.get("profile_image");
                        final int[] habit_percent = {0};

                        CollectionReference habitCollectionReference = collectionReference.document(snapshot.getId()).collection("habits");
                        habitCollectionReference.addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                if (error != null) {
                                    Toast.makeText(HabitsActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                }

                                if (value != null) {
                                    for (DocumentSnapshot snapshot : value.getDocuments()) {
                                        String percent = (String) snapshot.get("done_percent");
                                        habit_percent[0] = habit_percent[0] + Integer.parseInt(percent);
                                    }
                                    if (value.size() != 0){
                                        int done_percent = habit_percent[0]/value.size();
                                        users.add(new User(name, surname, pictureUrl, done_percent));
                                    } else {
                                        int done_percent = 0;
                                        users.add(new User(name, surname, pictureUrl, done_percent));
                                    }
                                }
                            }
                        });

                    }
                }
            }
        });

    }

}