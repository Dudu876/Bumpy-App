package com.example.michael.bumpy;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.StrictMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.example.michael.bumpy.Globals.Globals;
import com.example.michael.bumpy.Model.Accident;
import com.example.michael.bumpy.Model.Driver;
import com.example.michael.bumpy.Model.Witness;
import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class AccidentDetailsActivity extends AppCompatActivity {
    private String serverUrl = "http://10.10.16.151:3000/acc";
    private Accident accident;
    private String secondDriver;

//    Activity _activity = this;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    ImageButton addImageButton;
    Button finish;
    Button witnessesButton;
    LinearLayout imagesLayout;
    LayoutInflater inflater;

    ArrayList<Witness> witnessesList = new ArrayList<>();

    File file;

    int i = 0;

    ArrayList imagesList = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accident_details);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Intent intent = getIntent();
        secondDriver = intent.getStringExtra("secondDriver");

        accident = new Accident(Driver.getInstance().getId(), secondDriver);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "bla");
            jsonObject.put("location", "bla");
            jsonObject.put("my_id", accident.getFirstDriverId());
            jsonObject.put("opp_id", accident.getSecondDriverId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String result = Globals.postDataToServer(jsonObject, "acc");

        if (result != null){
            JSONObject js = null;
            try {
                js = new JSONObject(result);
                accident.setId(js.getString("id"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

//        addImageButton = (ImageButton) findViewById(R.id.addPhoto);
        imagesLayout = (LinearLayout) findViewById(R.id.imagesLayout);
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        finish = (Button) findViewById(R.id.finish);
        witnessesButton = (Button) findViewById(R.id.witnesses);

        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                JSONObject jsonObject = new JSONObject();
                try {
                    Gson g = new Gson();
                    String s = g.toJson(witnessesList);
                    jsonObject.put("witlist", s);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String result = Globals.postDataToServer(jsonObject, "acc/" + accident.getId() + "/wit");

                jsonObject = new JSONObject();
                try {
                    jsonObject.put("desc", ((EditText)findViewById(R.id.description)).getText().toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                result = Globals.postDataToServer(jsonObject, "acc/" + accident.getId() + "/desc");

                finish();
            }
        });

        witnessesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showListDialog();
            }
        });

        addImageButton = (ImageButton) inflater.inflate(R.layout.image_button, null);
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(300, 300);
        addImageButton.setLayoutParams(layoutParams);
        addImageButton.setBackgroundColor(Color.TRANSPARENT);
        imagesLayout.addView(addImageButton,i);

        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // duplicate + button
//                ImageButton newButton = addImageButton;
//                newButton.setId(0);

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ImageButton newButton = (ImageButton) inflater.inflate(R.layout.image_button, null, true);
                        newButton.setLayoutParams(layoutParams);
                        newButton.setBackgroundColor(Color.TRANSPARENT);
                        newButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Bitmap bitmap = ((BitmapDrawable)((ImageButton)view).getDrawable()).getBitmap();
                                ((ImageView)findViewById(R.id.bigPhoto)).setImageBitmap(bitmap);
                            }
                        });
                        imagesLayout.addView(newButton,i);
                        imagesList.add(newButton);
                        i++;
                    }
                }, 500);
            }
        });
    }


    protected void onResume() {
        super.onResume();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            FileOutputStream out = null;
            final String fileUri = "/new.jpeg";
            try {
                out = new FileOutputStream(getFilesDir() + fileUri, false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ((ImageButton)imagesList.get(imagesList.size() - 1)).setImageBitmap(imageBitmap);

            Thread thread = new Thread(new Runnable(){
                public void run(){
                    File file = new File(getFilesDir() + fileUri);
                    String result = Globals.uploadFile(file, accident.getId(),true);
                }
            });
            thread.start();
//            Uri uri = data.getData();
//            file = new File(getRealPathFromURI(uri));
        }
        else if (resultCode == RESULT_CANCELED) {
            ImageButton ib = (ImageButton) imagesList.get(imagesList.size() - 1);
            ((ViewGroup) ib.getParent()).removeView(ib);
        }
    }

    public void showListDialog() {
        final Dialog dialog = new Dialog(this);

        View view = getLayoutInflater().inflate(R.layout.witnesses_list_dialog, null);

        ListView lv = (ListView) view.findViewById(R.id.custom_list);
//
//        witnessesList.add(new Witness("Michael","0525950412"));
//        witnessesList.add(new Witness("Dani","0521840419"));

        // Change MyActivity.this and myListOfItems to your own values
        CustomListAdapter clad = new CustomListAdapter(AccidentDetailsActivity.this, witnessesList);

        lv.setAdapter(clad);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //do nothing
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(AccidentDetailsActivity.this);
        // Get the layout inflater
//        LayoutInflater inflater = AccidentDetailsActivity.this.getLayoutInflater();

        final AlertDialog ad;

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setNeutralButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        showAddDialog();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        ad = builder.create();
        ad.show();

//        dialog.setContentView(view);

//        dialog.show();
    }

    public void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(AccidentDetailsActivity.this);
        // Get the layout inflater
        LayoutInflater inflater = AccidentDetailsActivity.this.getLayoutInflater();

        View v = inflater.inflate(R.layout.add_witnesses_dialog, null);
        final EditText name = (EditText) v.findViewById(R.id.name);
        final EditText phone = (EditText) v.findViewById(R.id.phone);

        final AlertDialog ad;

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(v)
                // Add action buttons
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        witnessesList.add(new Witness(name.getText().toString(),phone.getText().toString()));
                        showListDialog();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        showListDialog();
                    }
                });
        ad = builder.create();
        ad.setTitle("Add Witness");
        ad.show();
    }
}